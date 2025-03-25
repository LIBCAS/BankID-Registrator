package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.LdapServiceTest;
import cz.cas.lib.bankid_registrator.configurations.ApiConfig;
import cz.cas.lib.bankid_registrator.services.*;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.context.support.WithMockUser;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Integration tests for the ApiController class.
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
 * ./mvnw test -Dtest=ApiControllerTest
 * }</pre>
 * or in order to override the properties:
 * <pre>{@code
 * ./mvnw test -Dtest=ApiControllerTest -DldapUsername=validUsername -DldapPassword=validPassword
 * }</pre>
 */
@SpringBootTest
public class ApiControllerTest
{
    @Autowired
    private ApiController apiController;

    private MockMvc mockMvc;

    @MockBean
    private ApiConfig apiConfig;

    @MockBean
    private PatronService patronService;

    @MockBean
    private AlephService alephService;

    @MockBean
    private AlephServiceIface alephServiceIface;

    @MockBean
    private IdentityService identityService;

    @MockBean
    private IdentityActivityService identityActivityService;

    @MockBean
    private MapyCzService mapyCzService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private IdentityAuthService identityAuthService;

    private static String ldapUsername;
    private static String ldapPassword;
    private static String invalidLdapUsername;
    private static String invalidLdapPassword;

    private static final Logger logger = LoggerFactory.getLogger(LdapServiceTest.class);

    // Example values
    private static final String EXAMPLE_TOKEN = "sampleToken";
    private static final Long EXAMPLE_CLIENTID = 1L;
    private static final String EXAMPLE_BANKIDSUB = "sampleBankIdSub";

    @BeforeAll
    static void loadProperties() {
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream("src/test/resources/tests.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            logger.warn("Failed to load test properties from 'tests.properties'.", e);
        }

        ApiControllerTest.ldapUsername = System.getProperty("ldapUsername", properties.getProperty("ldapUsername"));
        ApiControllerTest.ldapPassword = System.getProperty("ldapPassword", properties.getProperty("ldapPassword"));
        ApiControllerTest.invalidLdapUsername = System.getProperty("invalidLdapUsername", properties.getProperty("invalidLdapUsername"));
        ApiControllerTest.invalidLdapPassword = System.getProperty("invalidLdapPassword", properties.getProperty("invalidLdapPassword"));
    }

    @BeforeEach
    public void setup()
    {
        this.mockMvc = MockMvcBuilders.standaloneSetup(apiController).build();

        when(tokenService.isApiTokenValid(EXAMPLE_TOKEN)).thenReturn(true);
        when(tokenService.extractClientIdFromApiToken(EXAMPLE_TOKEN)).thenReturn(EXAMPLE_CLIENTID.toString());
        when(patronService.getBankIdSubById(EXAMPLE_CLIENTID)).thenReturn(EXAMPLE_BANKIDSUB);
        when(patronService.isProcessing(EXAMPLE_BANKIDSUB)).thenReturn(true);
    }

    /**
     * Test the checkLdapAccountLogin method with valid credentials.
     * 
     * @throws Exception
     * @see ApiController#checkLdapAccountLogin(String, String, HttpServletRequest)
     */
    @Test
    @WithMockUser
    public void testCheckLdapAccountLogin_shouldReturnJsonWithTrue_forValidLogin() throws Exception
    {
        String validUsername = ApiControllerTest.ldapUsername;
        assertNotNull(validUsername, "The system property 'ldapUsername' must be set.");

        String validPassword = ApiControllerTest.ldapPassword;
        assertNotNull(validPassword, "The system property 'ldapPassword' must be set.");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("patronPassword", validPassword);

        mockMvc.perform(get("/api/check-ldap-account-login/" + validUsername)
            .param("token", EXAMPLE_TOKEN)
            .session(session)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("true"));
    }

    /**
     * Test the checkLdapAccountLogin method with invalid credentials.
     * 
     * @throws Exception
     * @see ApiController#checkLdapAccountLogin(String, String, HttpServletRequest)
     */
    @Test
    @WithMockUser
    public void testCheckLdapAccountLogin_shouldReturnJsonWithFalse_forInvalidLogin() throws Exception
    {
        String invalidUsername = ApiControllerTest.invalidLdapUsername;
        assertNotNull(invalidUsername, "The system property 'invalidLdapUsername' must be set.");

        String invalidPassword = ApiControllerTest.invalidLdapPassword;
        assertNotNull(invalidPassword, "The system property 'invalidLdapPassword' must be set.");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("patronPassword", invalidPassword);

        mockMvc.perform(get("/api/check-ldap-account-login/" + invalidUsername)
            .param("token", EXAMPLE_TOKEN)
            .sessionAttr("patronPassword", invalidPassword)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("false"));
    }
}
