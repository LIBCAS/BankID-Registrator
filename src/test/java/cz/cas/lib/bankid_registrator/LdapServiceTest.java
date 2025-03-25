package cz.cas.lib.bankid_registrator;

import cz.cas.lib.bankid_registrator.services.LdapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the LdapService class.
 * 
 * <p>System properties required:</p>
 * <ul>
 *  <li>ldapUsername: valid username for the LDAP account to test.</li>
 *  <li>ldapPassword: valid password for the LDAP account to test.</li>
 *  <li>invalidLdapUsername: invalid username for the LDAP account to test.</li>
 *  <li>invalidLdapPassword: invalid password for the LDAP account to test.</li>
 * </ul>
 * 
 * <p>Example usage with `test.properties`:</p>
 * <pre>{@code
 * ./mvnw test -Dtest=LdapServiceTest
 * }</pre>
 * or in order to override the properties:
 * <pre>{@code
 * ./mvnw test -Dtest=LdapServiceTest -DldapUsername=validUsername -DldapPassword=validPassword -DinvalidLdapUsername=invalidUsername -DinvalidLdapPassword=invalidPassword
 * }</pre>
 */
@SpringBootTest
public class LdapServiceTest
{
    @Autowired
    private LdapService ldapService;

    private static String ldapUsername;
    private static String ldapPassword;
    private static String invalidLdapUsername;
    private static String invalidLdapPassword;
    private static final Logger logger = LoggerFactory.getLogger(LdapServiceTest.class);

    @BeforeAll
    static void loadProperties() {
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream("src/test/resources/tests.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            logger.warn("Failed to load test properties from 'tests.properties'.", e);
        }

        LdapServiceTest.ldapUsername = System.getProperty("ldapUsername", properties.getProperty("ldapUsername"));
        LdapServiceTest.ldapPassword = System.getProperty("ldapPassword", properties.getProperty("ldapPassword"));
        LdapServiceTest.invalidLdapUsername = System.getProperty("invalidLdapUsername", properties.getProperty("invalidLdapUsername"));
        LdapServiceTest.invalidLdapPassword = System.getProperty("invalidLdapPassword", properties.getProperty("invalidLdapPassword"));
    }

    /**
     * Tests the accountExistsByLogin method with valid credentials.
     * 
     * @see LdapService#accountExistsByLogin(String, String)
     */
    @Test
    public void testAccountExistsByLogin_shouldReturnTrue_forValidLogin()
    {
        String validUsername = LdapServiceTest.ldapUsername;
        assertNotNull(validUsername, "The system property 'ldapUsername' must be set.");

        String validPassword = LdapServiceTest.ldapPassword;
        assertNotNull(validPassword, "The system property 'ldapPassword' must be set.");

        assertTrue(
            ldapService.accountExistsByLogin(validUsername, validPassword),
    "Expected the account to exist for valid credentials."
        );
    }

    /**
     * Tests the accountExistsByLogin method with invalid credentials.
     * 
     * @see LdapService#accountExistsByLogin(String, String)
     */
    @Test
    public void testAccountExistsByLogin_shouldReturnFalse_forInvalidLogin()
    {
        String invalidUsername = LdapServiceTest.invalidLdapUsername;
        assertNotNull(invalidUsername, "The system property 'invalidLdapUsername' must be set.");

        String invalidPassword = LdapServiceTest.invalidLdapPassword;
        assertNotNull(invalidPassword, "The system property 'invalidLdapPassword' must be set.");

        assertFalse(
            ldapService.accountExistsByLogin(invalidUsername, invalidPassword),
            "Expected the account not to exist for invalid credentials."
        );
    }
}
