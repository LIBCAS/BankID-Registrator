package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.PaymentServiceConfig;
import cz.cas.lib.bankid_registrator.services.RegistrationFeeService;
import cz.cas.lib.bankid_registrator.configurations.SessionTimerConfig;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.PaymentService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import cz.cas.lib.bankid_registrator.services.VoucherService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest
{
    @Test
    void paymentPageDisplaysAmountRefreshedFromAleph() {
        ControllerFixture fixture = new ControllerFixture();
        Payment stalePayment = fixture.payment(new BigDecimal("999.00"));
        Payment refreshedPayment = fixture.payment(new BigDecimal("280.00"));
        fixture.stubAuthenticatedPayment(stalePayment, refreshedPayment);
        Model model = new ExtendedModelMap();

        String view = fixture.controller.paymentPage(model, Locale.ENGLISH, fixture.request);

        assertEquals("payment", view);
        assertEquals(new BigDecimal("280.00"), model.getAttribute("amount"));
        assertEquals(new BigDecimal("280.00"), model.getAttribute("amountToPay"));
        verify(fixture.paymentService).refreshPaymentAmountFromAleph(stalePayment);
    }

    @Test
    void initiatePaymentSignsAmountRefreshedFromAleph() {
        ControllerFixture fixture = new ControllerFixture();
        Payment stalePayment = fixture.payment(new BigDecimal("999.00"));
        Payment refreshedPayment = fixture.payment(new BigDecimal("280.00"));
        fixture.stubAuthenticatedPayment(stalePayment, refreshedPayment);
        when(fixture.paymentService.generatePaymentFormData(refreshedPayment, fixture.identity, null))
            .thenReturn(Collections.singletonMap("amount", "28000"));
        Model model = new ExtendedModelMap();

        String view = fixture.controller.initiatePayment(model, Locale.ENGLISH, fixture.request);

        assertEquals("payment_redirect", view);
        assertEquals(Collections.singletonMap("amount", "28000"), model.getAttribute("formData"));
        verify(fixture.paymentService).refreshPaymentAmountFromAleph(stalePayment);
        verify(fixture.paymentService).generatePaymentFormData(refreshedPayment, fixture.identity, null);
    }

    @Test
    void repeatedPaymentInitiationIsRejectedForSameDisplayedPayment() {
        ControllerFixture fixture = new ControllerFixture();
        Payment payment = fixture.payment(new BigDecimal("280.00"));
        fixture.stubAuthenticatedPayment(payment, payment);
        when(fixture.paymentService.generatePaymentFormData(payment, fixture.identity, null))
            .thenReturn(Collections.singletonMap("amount", "28000"));

        fixture.controller.initiatePayment(new ExtendedModelMap(), Locale.ENGLISH, fixture.request);

        assertThrows(
            cz.cas.lib.bankid_registrator.exceptions.HttpErrorException.class,
            () -> fixture.controller.initiatePayment(new ExtendedModelMap(), Locale.ENGLISH, fixture.request)
        );
    }

    @Test
    void fullyDiscountedExistingChargeUsesZeroPaymentFinalization() {
        ControllerFixture fixture = new ControllerFixture();
        Payment payment = fixture.payment(new BigDecimal("150"));
        payment.setVoucherCode("SENIOR");
        payment.setDiscountAmount(new BigDecimal("150"));
        fixture.stubAuthenticatedPayment(payment, payment);
        String returnUrl = "http://localhost/payment/callback?refId=999999999&status=success";
        when(fixture.paymentService.generatePaymentFormData(payment, fixture.identity, returnUrl))
            .thenReturn(Collections.singletonMap("amount", "0"));
        Model model = new ExtendedModelMap();

        assertEquals("payment_redirect", fixture.controller.initiatePayment(model, Locale.ENGLISH, fixture.request));
        verify(fixture.paymentService).generatePaymentFormData(payment, fixture.identity, returnUrl);
        assertEquals(Collections.singletonMap("amount", "0"), model.getAttribute("formData"));
    }

    @Test
    void voucherCoveredStatusCannotReportSuccessWhileAlephStillHasDebt() {
        ControllerFixture fixture = new ControllerFixture();
        Payment payment = fixture.payment(new BigDecimal("150"));
        payment.setStatus(PaymentStatus.VOUCHER_COVERED);
        fixture.stubAuthenticatedPayment(payment, payment);
        when(fixture.paymentService.getPaymentByAlephBarcode(fixture.identity.getAlephBarcode()))
            .thenReturn(Optional.of(payment));
        when(fixture.paymentService.verifyPaymentStatus(fixture.identity.getAlephId())).thenReturn(false);
        Model model = new ExtendedModelMap();

        assertEquals("payment", fixture.controller.paymentCallback(fixture.identity.getAlephBarcode(),
            null, "success", model, Locale.ENGLISH, fixture.request));
        assertEquals(false, model.getAttribute("paymentSuccess"));
        assertEquals(true, model.getAttribute("paymentFailed"));
        verify(fixture.paymentService).updatePaymentStatus(payment, PaymentStatus.FAILED);
    }

    private static class ControllerFixture
    {
        private final IdentityAuthService identityAuthService = mock(IdentityAuthService.class);
        private final PaymentService paymentService = mock(PaymentService.class);
        private final IdentityService identityService = mock(IdentityService.class);
        private final PaymentServiceConfig paymentServiceConfig = mock(PaymentServiceConfig.class);
        private final TokenService tokenService = mock(TokenService.class);
        private final Identity identity = identity();
        private final MockHttpServletRequest request = new MockHttpServletRequest();
        private final PaymentController controller = new PaymentController(
            mock(MessageSource.class),
            identityAuthService,
            paymentService,
            paymentServiceConfig,
            mock(RegistrationFeeService.class),
            tokenService,
            identityService,
            mock(VoucherService.class),
            mock(SessionTimerConfig.class)
        );

        private void stubAuthenticatedPayment(Payment stalePayment, Payment refreshedPayment) {
            when(identityAuthService.isLoggedin(request)).thenReturn(true);
            when(identityAuthService.isAuthenticatedAs(request, identity.getId())).thenReturn(true);
            when(identityAuthService.getAuthenticatedIdentityId(request)).thenReturn(identity.getId());
            when(identityService.findById(identity.getId())).thenReturn(Optional.of(identity));
            when(paymentService.getLatestPaymentByIdentity(identity)).thenReturn(Optional.of(stalePayment));
            when(paymentService.refreshPaymentAmountFromAleph(stalePayment)).thenReturn(refreshedPayment);
            when(paymentServiceConfig.getApiUrl()).thenReturn("https://payments.example.test");
            when(tokenService.tryClaimKey("payment-initiate:" + refreshedPayment.getId())).thenReturn(true);
        }

        private Payment payment(BigDecimal amount) {
            Payment payment = new Payment(identity, PaymentType.REGISTRATION, PaymentStatus.PENDING, amount);
            payment.setId(999L);
            return payment;
        }

        private static Identity identity() {
            Identity identity = new Identity("bank-id-sub");
            identity.setId(999L);
            identity.setAlephId("TEST00001");
            identity.setAlephBarcode("999999999");
            return identity;
        }
    }
}
