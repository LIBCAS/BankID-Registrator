package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.*;
import cz.cas.lib.bankid_registrator.dao.mariadb.*;
import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import cz.cas.lib.bankid_registrator.entities.payment.*;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.services.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IdentityVoucherTest {
    @ParameterizedTest
    @CsvSource({"10,150,150,true", "10,75,150,false", "16,150,280,false", "10,280,150,true"})
    void registrationCreatesFeeOnlyWhenVoucherDoesNotWaiveIt(
            String status, String voucherValue, String tariff, boolean waived) {
        Identity identity = new Identity("test");
        identity.setId(93L);
        identity.setAlephId("TEST");
        identity.setAlephBarcode("BARCODE");
        Patron patron = new Patron();
        patron.setStatus(status);
        TokenService tokens = mock(TokenService.class);
        IdentityService identities = mock(IdentityService.class);
        IdentityAuthService auth = mock(IdentityAuthService.class);
        AlephService aleph = mock(AlephService.class);
        PaymentRepository payments = mock(PaymentRepository.class);
        VoucherService vouchers = mock(VoucherService.class);
        VoucherRepository voucherRepository = mock(VoucherRepository.class);
        Voucher voucher = new Voucher("TEST", DiscountType.FIXED_CZK, new BigDecimal(voucherValue), 1);
        when(voucherRepository.findByCode("TEST")).thenReturn(Optional.of(voucher));
        VoucherService validator = new VoucherService(voucherRepository, mock(VoucherUsageRepository.class));
        when(vouchers.validateVoucher(eq("TEST"), eq(identity), any(BigDecimal.class)))
            .thenAnswer(call -> validator.validateVoucher("TEST", identity, call.getArgument(2)));
        when(vouchers.findByCode("TEST")).thenReturn(Optional.of(voucher));
        AtomicReference<BigDecimal> balance = new AtomicReference<>(BigDecimal.ZERO);
        when(aleph.getPatronFines("TEST"))
            .thenAnswer(call -> Collections.singletonMap("totalDueCash", balance.get()));
        when(aleph.createRegistrationFee(patron)).thenAnswer(call -> {
            balance.set(new BigDecimal(tariff));
            return Collections.singletonMap("success", true);
        });
        when(aleph.getAlephPatron(eq("TEST"), anyBoolean()))
            .thenReturn(Collections.singletonMap("patron", patron));
        when(aleph.updatePatronPassword("TEST", "password"))
            .thenReturn(Collections.singletonMap("success", true));
        when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        PaymentService paymentService = new PaymentService(payments, aleph,
            mock(PaymentServiceConfig.class), mock(AlephServiceConfig.class));
        RegistrationFeeService fees = new RegistrationFeeService(new RegistrationFeeConfig(), aleph, mock(PatronService.class));
        IdentityController controller = new IdentityController(mock(MessageSource.class), tokens,
            identities, aleph, mock(EmailService.class), auth, mock(LdapService.class),
            paymentService, vouchers, fees, mock(SessionTimerConfig.class));
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(auth.isLoggedin(request)).thenReturn(true);
        when(tokens.isIdentityTokenValid("token")).thenReturn(true);
        when(tokens.tryInvalidateToken("token")).thenReturn(true);
        when(tokens.extractIdentityIdFromToken("token")).thenReturn("93");
        when(tokens.tryClaimKey(anyString())).thenReturn(true);
        when(identities.findById(93L)).thenReturn(Optional.of(identity));
        PatronPasswordDTO password = new PatronPasswordDTO();
        password.setNewPassword("password");

        String view = controller.passwordSetFormSubmitted("token", "TEST", password,
            mock(BindingResult.class), new ExtendedModelMap(), Locale.ENGLISH, request);

        verify(vouchers).validateVoucher("TEST", identity, new BigDecimal(tariff));
        verify(aleph, times(waived ? 0 : 1)).createRegistrationFee(patron);
        verify(vouchers, times(waived ? 1 : 0)).confirmUsage(voucher, identity);
        assertEquals(waived ? "redirect:/payment/callback?refId=BARCODE&status=success"
            : "redirect:/payment/initiate", view);
        assertEquals(waived ? BigDecimal.ZERO : new BigDecimal(tariff), balance.get());
    }
}
