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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
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

/**
 * Integration tests for the AlephService class.
 * 
 * <p>System properties required:</p>
 * <ul>
 *  <li><b>testingPatronId</b>: An ID of the testing Aleph patron - Do not use a production patron, use a testing one.</li>
 *  <li><b>useCustomAlephServiceConfig</b>: Whether to use custom Aleph service config specified in this test class via alephPatronPrefixes, alephServiceHost, alephServicePort, alephServiceRestApiUri, alephServiceWwwuser, alephServiceWwwPasswd
 *  
 * </ul>
 * 
 * If you want to use custom Aleph service configuration for this test i.e. if `useCustomAlephServiceConfig` is true, you will need to modify these variables:
 * - alephPatronPrefixes
 * - alephServiceHost
 * - alephServicePort
 * - alephServiceRestApiUri
 * - alephServiceWwwuser
 * - alephServiceWwwPasswd
 * 
 * <p>Example usage with `test.properties`:</p>
 * <pre>{@code
 *      ./mvnw test -Dtest=AlephServiceTest
 * }</pre>
 * or in order to override the properties:
 * <pre>{@code
 *      ./mvnw test -Dtest=AlephServiceTest -DtestingPatronId=PREFIX12345 -DuseCustomAlephServiceConfig=false -DpatronNonExistingEmail=non-existing@example.com -patronEmailInUse=used-email@example.com -patronEmailUnique=unique-email@example.com -patronWithUniqueEmail=LIB00001 ...
 * }</pre>
 */
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

    @Autowired
    private AlephServiceConfig prodAlephServiceConfig;  // Aleph service config used in production

    @Autowired
    private ResourceLoader resourceLoader;

    private AlephServiceConfig alephServiceConfig;  // Aleph service config used in this test

    private static String testingPatronId;
    private static String patronNonExistingEmail;
    private static String patronEmailInUse;
    private static String patronEmailUnique;
    private static String patronWithUniqueEmail;
    private static String test2PatronName;
    private static String test2PatronGivenName;
    private static String test2PatronFamilyName;
    private static String test2PatronMiddleName;
    private static String test2PatronBirthDate;
    
    private static boolean useCustomAlephServiceConfig = false; // By default this test class uses Aleph service config like in production

    private static String[] alephPatronPrefixes = {"LIB1", "LIB2"};
    private static String alephServiceHost = "https://aleph.test.com";
    private static String alephServicePort = "443";
    private static String alephServiceRestApiUri = "http://aleph.test.com:XXXX";
    private static String alephServiceWwwuser = "AlephNickname";
    private static String alephServiceWwwPasswd = "AlephPassword";

    private static final Logger logger = LoggerFactory.getLogger(AlephServiceTest.class);

    @BeforeAll
    static void loadProperties() {
        Properties properties = new Properties();

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream("src/test/resources/tests.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            logger.warn("Failed to load test properties from 'tests.properties'.", e);
        }

        AlephServiceTest.useCustomAlephServiceConfig = Boolean.parseBoolean(
            System.getProperty("useCustomAlephServiceConfig", 
            properties.getProperty("AlephServiceTest.useCustomAlephServiceConfig"))
        );

        AlephServiceTest.testingPatronId = System.getProperty("testingPatronId", properties.getProperty("AlephServiceTest.testingPatronId"));
        AlephServiceTest.patronNonExistingEmail = System.getProperty("patronNonExistingEmail", properties.getProperty("AlephServiceTest.patronNonExistingEmail"));
        AlephServiceTest.patronEmailInUse = System.getProperty("patronEmailInUse", properties.getProperty("AlephServiceTest.patronEmailInUse"));
        AlephServiceTest.patronEmailUnique = System.getProperty("patronEmailUnique", properties.getProperty("AlephServiceTest.patronEmailUnique"));
        AlephServiceTest.patronWithUniqueEmail = System.getProperty("patronWithUniqueEmail", properties.getProperty("AlephServiceTest.patronWithUniqueEmail"));
        AlephServiceTest.test2PatronName = System.getProperty("test2PatronName", properties.getProperty("AlephServiceTest.test2.patronName"));
        AlephServiceTest.test2PatronGivenName = System.getProperty("test2PatronGivenName", properties.getProperty("AlephServiceTest.test2.patronGivenName"));
        AlephServiceTest.test2PatronFamilyName = System.getProperty("test2PatronFamilyName", properties.getProperty("AlephServiceTest.test2.patronFamilyName"));
        AlephServiceTest.test2PatronMiddleName = System.getProperty("test2PatronMiddleName", properties.getProperty("AlephServiceTest.test2.patronMiddleName"));
        AlephServiceTest.test2PatronBirthDate = System.getProperty("test2PatronBirthDate", properties.getProperty("AlephServiceTest.test2.patronBirthDate"));
    }

    @BeforeEach
    void setUp() {
        if (AlephServiceTest.useCustomAlephServiceConfig) {
            this.alephServiceConfig = Mockito.mock(AlephServiceConfig.class);
            Mockito.when(this.alephServiceConfig.getPatronidPrefixes()).thenReturn(AlephServiceTest.alephPatronPrefixes);
            Mockito.when(this.alephServiceConfig.getHost()).thenReturn(AlephServiceTest.alephServiceHost);
            Mockito.when(this.alephServiceConfig.getPort()).thenReturn(AlephServiceTest.alephServicePort);
            Mockito.when(this.alephServiceConfig.getRestApiUri()).thenReturn(AlephServiceTest.alephServiceRestApiUri);
            Mockito.when(this.alephServiceConfig.getWwwuser()).thenReturn(AlephServiceTest.alephServiceWwwuser);
            Mockito.when(this.alephServiceConfig.getWwwpasswd()).thenReturn(AlephServiceTest.alephServiceWwwPasswd);
        } else {
            this.alephServiceConfig = this.prodAlephServiceConfig;
        }

        this.alephService = new AlephService(this.mainConfig, this.alephServiceConfig, this.identityService, this.oracleRepository, this.resourceLoader);
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
     *  <li><b>testingPatronId</b>: An ID of the testing Aleph patron - Do not use a production patron, use a testing one.</li>
     *  <li><b>useCustomAlephServiceConfig</b>: Whether to use custom Aleph service config specified in this test class via alephPatronPrefixes, alephServiceHost, alephServicePort, alephServiceRestApiUri, alephServiceWwwuser, alephServiceWwwPasswd
     * </ul>
     * 
     * If you want to use custom Aleph service configuration for this test i.e. if `useCustomAlephServiceConfig` is true, you will need to modify these variables:
     * - alephPatronPrefixes
     * - alephServiceHost
     * - alephServicePort
     * - alephServiceRestApiUri
     * - alephServiceWwwuser
     * - alephServiceWwwPasswd
     *
     * <p>Example usage with `test.properties`:</p>
     * <pre>{@code
     *      ./mvnw test -Dtest=AlephServiceTest#testUpdatePatron
     * }</pre>
     * or in order to override the properties:
     * <pre>{@code
     *      ./mvnw test -Dtest=AlephServiceTest#testUpdatePatron -DtestingPatronId=PREFIX12345 -DuseCustomAlephServiceConfig=false
     * }</pre>
     */
    @Test
    void testUpdatePatron()
    {
        String patronAlephId = AlephServiceTest.testingPatronId;
        assertNotNull(patronAlephId, "The system property 'testingPatronId' must be set.");

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
     * Test for retrieving an Aleph patron using Bank iD profile data even when the names are provided in uppercase.
     *
     * <p>This test ensures that Aleph can correctly find an existing patron even if the Bank iD profile returns uppercase names
     * (e.g. `family_name`, `middle_name`, `given_name`).</p>
     *
     * <p>System properties required:</p>
     * <ul>
     *     <li><b>test2PatronName</b>: Full name of the patron</li>
     *     <li><b>test2PatronGivenName</b>: Given name</li>
     *     <li><b>test2PatronFamilyName</b>: Family name</li>
     *     <li><b>test2PatronMiddleName</b>: Middle name</li>
     *     <li><b>test2PatronBirthDate</b>: Birth date in format yyyy-MM-dd</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testGetAlephPatronIdByNameAndBirth_shouldNotBeEmpty_ifBankidProfileNamesUppercase \
     *             -Dtest2PatronName=PHD. JOHN DOE \
     *             -Dtest2PatronGivenName=JOHN \
     *             -Dtest2PatronFamilyName=DOE \
     *             -Dtest2PatronMiddleName=JAMES \
     *             -Dtest2PatronBirthDate=2000-02-20
     * }</pre>
     */
    @Test
    void testGetAlephPatronIdByNameAndBirth_shouldNotBeEmpty_ifBankidProfileNamesUppercase()
    {
        // Setup custom user profile data which would normally be provided by Bank iD service when the user verifies their identity
        String customName = AlephServiceTest.test2PatronName;
        String customGivenName = AlephServiceTest.test2PatronGivenName;
        String customFamilyName = AlephServiceTest.test2PatronFamilyName;
        String customMiddleName = AlephServiceTest.test2PatronMiddleName;
        String customBirthDate = AlephServiceTest.test2PatronBirthDate; // yyyy-MM-dd

        assertNotNull(customName, "The system property 'test2PatronName' must be set.");
        assertNotNull(customGivenName, "The system property 'test2PatronGivenName' must be set.");
        assertNotNull(customFamilyName, "The system property 'test2PatronFamilyName' must be set.");
        assertNotNull(customMiddleName, "The system property 'test2PatronMiddleName' must be set.");
        assertNotNull(customBirthDate, "The system property 'test2PatronBirthDate' must be set.");

        // The `localAlephService.generateTestingMname` method generates a random middle name but we want to use a custom one defined above
        LocalAlephService spyLocalAlephService = Mockito.spy(localAlephService);
        Mockito.doReturn(customMiddleName).when(spyLocalAlephService).generateTestingMname();

        Connect userInfo = new Connect(
            customName,
            customGivenName,
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
            customGivenName,
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
     * Test for verifying that an unused email is not found among Aleph patrons.
     *
     * <p>This test checks if an email address, which is known to be unused, is not found when querying Aleph patrons
     * whose patron ID contains one of the prefixes defined in {@code AlephServiceTest.alephPatronPrefixes}.</p>
     *
     * <p>System properties required:</p>
     * <ul>
     *     <li><b>patronNonExistingEmail</b>: Email address that is expected to not be used by any patron</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testIsEmailInUse_emailOnly_shouldReturnFalse_ifNoEmailFound \
     *             -DpatronNonExistingEmail=nonexistent@example.com
     * }</pre>
     */
    @Test
    void testIsEmailInUse_emailOnly_shouldReturnFalse_ifNoEmailFound() {
        // A non-existing patron email
        String searchEmail = AlephServiceTest.patronNonExistingEmail;
        assertNotNull(searchEmail, "The system property 'patronNonExistingEmail' must be set.");        

        boolean isEmailInUse = alephService.isEmailInUse(searchEmail, null);

        assertFalse(isEmailInUse, "Expected email to not be in use, but it was used by 1 or more different patrons.");
    }

    /**
     * Test for verifying that an existing email is correctly found among Aleph patrons.
     *
     * <p>This test checks if a known existing email is correctly found among Aleph patrons whose patron ID contains
     * one of the prefixes defined in {@code AlephServiceTest.alephPatronPrefixes}.</p>
     *
     * <p>System properties required:</p>
     * <ul>
     *     <li><b>patronEmailInUse</b>: Email address that is expected to be used by at least one patron</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testIsEmailInUse_emailOnly_shouldReturnTrue_ifEmailFound \
     *             -DpatronEmailInUse=used@example.com
     * }</pre>
     */
    @Test
    void testIsEmailInUse_emailOnly_shouldReturnTrue_ifEmailFound() {
        // An existing patron email whose patron has an ID with one of the prefixes defined in `AlephServiceTest.alephPatronPrefixes`:
        String searchEmail = AlephServiceTest.patronEmailInUse;
        assertNotNull(searchEmail, "The system property 'patronEmailInUse' must be set.");    

        boolean isEmailInUse = alephService.isEmailInUse(searchEmail, null);

        assertTrue(isEmailInUse, "Expected email to be in use, but it was not.");
    }

    /**
     * Test for checking if an email is only used by a specific patron and not by others.
     *
     * <p>This test verifies that an email address is in use but exclusively by the patron specified by ID,
     * and that no other patrons share the same email address. The search is done among Aleph patrons whose
     * patron ID contains one of the prefixes defined in {@code AlephServiceTest.alephPatronPrefixes}.</p>
     *
     * <p>System properties required:</p>
     * <ul>
     *     <li><b>patronEmailUnique</b>: Email address used by a single patron</li>
     *     <li><b>patronWithUniqueEmail</b>: Patron ID of the patron using the above email</li>
     * </ul>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * ./mvnw test -Dtest=AlephServiceTest#testIsEmailInUse_excludePatronId_shouldReturnFalse_ifNoEmailFound \
     *             -DpatronEmailUnique=unique@example.com \
     *             -DpatronWithUniqueEmail=LIB00001
     * }</pre>
     */
    @Test
    void testIsEmailInUse_excludePatronId_shouldReturnFalse_ifNoEmailFound() {
        // An existing patron email whose patron has an ID with one of the prefixes defined in `AlephServiceTest.alephPatronPrefixes`:
        String searchEmail = AlephServiceTest.patronEmailUnique;
        assertNotNull(searchEmail, "The system property 'patronEmailUnique' must be set."); 

        // An ID of a patron who has an email defined in `searchEmail`:
        String excludePatronId = AlephServiceTest.patronWithUniqueEmail;
        assertNotNull(excludePatronId, "The system property 'patronWithUniqueEmail' must be set."); 

        boolean isEmailInUse = alephService.isEmailInUse(searchEmail, excludePatronId);

        assertFalse(isEmailInUse, "Expected email to be in used only by the patron " + excludePatronId + ", but it was used by 1 or more different patrons as well.");
    }
}
