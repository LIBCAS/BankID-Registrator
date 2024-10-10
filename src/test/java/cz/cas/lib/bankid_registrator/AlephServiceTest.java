package cz.cas.lib.bankid_registrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import cz.cas.lib.bankid_registrator.validators.PatronDTOValidator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AlephServiceTest
{
    @Autowired
    private AlephService alephService;

    @Autowired
    private PatronDTOValidator patronDTOValidator;

    private static final Logger logger = LoggerFactory.getLogger(AlephServiceTest.class);

    /**
     * Test for updating Aleph patron. 
     * These changes will be applied to the patron:
     * - New e-mail
     * - Membership renewed (membership status 16, expiration date extended)
     * - New RFID
     * 
     * <p>System properties required:</p>
     * <ul>
     *   <li><b>patron</b>: The Aleph ID of the patron to be updated.</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ./mvnw -Dtest=AlephServiceTest -Dpatron=PREFIX12345 test
     * }</pre>
     */
    @Test
    void testUpdatePatron()
    {
        String patronAlephId = System.getProperty("patron");
        assertNotNull(patronAlephId, "The system property 'patron' must be set.");

        String dtNow = DateUtils.getDateTime("yyMMddHHmmss");

        // Retrieve a patron from Aleph
        Map<String, Object> alephPatronSearch = this.alephService.getAlephPatron(patronAlephId, true);
        assertFalse(
            alephPatronSearch.containsKey("error"), 
            "An error occurred while retrieving the patron: " + alephPatronSearch.get("error")
        );
        Patron alephPatron = (Patron) alephPatronSearch.get("patron");

        // This will store the final patron with updated data
        Patron patronNew = new Patron(alephPatron);
        patronNew.setBirthDate(DateUtils.convertDateFormat(patronNew.getBirthDate(), "dd-MM-yyyy", "yyyyMMdd"));

        // Setting new Patron's data in a PatronDTO object
        PatronDTO patronNewDTO = new PatronDTO();
        patronNewDTO.setEmail("t" + dtNow + "@test.com");
        patronNewDTO.setIsCasEmployee(false);
        patronNewDTO.setDeclaration1(true);
        patronNewDTO.setDeclaration2(true);
        patronNewDTO.setDeclaration3(true);
        patronNewDTO.setRfid("T" + dtNow);

        // Validate the PatronDTO
        BindingResult bindingResult = new BeanPropertyBindingResult(patronNewDTO, "patronNewDTO");
        patronDTOValidator.validate(patronNewDTO, bindingResult);

        // Check for validation errors
        assertFalse(bindingResult.hasErrors(), "Validation errors: " + bindingResult.getAllErrors());

        // Assign the new data to the final patron
        patronNew.update(patronNewDTO);
        try {
            logger.info("FINAL PATRON WITH UPDATES: {}", patronNew.toJson());
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse patron data: {}", e.getMessage());
        }

        // Update the patron in Aleph
        Map<String, Object> patronUpdate = this.alephService.updatePatron(patronNew, alephPatron);

        logger.info("RESULT: {}", patronUpdate);

        if (patronUpdate.containsKey("error")) {
            logger.error("Error updating patron: {}", patronUpdate.get("error"));
        }

        assertTrue(
            patronUpdate.containsKey("success") && (Boolean) patronUpdate.get("success"),
            "Patron update was not successful."
        );
    }
}
