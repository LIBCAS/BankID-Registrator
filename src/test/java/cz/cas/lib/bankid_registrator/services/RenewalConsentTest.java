package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.validators.PatronDTOValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.validation.BeanPropertyBindingResult;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RenewalConsentTest {
    private Patron patron() {
        Patron patron = new Patron();
        patron.setId("TEST123");
        patron.setFirstname("Test");
        patron.setLastname("Patron");
        patron.setName("Patron Test");
        patron.setBirthDate("19800101");
        patron.setAddress0("Patron Test");
        patron.setAddress1("Test street");
        patron.setAddress2("Prague");
        patron.setZip("11000");
        patron.setEmail("test@example.com");
        patron.setSmsNumber("");
        patron.setBarcode("TEST123");
        patron.setVerification("test-password");
        patron.setStatus("03");
        return patron;
    }

    private PatronDTO acceptedDeclarations(boolean employee) {
        PatronDTO dto = new PatronDTO();
        dto.setIsCasEmployee(employee);
        dto.setDeclaration1(true);
        dto.setDeclaration2(true);
        dto.setDeclaration3(true);
        dto.setDeclaration4(!employee);
        return dto;
    }

    @ParameterizedTest
    @EnumSource(PatronBoolean.class)
    void renewalPreservesOriginalConsentInPatronAndXml(PatronBoolean originalConsent) {
        Patron original = patron();
        original.setId("TEST123");
        original.setExportConsent(originalConsent);
        Patron bankId = patron();
        Patron renewed = PatronService.mergePatrons(bankId, original);
        assertEquals(originalConsent, renewed.getExportConsent());
        PatronDTO dto = acceptedDeclarations(false);
        // Submitted consent cannot override the original value in either direction.
        dto.setExportConsent(originalConsent == PatronBoolean.N ? PatronBoolean.Y : PatronBoolean.N);
        renewed.updateForRenewal(dto, original);
        assertEquals(originalConsent, renewed.getExportConsent());
        AlephService service = new AlephService(mock(MainConfiguration.class), null, null, null,
                new DefaultResourceLoader());
        String xml = service.updatePatronXML(renewed, original).get("xml");
        assertNotNull(xml);
        assertTrue(xml.contains("<z303-export-consent>" + originalConsent + "</z303-export-consent>"));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void renewalAcceptsRequiredDeclarationsRegardlessOfExportConsent(boolean employee) {
        PatronDTO dto = acceptedDeclarations(employee);
        dto.setExportConsent(PatronBoolean.N);
        if (employee)
            dto.setEmail("employee@example.com");
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(dto, "patron");
        // Empty RFID avoids external lookups; employee email lookup uses a mock.
        PatronDTOValidator validator = new PatronDTOValidator();
        org.springframework.test.util.ReflectionTestUtils.setField(validator, "alephService", mock(AlephService.class));
        validator.validateRenewal(dto, errors, "TEST123", null);
        assertFalse(errors.hasErrors(), errors.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3, 4 })
    void renewalRejectsEachMissingDeclarationEvenWithExportConsentY(int missing) {
        PatronDTO dto = acceptedDeclarations(false);
        if (missing == 1)
            dto.setDeclaration1(false);
        if (missing == 2)
            dto.setDeclaration2(false);
        if (missing == 3)
            dto.setDeclaration3(false);
        if (missing == 4)
            dto.setDeclaration4(false);
        dto.setExportConsent(PatronBoolean.Y);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(dto, "patron");
        new PatronDTOValidator().validateRenewal(dto, errors, "TEST123", null);
        assertTrue(errors.hasFieldErrors("declaration" + missing));
    }

    @Test
    void registrationStillRequiresAndSendsConsentY() {
        PatronDTO dto = acceptedDeclarations(false);
        assertEquals(PatronBoolean.Y, dto.getExportConsent());
        Patron patron = patron();
        patron.update(dto);
        AlephService service = new AlephService(mock(MainConfiguration.class), null, null, null,
                new DefaultResourceLoader());
        assertTrue(service.createPatronXML(patron).get("xml").contains("<z303-export-consent>Y</z303-export-consent>"));
        dto.setExportConsent(PatronBoolean.N);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(dto, "patron");
        new PatronDTOValidator().validate(dto, errors, null, null);
        assertTrue(errors.hasFieldErrors("exportConsent"));
    }
}
