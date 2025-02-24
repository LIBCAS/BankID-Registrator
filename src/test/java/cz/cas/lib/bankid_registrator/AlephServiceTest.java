package cz.cas.lib.bankid_registrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.oracle.OracleRepository;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.AddressType;
import cz.cas.lib.bankid_registrator.entities.entity.Gender;
import cz.cas.lib.bankid_registrator.entities.entity.MaritalStatus;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.LocalAlephService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import cz.cas.lib.bankid_registrator.validators.PatronDTOValidator;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
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
    private PatronService patronService;

    @Autowired
    private LocalAlephService localAlephService;

    @Autowired
    private PatronDTOValidator patronDTOValidator;

    @Autowired
    private MainConfiguration mainConfig;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private OracleRepository oracleRepository;

    private ResourceLoader resourceLoader;

    private AlephServiceConfig alephServiceConfig;

    private static final String[] ALEPH_PATRONID_PREFIXES = {"LIB1", "LIB2"}; // Set Aleph patron prefixes for testing, this will override the value from the application.properties

    private static final Logger logger = LoggerFactory.getLogger(AlephServiceTest.class);

    @BeforeEach
    void setUp() {
        this.alephServiceConfig = Mockito.mock(AlephServiceConfig.class);
        Mockito.when(this.alephServiceConfig.getPatronidPrefixes()).thenReturn(AlephServiceTest.ALEPH_PATRONID_PREFIXES);

        this.alephService = new AlephService(this.mainConfig, this.alephServiceConfig, this.identityService, this.oracleRepository, this.resourceLoader);

        logger.info("For testing, Aleph patron ID prefixes are set to: " + String.join(", ", this.alephServiceConfig.getPatronidPrefixes()));
    }

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
     * ./mvnw -Dtest=AlephServiceTest#testUpdatePatron -Dpatron=PREFIX12345 test
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
        patronNewDTO.setSmsNumber(patronNew.getSmsNumber());
        patronNewDTO.setIsCasEmployee(false);
        patronNewDTO.setUseContactAddress(true);
        patronNewDTO.setContactAddress0(patronNew.getContactAddress0());
        patronNewDTO.setContactAddress1(patronNew.getContactAddress1());
        patronNewDTO.setContactAddress2(patronNew.getContactAddress2());
        patronNewDTO.setContactZip(patronNew.getContactZip());
        patronNewDTO.setDeclaration1(true);
        patronNewDTO.setDeclaration2(true);
        patronNewDTO.setDeclaration3(true);
        patronNewDTO.setDeclaration4(true);
        patronNewDTO.setRfid("T" + dtNow);

        try {
            logger.info("PATRON UPDATES: {}", patronNewDTO.toJson());
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse patron updates data: {}", e.getMessage());
        }

        // Validate the PatronDTO
        BindingResult bindingResult = new BeanPropertyBindingResult(patronNewDTO, "patronNewDTO");
        patronDTOValidator.validate(patronNewDTO, bindingResult);

        // Check for validation errors
        assertFalse(bindingResult.hasErrors(), "Validation errors: " + bindingResult.getAllErrors());

        // Assign the new data to the final patron
        patronNew.update(patronNewDTO);
        patronNew.setStatus(this.patronService.determinePatronStatus(patronNew).getId());
        patronNew.setExpiryDate(this.patronService.determinePatronExpiryDate(patronNew));
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

    /**
     * Tests if an email is not used in Aleph while searching among all patrons whose patron ID contains of of the prefixes defined in `ALEPH_PATRONID_PREFIXES`.
     *
     * <p><b>Usage:</b></p>
     * <p>Check and modify these variables: `ALEPH_PATRONID_PREFIXES`, `searchEmail`</p>
     * <p>Then run the test via:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testIsEmailInUse_emailOnly_shouldReturnFalse_ifNoEmailFound
     * }</pre>
     */
    @Test
    void testIsEmailInUse_emailOnly_shouldReturnFalse_ifNoEmailFound() {
        // Set an non-existing patron email
        String searchEmail = "non-existing@email.com";

        boolean isEmailInUse = alephService.isEmailInUse(searchEmail, null);

        assertFalse(isEmailInUse, "Expected email to not be in use, but it was used by 1 or more different patrons.");
    }

    /**
     * Tests if an email is in use in Aleph while searching among all patrons whose patron ID contains of of the prefixes defined in `ALEPH_PATRONID_PREFIXES`.
     *
     * <p><b>Usage:</b></p>
     * <p>Check and modify these variables: `ALEPH_PATRONID_PREFIXES`, `searchEmail`</p>
     * <p>Then run the test via:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testIsEmailInUse_emailOnly_shouldReturnTrue_ifEmailFound
     * }</pre>
     */
    @Test
    void testIsEmailInUse_emailOnly_shouldReturnTrue_ifEmailFound() {
        // Set an existing patron email whose patron has an ID with one of the prefixes defined in `ALEPH_PATRONID_PREFIXES`:
        String searchEmail = "test@email.com";

        boolean isEmailInUse = alephService.isEmailInUse(searchEmail, null);

        assertTrue(isEmailInUse, "Expected email to be in use, but it was not.");
    }

    /**
     * Tests if an email is in use in Aleph while searching among all patrons whose patron ID contains of of the prefixes defined in `ALEPH_PATRONID_PREFIXES` excluding the given patron ID. I.e. this tests if an email is used only by the given patron.
     *
     * <p><b>Usage:</b></p>
     * <p>Check and modify these variables: `ALEPH_PATRONID_PREFIXES`, `searchEmail`, `excludePatronId`</p>
     * <p>Then run the test via:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testIsEmailInUse_excludePatronId_shouldReturnFalse_ifNoEmailFound
     * }</pre>
     */
    @Test
    void testIsEmailInUse_excludePatronId_shouldReturnFalse_ifNoEmailFound() {
        // Set an existing patron email whose patron has an ID with one of the prefixes defined in `ALEPH_PATRONID_PREFIXES`:
        String searchEmail = "test@email.com";

        // Set an ID of a patron who has an email defined in `searchEmail`:
        String excludePatronId = "LIB10001";

        boolean isEmailInUse = alephService.isEmailInUse(searchEmail, excludePatronId);

        assertFalse(isEmailInUse, "Expected email to be in used only by the patron " + excludePatronId + ", but it was used by 1 or more different patrons as well.");
    }
}
