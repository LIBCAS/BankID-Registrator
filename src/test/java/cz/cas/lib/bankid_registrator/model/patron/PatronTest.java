package cz.cas.lib.bankid_registrator.model.patron;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.entities.patron.PatronStatus;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Patron model.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * ./mvnw -Dtest=PatronTest test
 * }</pre>
 */
class PatronTest
{
    private Patron patron;

    @BeforeEach
    void setUp() {
        patron = new Patron();
        patron.setEmail("original@example.com");
        patron.setSmsNumber("123456789");
        patron.setContactAddress0("Old Address Line 0");
        patron.setContactAddress1("Old Address Line 1");
        patron.setContactAddress2("Old Address Line 2");
        patron.setContactZip("11111");
        patron.setConLng(PatronLanguage.CZE);
        patron.setExportConsent(PatronBoolean.N);
        patron.setRfid("OldRFID");
        patron.setIsCasEmployee(false);
        patron.setExpiryDate("01/01/2030");  // Setting a future expiry date initially
    }

    @Test
    void testUpdateWithNonNullFields() {
        PatronDTO patronDTO = new PatronDTO();
        patronDTO.email = "updated@example.com";
        patronDTO.smsNumber = "987654321";
        patronDTO.contactAddress0 = "New Address Line 0";
        patronDTO.contactAddress1 = "New Address Line 1";
        patronDTO.contactAddress2 = "New Address Line 2";
        patronDTO.contactZip = "22222";
        patronDTO.conLng = PatronLanguage.ENG;
        patronDTO.exportConsent = PatronBoolean.Y;
        patronDTO.rfid = "NewRFID";
        patronDTO.isCasEmployee = true;
        patronDTO.useContactAddress = true;

        patron.update(patronDTO);

        assertEquals("updated@example.com", patron.getEmail());
        assertEquals("987654321", patron.getSmsNumber());
        assertEquals("New Address Line 0", patron.getContactAddress0());
        assertEquals("New Address Line 1", patron.getContactAddress1());
        assertEquals("New Address Line 2", patron.getContactAddress2());
        assertEquals("22222", patron.getContactZip());
        assertEquals(PatronLanguage.ENG, patron.getConLng());
        assertEquals(PatronBoolean.Y, patron.getExportConsent());
        assertEquals("NewRFID", patron.getRfid());
        assertTrue(patron.getIsCasEmployee());
        assertEquals(PatronStatus.STATUS_03.getId(), patron.getStatus());
    }

    @Test
    void testUpdateExpiryDateWithFutureDateAsCasEmployee() {
        PatronDTO patronDTO = new PatronDTO();
        patronDTO.isCasEmployee = true;

        // Mock current expiry date as a future date
        String expiryDate = DateUtils.addDaysToToday(10, "dd/MM/yyyy");
        patron.setExpiryDate(expiryDate);
        assertFalse(DateUtils.isDateExpired(patron.getExpiryDate(), "dd/MM/yyyy"));

        patron.update(patronDTO);

        assertEquals(PatronStatus.STATUS_03.getId(), patron.getStatus());
        String newExpiryDate = DateUtils.addDaysToSpecificDate(expiryDate, PatronStatus.STATUS_03.getMembershipLength(), "dd/MM/yyyy", "dd/MM/yyyy");
        assertEquals(patron.getExpiryDate(), newExpiryDate);
    }

    @Test
    void testUpdateExpiryDateWithExpiredDateAsNonCasEmployee() {
        PatronDTO patronDTO = new PatronDTO();
        patronDTO.isCasEmployee = false;

        // Mock current expiry date as an expired date
        String expiryDate = DateUtils.addDaysToToday(-10, "dd/MM/yyyy");
        patron.setExpiryDate(expiryDate);
        assertTrue(DateUtils.isDateExpired(patron.getExpiryDate(), "dd/MM/yyyy"));

        patron.update(patronDTO);

        assertEquals(PatronStatus.STATUS_16.getId(), patron.getStatus());
        String newExpiryDate = DateUtils.addDaysToToday(PatronStatus.STATUS_16.getMembershipLength(), "dd/MM/yyyy");
        assertEquals(patron.getExpiryDate(), newExpiryDate);
    }

    @Test
    void testUpdateExpiryDateWithTodayExpiryDateAsCasEmployee() {
        PatronDTO patronDTO = new PatronDTO();
        patronDTO.isCasEmployee = true;

        // Mock current expiry date as today's date
        String expiryDate = DateUtils.addDaysToToday(0, "dd/MM/yyyy");
        patron.setExpiryDate(expiryDate);
        assertFalse(DateUtils.isDateExpired(patron.getExpiryDate(), "dd/MM/yyyy"));

        patron.update(patronDTO);

        assertEquals(PatronStatus.STATUS_03.getId(), patron.getStatus());
        String newExpiryDate = DateUtils.addDaysToSpecificDate(expiryDate, PatronStatus.STATUS_03.getMembershipLength(), "dd/MM/yyyy", "dd/MM/yyyy");
        assertEquals(patron.getExpiryDate(), newExpiryDate);
    }

    @Test
    void testUpdateUseContactAddressFalse() {
        PatronDTO patronDTO = new PatronDTO();
        patronDTO.useContactAddress = false;

        patron.update(patronDTO);

        assertEquals("", patron.getContactAddress1());
        assertEquals("", patron.getContactAddress2());
        assertEquals("", patron.getContactZip());
    }

    @Test
    void testUpdateUseContactAddressTrue() {
        PatronDTO patronDTO = new PatronDTO();
        patronDTO.useContactAddress = true;
        patronDTO.contactAddress1 = "New Contact Address 1";
        patronDTO.contactAddress2 = "New Contact Address 2";
        patronDTO.contactZip = "33333";

        patron.update(patronDTO);

        assertEquals("New Contact Address 1", patron.getContactAddress1());
        assertEquals("New Contact Address 2", patron.getContactAddress2());
        assertEquals("33333", patron.getContactZip());
    }

    @Test
    void testToJson() throws JsonProcessingException {
        String json = patron.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"email\":\"original@example.com\""));
        assertTrue(json.contains("\"smsNumber\":\"123456789\""));
    }
}