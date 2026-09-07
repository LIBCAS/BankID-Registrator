package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.PaymentServiceConfig;
import cz.cas.lib.bankid_registrator.dao.mariadb.PaymentRepository;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest
{
    private static final String TEST_ALEPH_ID = "TEST00001";

    @Test
    void refreshPaymentAmountFromAlephReplacesAndPersistsStaleAmount() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        AlephService alephService = mock(AlephService.class);
        PaymentService paymentService = createPaymentService(paymentRepository, alephService);
        Payment payment = paymentWithAmount(new BigDecimal("999.00"));

        when(alephService.getPatronFines(TEST_ALEPH_ID))
            .thenReturn(finesResult(new BigDecimal("280.00")));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment refreshed = paymentService.refreshPaymentAmountFromAleph(payment);

        assertSame(payment, refreshed);
        assertEquals(new BigDecimal("280.00"), refreshed.getAmount());
        verify(paymentRepository).save(payment);
    }

    @Test
    void refreshPaymentAmountFromAlephDoesNotWriteWhenAmountIsCurrent() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        AlephService alephService = mock(AlephService.class);
        PaymentService paymentService = createPaymentService(paymentRepository, alephService);
        Payment payment = paymentWithAmount(new BigDecimal("280.00"));

        when(alephService.getPatronFines(TEST_ALEPH_ID))
            .thenReturn(finesResult(new BigDecimal("280.00")));

        Payment refreshed = paymentService.refreshPaymentAmountFromAleph(payment);

        assertSame(payment, refreshed);
        assertEquals(new BigDecimal("280.00"), refreshed.getAmount());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void refreshPaymentAmountFromAlephRejectsLookupFailureWithoutUsingStoredAmount() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        AlephService alephService = mock(AlephService.class);
        PaymentService paymentService = createPaymentService(paymentRepository, alephService);
        Payment payment = paymentWithAmount(new BigDecimal("999.00"));
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("error", "Aleph unavailable");

        when(alephService.getPatronFines(TEST_ALEPH_ID)).thenReturn(errorResult);

        assertThrows(IllegalStateException.class,
            () -> paymentService.refreshPaymentAmountFromAleph(payment));
        assertEquals(new BigDecimal("999.00"), payment.getAmount());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void paymentExistsUsesIdentityAndBusinessType() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentService paymentService = createPaymentService(paymentRepository, mock(AlephService.class));
        Identity identity = new Identity("bank-id-sub");
        when(paymentRepository.existsByIdentityAndType(identity, PaymentType.REGISTRATION))
            .thenReturn(true);

        assertTrue(paymentService.paymentExists(identity, PaymentType.REGISTRATION));
        assertFalse(paymentService.paymentExists(identity, PaymentType.RENEWAL));
    }

    @Test
    void seniorChargeCoveredByVoucherStillRequiresSettlement() {
        PaymentRepository repository = mock(PaymentRepository.class);
        AlephService aleph = mock(AlephService.class);
        PaymentService service = createPaymentService(repository, aleph);
        Identity identity = paymentWithAmount(BigDecimal.ZERO).getIdentity();
        when(aleph.getPatronFines(TEST_ALEPH_ID)).thenReturn(finesResult(new BigDecimal("150")));
        when(repository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        Payment payment = service.createPayment(identity, PaymentType.REGISTRATION, "SENIOR", new BigDecimal("150"));

        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(0, payment.getAmountToPay().signum());
        assertFalse(service.verifyPaymentStatus(TEST_ALEPH_ID));
    }

    @Test
    void waivedFeeWithoutAlephChargeCanCompleteWithoutGateway() {
        PaymentRepository repository = mock(PaymentRepository.class);
        AlephService aleph = mock(AlephService.class);
        PaymentService service = createPaymentService(repository, aleph);
        when(aleph.getPatronFines(TEST_ALEPH_ID)).thenReturn(finesResult(BigDecimal.ZERO));
        when(repository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        Payment payment = service.createPayment(paymentWithAmount(BigDecimal.ZERO).getIdentity(),
            PaymentType.REGISTRATION, "SENIOR", new BigDecimal("150"));

        assertEquals(PaymentStatus.VOUCHER_COVERED, payment.getStatus());
        assertTrue(service.verifyPaymentStatus(TEST_ALEPH_ID));
    }

    @Test
    void seniorVoucherLeavesRenewalFinesPayable() {
        PaymentRepository repository = mock(PaymentRepository.class);
        AlephService aleph = mock(AlephService.class);
        when(aleph.getPatronFines(TEST_ALEPH_ID)).thenReturn(finesResult(new BigDecimal("200")));
        when(repository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        Payment payment = createPaymentService(repository, aleph).createPayment(
            paymentWithAmount(BigDecimal.ZERO).getIdentity(), PaymentType.RENEWAL, "SENIOR", new BigDecimal("150"));
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(new BigDecimal("50"), payment.getAmountToPay());
    }

    @Test
    void balanceLookupFailureCannotCreateOrVerifyPayment() {
        PaymentRepository repository = mock(PaymentRepository.class);
        AlephService aleph = mock(AlephService.class);
        PaymentService service = createPaymentService(repository, aleph);
        when(aleph.getPatronFines(TEST_ALEPH_ID)).thenReturn(java.util.Collections.singletonMap("error", "unavailable"));
        assertThrows(IllegalStateException.class, () -> service.createPayment(
            paymentWithAmount(BigDecimal.ZERO).getIdentity(), PaymentType.REGISTRATION, "SENIOR", new BigDecimal("150")));
        assertThrows(IllegalStateException.class, () -> service.verifyPaymentStatus(TEST_ALEPH_ID));
        verify(repository, never()).save(any(Payment.class));
    }

    private PaymentService createPaymentService(
        PaymentRepository paymentRepository,
        AlephService alephService
    ) {
        return new PaymentService(
            paymentRepository,
            alephService,
            mock(PaymentServiceConfig.class),
            mock(AlephServiceConfig.class)
        );
    }

    private Payment paymentWithAmount(BigDecimal amount) {
        Identity identity = new Identity("bank-id-sub");
        identity.setAlephId(TEST_ALEPH_ID);
        Payment payment = new Payment(identity, PaymentType.REGISTRATION, PaymentStatus.PENDING, amount);
        payment.setId(999L);
        return payment;
    }

    private Map<String, Object> finesResult(BigDecimal totalDueCash) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalDueCash", totalDueCash);
        return result;
    }
}
