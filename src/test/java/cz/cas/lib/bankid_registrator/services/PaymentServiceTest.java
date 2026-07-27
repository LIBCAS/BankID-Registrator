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
