package cz.cas.lib.bankid_registrator;

import cz.cas.lib.bankid_registrator.configurations.AppConfig;
import cz.cas.lib.bankid_registrator.entities.patron.PatronStatus;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.services.PatronService;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test PatronService
 * 
 * <p><b>Usage:</b></p>
 * Check the `RETIREMENT_AGE` and adjust it if needed. Then you can run the tests with:
 * <pre>{@code
 * ./mvnw test -Dtest=PatronServiceTest
 * }</pre>
 */
@SpringBootTest
class PatronServiceTest
{
    @Autowired
    private PatronService patronService;

    private AppConfig appConfig;

    private static final int RETIREMENT_AGE = 70; // Set retirement age for testing, this will override the value from the application.properties
    private static final String DATE_FORMAT = "yyyyMMdd"; // Format used for `patron.birthDate`
    private static final Logger logger = LoggerFactory.getLogger(PatronServiceTest.class);

    @BeforeEach
    void setUp() {
        this.appConfig = Mockito.mock(AppConfig.class);
        Mockito.when(this.appConfig.getRetirementAge()).thenReturn(PatronServiceTest.RETIREMENT_AGE);
        this.patronService = new PatronService(this.appConfig, null, null);
    }

    /**
     * Helper method to generate a dynamic birth date based on age.
     */
    private String getBirthDateForAge(int age) {
        LocalDate birthDate = LocalDate.now().minusYears(age);
        return birthDate.format(DateTimeFormatter.ofPattern(PatronServiceTest.DATE_FORMAT));
    }

    /**
     * Test determinePatronStatus for a CAS employee
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronStatus_NewRegistrationCasEmployee
     * }</pre>
     */
    @Test
    void testDeterminePatronStatus_NewRegistrationCasEmployee() {
        Patron patron = new Patron();
        patron.setIsCasEmployee(true);

        PatronStatus status = this.patronService.determinePatronStatus(patron);

        assertEquals(PatronStatus.STATUS_03, status, "CAS employee should get STATUS_03.");
    }

    /**
     * Test determinePatronStatus for a retiree (age >= retirementAge)
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronStatus_NewRegistrationRetiree
     * }</pre>
     */
    @Test
    void testDeterminePatronStatus_NewRegistrationRetiree() {
        Patron patron = new Patron();
        patron.setIsCasEmployee(false);
        patron.setBirthDate(getBirthDateForAge(RETIREMENT_AGE + 5)); // Patron is 5 years older than the retirement age

        PatronStatus status = this.patronService.determinePatronStatus(patron);

        assertEquals(PatronStatus.STATUS_10, status, "Retiree should get STATUS_10.");
    }

    /**
     * Test determinePatronStatus for a regular user (age < retirementAge)
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronStatus_NewRegistrationRegularUser
     * }</pre>
     */
    @Test
    void testDeterminePatronStatus_NewRegistrationRegularUser() {
        Patron patron = new Patron();
        patron.setIsCasEmployee(false);
        patron.setBirthDate(getBirthDateForAge(30)); // Patron is 30 years old

        PatronStatus status = this.patronService.determinePatronStatus(patron);

        assertEquals(PatronStatus.STATUS_16, status, "Regular user should get STATUS_16.");
    }

    /**
     * Test determinePatronStatus for a patron renewing membership before expiry (future expiry date used for age check), calculated on the last day before patron reaches retirement age, patron reaches the retirement age after the midnight of that day.
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronStatus_ActiveMembershipRenewal_LastDayBeforeRetirement
     * }</pre>
     */
    @Test
    void testDeterminePatronStatus_ActiveMembershipRenewal_LastDayBeforeRetirement() {
        Patron patron = new Patron();
        patron.setIsCasEmployee(false);
        patron.setBirthDate(getBirthDateForAge(RETIREMENT_AGE - 5)); // Patron is 5 years younger than retirement
        patron.setExpiryDate(DateUtils.addYearsToToday(5, "dd/MM/yyyy")); // Expiry in 5 years, on the last day before retirement

        logger.info("LastDayBeforeRetirement patron.getBirthDate() " + patron.getBirthDate());
        logger.info("LastDayBeforeRetirement patron.getExpiryDate() " + patron.getExpiryDate());

        PatronStatus status = this.patronService.determinePatronStatus(patron);

        assertEquals(PatronStatus.STATUS_16, status, "Since the patron will have 1 day to retirement when the new membership starts, they should still get STATUS_16.");
    }

