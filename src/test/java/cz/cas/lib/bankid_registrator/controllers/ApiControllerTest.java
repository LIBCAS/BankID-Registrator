package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.ApiConfig;
import cz.cas.lib.bankid_registrator.services.*;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/**
 * Integration tests for the ApiController class.
 * 
 * <p>System properties required:</p>
 * <ul>
 *  <li><b>ldapUsername</b>: Valid LDAP username to be used for testing.</li>
 *  <li><b>ldapPassword</b>: Valid LDAP password to be used for testing.</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * ./mvnw -Dtest=ApiControllerTest -DldapUsername=exampleUsername -DldapPassword=examplePassword test
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

    // Example values
    private static final String EXAMPLE_TOKEN = "sampleToken";
    private static final Long EXAMPLE_CLIENTID = 1L;
    private static final String EXAMPLE_BANKIDSUB = "sampleBankIdSub";
    private static final String EXAMPLE_INVALID_USERNAME = "nonexistentUser";
    private static final String EXAMPLE_INVALID_PASSWORD = "wrongPassword";

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
        String username = System.getProperty("ldapUsername");
        assertNotNull(username, "The system property 'ldapUsername' must be set.");

        String password = System.getProperty("ldapPassword");
        assertNotNull(password, "The system property 'ldapPassword' must be set.");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("patronPassword", password);

        mockMvc.perform(get("/api/check-ldap-account-login/" + username)
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
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("patronPassword", EXAMPLE_INVALID_PASSWORD);

        mockMvc.perform(get("/api/check-ldap-account-login/" + EXAMPLE_INVALID_USERNAME)
            .param("token", EXAMPLE_TOKEN)
            .sessionAttr("patronPassword", EXAMPLE_INVALID_PASSWORD)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("false"));
    }
}
