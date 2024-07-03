package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.exceptions.PatronNotProcessableException;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.IdentityActivityService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotBlank;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API controller with endpoints for front-end
 */
@RestController
public class ApiController extends ControllerAbstract
{
    private final PatronService patronService;
    private final AlephService alephService;
    private final IdentityService identityService;
    private final IdentityActivityService identityActivityService;

    public ApiController(MessageSource messageSource, PatronService patronService, AlephService alephService, IdentityService identityService, IdentityActivityService identityActivityService) {
        super(messageSource);
        this.patronService = patronService;
        this.alephService = alephService;
        this.identityService = identityService;
        this.identityActivityService = identityActivityService;
    }

    /*
     * Check if RFID is already in use for any other patron except the one being registered
     * @param rfid
     * @param patronId - Patron system ID
     * @return ResponseEntity<Map<String, Object>>
     */
    @PostMapping("/api/check-rfid")
    public ResponseEntity<Map<String, Object>> checkRfid(
        @RequestParam @NotBlank String rfid, 
        @RequestParam @NotBlank String patronSysId
    ) {
        Long patronSysIdLong = Long.parseLong(patronSysId);
        String bid = this.patronService.getBankIdSubById(patronSysIdLong);
        Boolean isContinuable = this.patronService.isProcessing(bid);

        if (!isContinuable) {
            throw new PatronNotProcessableException();
        }

        Map<String, Object> result = new HashMap<>();

        String patronId = this.patronService.getPatronIdById(patronSysIdLong);

        result.put("result", this.alephService.isRfidInUse(rfid, patronId));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/reset-identities")
    public ResponseEntity<Map<String, Object>> resetIdentities(HttpSession session)
    {
        this.identityActivityService.emptyTable();
        this.identityService.emptyTable();

        Map<String, Object> result = new HashMap<>();
        result.put("result", true);

        return ResponseEntity.ok(result);
    }
}
