package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.exceptions.PatronNotProcessableException;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API controller with endpoints for front-end
 */
@RestController
public class ApiController
{
    private final PatronService patronService;
    private final AlephService alephService;

    public ApiController(PatronService patronService, AlephService alephService) {
        this.patronService = patronService;
        this.alephService = alephService;
    }

    /*
     * Check if RFID is already in use for any other patron except the one being registered
     * @param rfid
     * @param bid - BankId sub
     * @param patronId - patron Aleph ID
     * @return ResponseEntity<Map<String, Object>>
     */
    @PostMapping("/api/check-rfid")
    public ResponseEntity<Map<String, Object>> checkRfid(
        @RequestParam @NotBlank String rfid, 
        @RequestParam @NotBlank String bid, 
        @RequestParam @Nullable @NotBlank String patronId
    ) {
        Boolean isContinuable = this.patronService.isProcessing(bid);

        if (!isContinuable) {
            throw new PatronNotProcessableException();
        }

        Map<String, Object> result = new HashMap<>();

        patronId = (patronId != null && !patronId.isEmpty()) ? patronId : null;

        result.put("result", this.alephService.isRfidInUse(rfid, patronId));

        return ResponseEntity.ok(result);
    }
}
