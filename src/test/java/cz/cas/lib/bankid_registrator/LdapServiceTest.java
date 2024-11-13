package cz.cas.lib.bankid_registrator;

import cz.cas.lib.bankid_registrator.services.LdapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for the LdapService class.
 * 
 * <p>System properties required:</p>
 * <ul>
 *  <li>ldapUsername: valid username for the LDAP account to test.</li>
 *  <li>ldapPassword: valid password for the LDAP account to test.</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * ./mvnw -Dtest=LdapServiceTest -DldapUsername=exampleUsername -DldapPassword=examplePassword test
 * }</pre>
 */
@SpringBootTest
public class LdapServiceTest
{
    @Autowired
    private LdapService ldapService;

    /**
     * Tests the accountExistsByLogin method with valid credentials.
     * 
     * @see LdapService#accountExistsByLogin(String, String)
     */
    @Test
    public void testAccountExistsByLogin_shouldReturnTrue_forValidLogin()
    {
        String validUsername = System.getProperty("ldapUsername");
        assertNotNull(validUsername, "The system property 'ldapUsername' must be set.");

        String validPassword = System.getProperty("ldapPassword");
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
        String invalidUsername = "nonexistentUser";
        String invalidPassword = "wrongPassword";

        assertFalse(
            ldapService.accountExistsByLogin(invalidUsername, invalidPassword),
            "Expected the account not to exist for invalid credentials."
        );
    }
}
