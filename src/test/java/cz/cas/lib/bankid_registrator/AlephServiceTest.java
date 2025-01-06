package cz.cas.lib.bankid_registrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.AddressType;
import cz.cas.lib.bankid_registrator.entities.entity.Gender;
import cz.cas.lib.bankid_registrator.entities.entity.MaritalStatus;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.LocalAlephService;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import cz.cas.lib.bankid_registrator.validators.PatronDTOValidator;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
    private LocalAlephService localAlephService;

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

    /**
     * Test Aleph patron existence check by name and birth date even if Bank iD service returns user profile with uppercase name (`family_name`, `middle_name`, `given_name`).
     * 
     * <p>Example usage:</p>
     * First modify the `customName`, `customGiveName`, `customFamilyName`, `customMiddleName`, `customBirthDate` variables in this test method - fill them with some existing Aleph patron's data but in uppercase. After that run the test:
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testGetAlephPatronIdByNameAndBirth_shouldNotBeEmpty_ifBankidProfileNamesUppercase
     * }</pre>
     */
    @Test
    void testGetAlephPatronIdByNameAndBirth_shouldNotBeEmpty_ifBankidProfileNamesUppercase()
    {
        // Setup custom user profile data which would normally be provided by Bank iD service when the user verifies their identity
        String customName = "MGR. JOE DOE";
        String customGiveName = "JOE";
        String customFamilyName = "DOE";
        String customMiddleName = "";
        String customBirthDate = "2003-06-09";  // yyyy-MM-dd

        // The `localAlephService.generateTestingMname` method generates a random middle name but we want to use a custom one defined above
        LocalAlephService spyLocalAlephService = Mockito.spy(localAlephService);
        Mockito.doReturn(customMiddleName).when(spyLocalAlephService).generateTestingMname();

        Connect userInfo = new Connect(
            customName,
            customGiveName,
            customFamilyName,
            null,
            "Johnny",
            "johnny.doe",
            "john.doe@example.com",
            true,
            "male",
            customBirthDate,
            "Europe/Prague",
            "cs_CZ",
            "+420123456789",
            true,
            System.currentTimeMillis(),
            "unique-sub-id",
            "txn-12345",
            null,
            null
        );

        ArrayList<Address> addresses = new ArrayList<>();
        addresses.add(new Address(
            AddressType.PERMANENT_RESIDENCE,
            "V Parku",
            "2308",
            "8",
            null,
            "Praha",
            "Chodov",
            "14800",
            "CZ",
            "26013691"
        ));

        Identify userProfile = new Identify(
            null,
            null,
            customGiveName,
            customFamilyName,
            null,
            "+420123456789",
            "john.doe@example.com",
            addresses,
            customBirthDate,
            24,
            null,
            Gender.male,
            "900101/1234",
            "Czech Republic",
            "Prague",
            "Czech",
            new String[] { "Czech" },
            MaritalStatus.SINGLE,
            null,
            null,
            null,
            false,
            true,
            false,
            System.currentTimeMillis(),
            "unique-sub-id",
            "txn-12345",
            null
        );

        // Mapping BankID user data to a Patron entity (so-called "BankId patron")
        Map<String, Object> bankIdPatronCreation = spyLocalAlephService.newPatron(userInfo, userProfile);

        Patron bankIdPatron = (Patron) bankIdPatronCreation.get("patron");

        try {
            logger.info("patron: {}", bankIdPatron.toJson());
        } catch (JsonProcessingException e) {
            logger.error("Error converting patron to JSON", e);
        }

        // Check that the BankId patron exists in Aleph even though the names are in uppercase and even though the `userInfo.name` contains a title (`Mgr.`, `PhD`, etc.)
        assertFalse(
            bankIdPatron.isNew(),
            "Patron does not exist in Aleph."
        );
    }
}
