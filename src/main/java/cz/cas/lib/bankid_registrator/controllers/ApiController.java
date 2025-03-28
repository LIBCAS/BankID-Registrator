package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.ApiConfig;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.IdentityActivityService;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.LdapService;
import cz.cas.lib.bankid_registrator.services.MapyCzService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotBlank;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * API controller with endpoints for front-end
 */
@RestController
@RequestMapping("/api")
public class ApiController extends ApiControllerAbstract
{
    private final PatronService patronService;
    private final AlephService alephService;
    private final IdentityService identityService;
    private final IdentityActivityService identityActivityService;
    private final MapyCzService mapyCzService;
    private final LdapService ldapService;
    private final TokenService tokenService;
    private final IdentityAuthService identityAuthService;

    public ApiController(
        MessageSource messageSource, 
        ApiConfig apiConfig, 
        PatronService patronService, 
        AlephService alephService, 
        IdentityService identityService, 
        IdentityActivityService identityActivityService,
        MapyCzService mapyCzService,
        LdapService ldapService,
        TokenService tokenService,
        IdentityAuthService identityAuthService
    ) {
        super(messageSource, apiConfig);
        this.patronService = patronService;
        this.alephService = alephService;
        this.identityService = identityService;
        this.identityActivityService = identityActivityService;
        this.mapyCzService = mapyCzService;
        this.ldapService = ldapService;
        this.tokenService = tokenService;
        this.identityAuthService = identityAuthService;
    }

    /**
     * Check if the given RFID is already in use for any other patron except the one being registered
     * @param rfid
     * @param patronSysId - Patron system ID
     * @return ResponseEntity<Map<String, Object>>
     */
    @PostMapping("/check-rfid")
    public ResponseEntity<Map<String, Object>> checkRfid(
        @RequestParam @NotBlank String rfid, 
        @RequestParam @NotBlank String patronSysId
    ) {
        Long patronSysIdLong = Long.parseLong(patronSysId);
        String bid = this.patronService.getBankIdSubById(patronSysIdLong);
        boolean isContinuable = this.patronService.isProcessing(bid);

        // Check the validity of the request by checking if the application is currently working with the given bankIdSub, i.e. if the associated patron is being processed
        if (!isContinuable) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Map<String, Object> result = new HashMap<>();

        String patronId = this.patronService.getPatronIdById(patronSysIdLong);

        result.put("result", this.alephService.isRfidInUse(rfid, patronId));

        return ResponseEntity.ok(result);
    }

    /**
     * Check if the given email is already in use for any other patron except the one being registered
     * @param email
     * @param patronSysId - Patron system ID
     * @return ResponseEntity<Map<String, Object>>
     */
    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(
        @RequestParam @NotBlank String email, 
        @RequestParam @NotBlank String patronSysId
    ) {
        Long patronSysIdLong = Long.parseLong(patronSysId);
        String bid = this.patronService.getBankIdSubById(patronSysIdLong);
        boolean isContinuable = this.patronService.isProcessing(bid);

        // Check the validity of the request by checking if the application is currently working with the given bankIdSub, i.e. if the associated patron is being processed
        if (!isContinuable) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Map<String, Object> result = new HashMap<>();

        String patronId = this.patronService.getPatronIdById(patronSysIdLong);

        result.put("result", this.alephService.isEmailInUse(email, patronId));

        return ResponseEntity.ok(result);
    }

    /**
     * Suggest addresses based on the given query using the Mapy.cz API
     * @param query
     * @return
     */
    @GetMapping("/suggest-address/{query}")
    public Mono<String> suggestAddress(@PathVariable String query)
    {
        return this.mapyCzService.suggestAddress(query);
    }

    /**
     * Check if an account with the given email exists in the LDAP
     * @param username Aleph patron barcode
     * @param token
     * @return
     */
    @GetMapping("/check-ldap-account/{username}")
    public ResponseEntity<Map<String, Object>> checkLdapAccount(
        @PathVariable String username,
        @RequestParam("token") String token
    ) {
        if (!this.tokenService.isApiTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long patronSysIdLong = Long.parseLong(this.tokenService.extractClientIdFromApiToken(token));
        String bid = this.patronService.getBankIdSubById(patronSysIdLong);
        boolean isContinuable = this.patronService.isProcessing(bid);

        // Check the validity of the request by checking if the application is currently working with the given bankIdSub, i.e. if the associated patron is being processed
        if (!isContinuable) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        boolean accountExists = ldapService.accountExistsByUsername(username);

        Map<String, Object> result = new HashMap<>();
        result.put("result", accountExists);

        if (accountExists) {
            this.tokenService.invalidateToken(token);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Check if an account with the given login (username + password) exists in the LDAP
     * @param username Aleph patron ID, not Aleph patron barcode
     * @param token
     * @param request
     * @return
     */
    @GetMapping("/check-ldap-account-login/{username}")
    public ResponseEntity<Map<String, Object>> checkLdapAccountLogin(
        @PathVariable String username, 
        @RequestParam("token") String token, 
        HttpServletRequest request
    ) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String password = (String) session.getAttribute("patronPassword");
        if (password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!this.tokenService.isApiTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Long patronSysIdLong = Long.parseLong(this.tokenService.extractClientIdFromApiToken(token));
        String bid = this.patronService.getBankIdSubById(patronSysIdLong);
        boolean isContinuable = this.patronService.isProcessing(bid);

        // Check the validity of the request by checking if the application is currently working with the given bankIdSub, i.e. if the associated patron is being processed
        if (!isContinuable) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        boolean accountExists = ldapService.accountExistsByLogin(username, password);

        Map<String, Object> result = new HashMap<>();
        result.put("result", accountExists);

        if (accountExists) {
            session.removeAttribute("patronPassword");
            this.tokenService.invalidateToken(token);
            this.identityAuthService.logout(request);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Logout the Bank iD verified identity
     * @param token
     * @param request
     * @return ResponseEntity<Map<String, Object>>
     */
    @GetMapping("/identity/logout")
    public ResponseEntity<Map<String, Object>> routeLogout(
        @RequestParam("token") String token, 
        HttpServletRequest request
    ) {
        if (!this.tokenService.isApiTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        this.tokenService.invalidateToken(token);
        this.identityAuthService.logout(request);

        Map<String, Object> result = new HashMap<>();
        result.put("result", true);

        return ResponseEntity.ok(result);
    }

    // /**
    //  * !!! ONLY FOR TESTING PURPOSES !!!
    //  * Empty the `identities` and `identity_activities` tables
    //  * @param session
    //  * @return
    //  */
    // @GetMapping("/reset-identities")
    // public ResponseEntity<Map<String, Object>> resetIdentities(HttpSession session)
    // {
    //     Map<String, Object> result = new HashMap<>();

    //     try {
    //         // Get patron IDs of all BankID-verified identities which have an Aleph patron linked to them
    //         String[] alephIds = this.identityService.getAllAlephIds();

    //         // Delete those identities from the application database
    //         this.identityActivityService.emptyTable();
    //         this.identityService.emptyTable();

    //         // Delete the reference to those identities from the Aleph database
    //         for (String alephId : alephIds) {
    //             this.alephService.deletePatronBankIdSub(alephId);
    //         }

    //         result.put("result", true);
    //         result.put("message", alephIds.length == 1 ? "1 identity deleted" : (alephIds.length + " identities deleted"));
    //     } catch (Exception e) {
    //         result.put("result", false);
    //         result.put("message", e.getMessage());
    //     }

    //     return ResponseEntity.ok(result);
    // }
}