    /**
     * Test determinePatronStatus for a patron renewing membership before expiry (future expiry date used for age check), calculated on the first day of patron's retirement.
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronStatus_ActiveMembershipRenewal_FirstDayOfRetirement
     * }</pre>
     */
    @Test
    void testDeterminePatronStatus_ActiveMembershipRenewal_FirstDayOfRetirement() {
        Patron patron = new Patron();
        patron.setIsCasEmployee(false);
        patron.setBirthDate(getBirthDateForAge(RETIREMENT_AGE - 5)); // Patron is 5 years younger than retirement

        String lastDayBeforeRetirement = DateUtils.addYearsToToday(5, "dd/MM/yyyy");
        String firstDayOfRetirement = DateUtils.addDaysToDateString(lastDayBeforeRetirement, 1, "dd/MM/yyyy", "dd/MM/yyyy");

        patron.setExpiryDate(firstDayOfRetirement); // Expiry in 5 years + 1 day, on patron's very first retirement day

        logger.info("FirstDayOfRetirement patron.getBirthDate() " + patron.getBirthDate());
        logger.info("FirstDayOfRetirement patron.getExpiryDate() " + patron.getExpiryDate());

        PatronStatus status = this.patronService.determinePatronStatus(patron);

        assertEquals(PatronStatus.STATUS_10, status, "Since the patron has just reached retirement age when the new membership starts, they should get STATUS_10.");
    }

    /**
     * Test determinePatronStatus for a patron renewing membership after expiry (current date used for age check)
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronStatus_ExpiredMembershipRenewal
     * }</pre>
     */
    @Test
    void testDeterminePatronStatus_ExpiredMembershipRenewal() {
        Patron patron = new Patron();
        patron.setIsCasEmployee(false);
        patron.setBirthDate(getBirthDateForAge(RETIREMENT_AGE - 5)); // Patron is still younger than retirement age
        patron.setExpiryDate(DateUtils.addDaysToToday(-365, "dd/MM/yyyy")); // Expired 1 year ago

        PatronStatus status = this.patronService.determinePatronStatus(patron);

        assertEquals(PatronStatus.STATUS_16, status, "Since the patron is still under retirement age today, they should get STATUS_16.");
    }

    /**
     * Test determinePatronStatus for a patron who reaches retirement age exactly on expiry date
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronStatus_ExactRetirementOnExpiryDate
     * }</pre>
     */
    @Test
    void testDeterminePatronStatus_ExactRetirementOnExpiryDate() {
        Patron patron = new Patron();
        patron.setIsCasEmployee(false);
        patron.setBirthDate(getBirthDateForAge(RETIREMENT_AGE)); // Patron is exactly at retirement age
        patron.setExpiryDate(DateUtils.addDaysToToday(0, "dd/MM/yyyy")); // Expiry date is today

        PatronStatus status = this.patronService.determinePatronStatus(patron);

        assertEquals(PatronStatus.STATUS_10, status, "Since the patron is exactly at retirement age, they should get STATUS_10.");
    }

    /**
     * Test determinePatronExpiryDate for a new patron registration (new membership, no previous expiry date)
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronExpiryDate_NewRegistration
     * }</pre>
     */
    @Test
    void testDeterminePatronExpiryDate_NewRegistration() {
        Patron patron = new Patron();
        patron.setStatus(PatronStatus.STATUS_16.getId());
        patron.setExpiryDate(null); // No previous expiry because it is a new patron registration

        String expectedExpiryDate = DateUtils.addDaysToToday(PatronStatus.STATUS_16.getMembershipLength(), "dd/MM/yyyy");
        String actualExpiryDate = this.patronService.determinePatronExpiryDate(patron);

        assertEquals(expectedExpiryDate, actualExpiryDate, "Expiry date for a new membership should be calculated from today.");
    }

    /**
     * Test determinePatronExpiryDate for a membership renewal (still active membership)
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronExpiryDate_ActiveMembershipRenewal
     * }</pre>
     */
    @Test
    void testDeterminePatronExpiryDate_ActiveMembershipRenewal() {
        Patron patron = new Patron();
        patron.setStatus(PatronStatus.STATUS_16.getId());
        patron.setExpiryDate(DateUtils.addDaysToToday(10, "dd/MM/yyyy")); // Expires in 10 days

        String expectedExpiryDate = DateUtils.addDaysToDateString(
            patron.getExpiryDate(),
            PatronStatus.STATUS_16.getMembershipLength(),
            "dd/MM/yyyy",
            "dd/MM/yyyy"
        );
        String actualExpiryDate = this.patronService.determinePatronExpiryDate(patron);

        assertEquals(expectedExpiryDate, actualExpiryDate, "Expiry date should be calculated from the current expiry date.");
    }

    /**
     * Test determinePatronExpiryDate for a membership renewal when already expired
     * 
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * ./mvnw test -Dtest=PatronServiceTest#testDeterminePatronExpiryDate_ExpiredMembershipRenewal
     * }</pre>
     */
    @Test
    void testDeterminePatronExpiryDate_ExpiredMembershipRenewal() {
        Patron patron = new Patron();
        patron.setStatus(PatronStatus.STATUS_16.getId());
        patron.setExpiryDate(DateUtils.addDaysToToday(-10, "dd/MM/yyyy")); // Expired 10 days ago

        String expectedExpiryDate = DateUtils.addDaysToToday(PatronStatus.STATUS_16.getMembershipLength(), "dd/MM/yyyy");
        String actualExpiryDate = this.patronService.determinePatronExpiryDate(patron);

        assertEquals(expectedExpiryDate, actualExpiryDate, "Expiry date should be calculated from today if the membership is expired.");
    }
}
