package cz.cas.lib.bankid_registrator;

import cz.cas.lib.bankid_registrator.services.EmailService;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.fail;

/** Testing email service related functionalities
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * ./mvnw test -Dtest=EmailServiceTest
 * }</pre> */
@SpringBootTest
public class EmailServiceTest
{
    @Autowired
    private EmailService emailService;

    private static final Logger logger = LoggerFactory.getLogger(AlephServiceTest.class);

    /**
     * Test email templates rendering
     */
    @Test
    void testEmailTemplates()
    {
        String emailTo = "test@example.com";
        String alephPatronBarcode = "1234567890";

        // CZECH
        // Send a new registration confirmation email to the newly created member (Aleph patron) who IS NOT a CAS employee
        boolean isEmployee = false;
        String membershipExpiryDate = DateUtils.getLastDateOfCurrentMonth("dd/MM/yyyy");
        String lang = "cs";

        try {
            logger.info("Sending new registration email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailNewRegistration(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send a new registration confirmation email to the newly created member (Aleph patron) who IS a CAS employee
        isEmployee = true;

        try {
            logger.info("Sending new registration email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailNewRegistration(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send a membership renewal request confirmation email to a member (Aleph patron) who IS NOT a CAS employee
        isEmployee = false;

        try {
            logger.info("Sending membership renewal email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailMembershipRenewal(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send a membership renewal request confirmation email to a member (Aleph patron) who IS a CAS employee
        isEmployee = true;

        try {
            logger.info("Sending membership renewal email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailMembershipRenewal(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send an email with the link for resetting the identity password (Aleph patron password)
        String link = "https://example.com/reset-password";

        try {
            logger.info("Sending identity password reset email with parameters: " + emailTo + ", " + link + ", " + lang);
            this.emailService.sendEmailIdentityPasswordReset(emailTo, link, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }


        // ENGLISH
        // Send a new registration confirmation email to the newly created member (Aleph patron) who IS NOT a CAS employee
        isEmployee = false;
        lang = "en";

        try {
            logger.info("Sending new registration email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailNewRegistration(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send a new registration confirmation email to the newly created member (Aleph patron) who IS a CAS employee
        isEmployee = true;

        try {
            logger.info("Sending new registration email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailNewRegistration(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send a membership renewal request confirmation email to a member (Aleph patron) who IS NOT a CAS employee
        isEmployee = false;

        try {
            logger.info("Sending membership renewal email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailMembershipRenewal(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send a membership renewal request confirmation email to a member (Aleph patron) who IS a CAS employee
        isEmployee = true;

        try {
            logger.info("Sending membership renewal email with parameters: " + emailTo + ", " + alephPatronBarcode + ", " + isEmployee + ", " + membershipExpiryDate + ", " + lang);
            this.emailService.sendEmailMembershipRenewal(emailTo, alephPatronBarcode, isEmployee, membershipExpiryDate, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }

        // Send an email with the link for resetting the identity password (Aleph patron password)
        link = "https://example.com/reset-password";

        try {
            logger.info("Sending identity password reset email with parameters: " + emailTo + ", " + link + ", " + lang);
            this.emailService.sendEmailIdentityPasswordReset(emailTo, link, new Locale(lang));
        } catch (Exception e) {
            fail("Error sending email: " + e.getMessage());
        }
    }
}
