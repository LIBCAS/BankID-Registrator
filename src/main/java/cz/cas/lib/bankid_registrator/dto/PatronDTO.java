package cz.cas.lib.bankid_registrator.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import javax.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatronDTO
{
    public String firstname;         // z303-first-name

    public String lastname;         // z303-last-name

    public String name;         // z303-name

    @Email(message = "{form.error.field.invalid}")
    public String email;         // z304-email-address

    public String birthDate;         // z303-birth-date

    public PatronLanguage conLng = PatronLanguage.CZE;         // z303-con-lng

    public String address0;         // z304-address-0 (for <z304-address-type>01</z304-address-type>)

    public String address1;         // z304-address-1 (for <z304-address-type>01</z304-address-type>)

    public String address2;         // z304-address-2 (for <z304-address-type>01</z304-address-type>)

    public String zip;         // z304-zip

    public boolean useContactAddress = false;

    @Size(max = 50, message = "{form.error.field.sizeExceeded}")
    public String contactAddress0;         // z304-address-0 (for <z304-address-type>02</z304-address-type>)

    @Size(max = 50, message = "{form.error.field.sizeExceeded}")
    public String contactAddress1;         // z304-address-1 (for <z304-address-type>02</z304-address-type>)

    @Size(max = 50, message = "{form.error.field.sizeExceeded}")
    public String contactAddress2;         // z304-address-2 (for <z304-address-type>02</z304-address-type>)

    @Pattern(regexp = "^$|^[0-9\\- ]+$", message = "{form.error.field.invalid}")
    @Size(max = 15, message = "{form.error.field.sizeExceeded}")
    public String contactZip;         // z304-zip

    @Pattern(regexp = "^$|^[+]?[0-9]{1,3}?[-.\\s]?[(]?[0-9]{1,4}?[)]?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}$", message = "{form.error.field.invalid}")
    @Size(max = 20, message = "{form.error.field.sizeExceeded}")
    public String smsNumber; // z304-sms-number

    public String barcode;         // z308-key-data

    public String idCardName;         // nazev obcanskeho prukazu - napr. "ID CZ"

    public String idCardNumber;         // cislo obcanskeho prukazu

    public String idCardDetail;         // detail obcanskeho prukazu

    public PatronBoolean exportConsent = PatronBoolean.N;         // z303-export-consent

    @AssertTrue(message = "{form.error.field.required}")
    public boolean declaration1 = false;    // Declaration of library rules acceptance

    @AssertTrue(message = "{form.error.field.required}")
    public boolean declaration2 = false;    // Declaration of truthfulness of data

    @AssertTrue(message = "{form.error.field.required}")
    public boolean declaration3 = false;    // Declaration of data processing consent

    public boolean isCasEmployee = false;         // is CAS employee

    @Size(max = 20, message = "{form.error.field.sizeExceeded}")
    public String rfid;

    public String expiryDate;      // z305-expiry-date

    public boolean getIsCasEmployee() {
        return isCasEmployee;
    }

    public void setIsCasEmployee(boolean isCasEmployee) {
        this.isCasEmployee = isCasEmployee;
    }

    public void setDeclaration1(boolean declaration1) {
        this.declaration1 = declaration1;
        updateExportConsent();
    }

    public void setDeclaration2(boolean declaration2) {
        this.declaration2 = declaration2;
        updateExportConsent();
    }

    public void setDeclaration3(boolean declaration3) {
        this.declaration3 = declaration3;
        updateExportConsent();
    }

    private void updateExportConsent() {
        this.exportConsent = (this.declaration1 && this.declaration2 && this.declaration3) ? PatronBoolean.Y : PatronBoolean.N;
    }

    public void restoreDefaults(PatronDTO defaultPatron) {
        this.setFirstname(defaultPatron.getFirstname());
        this.setLastname(defaultPatron.getLastname());
        this.setName(defaultPatron.getName());
        this.setBirthDate(defaultPatron.getBirthDate());
        this.setAddress0(defaultPatron.getAddress0());
        this.setAddress1(defaultPatron.getAddress1());
        this.setAddress2(defaultPatron.getAddress2());
        this.setZip(defaultPatron.getZip());
        this.setContactAddress0(defaultPatron.getContactAddress0());
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}