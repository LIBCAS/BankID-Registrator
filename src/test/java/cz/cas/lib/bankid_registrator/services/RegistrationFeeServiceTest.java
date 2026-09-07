package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.AppConfig;
import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.entities.patron.PatronStatus;
import cz.cas.lib.bankid_registrator.entities.voucher.DiscountType;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationFeeServiceTest {
    private final RegistrationFeeConfig config = new RegistrationFeeConfig();
    private final AlephService aleph = mock(AlephService.class);
    private final AppConfig appConfig = mock(AppConfig.class);
    private final PatronService patrons = new PatronService(appConfig, null, null);
    private final RegistrationFeeService fees = new RegistrationFeeService(config, aleph, patrons);

    @Test
    void currentMembershipUsesAssignedStatusEvenIfSeniorNextYear() {
        Patron patron = patron("16");
        patron.setBirthDate(LocalDate.now().minusYears(69).format(DateTimeFormatter.BASIC_ISO_DATE));
        patron.setExpiryDate(LocalDate.now().plusYears(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertEquals(new BigDecimal("280"), fees.getFee(patron));
        patron.setStatus("10");
        assertEquals(new BigDecimal("150"), fees.getFee(patron));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yyyyMMdd", "yyyy-MM-dd", "dd-MM-yyyy"})
    void renewalPreviewUsesAgeAtStartOfNextMembership(String sourceFormat) {
        when(appConfig.getRetirementAge()).thenReturn(70);
        Patron patron = patron("16");
        LocalDate birthday = LocalDate.now().plusDays(10);
        patron.setBirthDate(birthday.minusYears(70).format(DateTimeFormatter.ofPattern(sourceFormat)));
        patron.setExpiryDate(birthday.minusDays(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        assertEquals(new BigDecimal("280"), fees.getRenewalFee(patron));
        patron.setExpiryDate(birthday.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Identity identity = new Identity("test");
        identity.setAlephId("TEST");
        when(aleph.getAlephPatron("TEST", false)).thenReturn(Collections.singletonMap("patron", patron));
        assertEquals(new BigDecimal("150"), fees.getPreviewFee(identity, true));
        assertEquals(new BigDecimal("280"), fees.getPreviewFee(identity, false));
    }

    @Test
    void configuredTariffsAndVoucherDiscountsUseSeniorFee() {
        config.setSeniorAmount(new BigDecimal("160"));
        assertEquals(new BigDecimal("160"), fees.getFee(patron("10")));
        config.setSeniorAmount(new BigDecimal("150"));
        VoucherService vouchers = mock(VoucherService.class, CALLS_REAL_METHODS);
        BigDecimal fee = fees.getFee(patron("10"));
        assertEquals(new BigDecimal("150"), vouchers.computeDiscount(DiscountType.FIXED_CZK, new BigDecimal("280"), fee));
        assertEquals(new BigDecimal("75.00"), vouchers.computeDiscount(DiscountType.PERCENTAGE, new BigDecimal("50"), fee));
        assertEquals(BigDecimal.ZERO, fees.getFee(patron("03")));
    }

    @Test
    void missingAlephDataCannotFallBackToStandardFee() {
        Identity identity = new Identity("test");
        identity.setAlephId("TEST");
        when(aleph.getAlephPatron("TEST", false)).thenReturn(Collections.singletonMap("error", "unavailable"));
        assertThrows(IllegalStateException.class, () -> fees.getFee(identity));
        assertThrows(IllegalStateException.class, () -> fees.getFee(patron(null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"19400716", "1940-07-16", "16-07-1940"})
    void leosSeniorRenewalUses150RegardlessOfBirthDateSource(String birthDate) {
        when(appConfig.getRetirementAge()).thenReturn(70);
        Patron patron = patron("10");
        patron.setBirthDate(birthDate);
        patron.setExpiryDate(LocalDate.now().plusYears(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        Identity identity = new Identity("test");
        identity.setAlephId("TEST");
        when(aleph.getAlephPatron("TEST", false)).thenReturn(Collections.singletonMap("patron", patron));
        assertEquals(new BigDecimal("150"), fees.getRenewalFee(patron));
        assertEquals(new BigDecimal("150"), fees.getPreviewFee(identity, true));
        assertEquals(birthDate, patron.getBirthDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "31-02-1940", "1940-02-31"})
    void invalidBirthDateCannotSilentlySelectStandardRenewalFee(String birthDate) {
        Patron patron = patron("10");
        patron.setBirthDate(birthDate);
        assertThrows(IllegalArgumentException.class, () -> fees.getRenewalFee(patron));
    }

    private Patron patron(String status) {
        Patron patron = new Patron();
        patron.setStatus(status);
        patron.setIsCasEmployee(false);
        return patron;
    }
}
