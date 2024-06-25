package cz.cas.lib.bankid_registrator.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
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

    public String email;         // z304-email-address

    public String birthDate;         // z303-birth-date

    public PatronLanguage conLng = PatronLanguage.CZE;         // z303-con-lng

    public String address0;         // z304-address-0 (for <z304-address-type>01</z304-address-type>)

    public String address1;         // z304-address-1 (for <z304-address-type>01</z304-address-type>)

    public String address2;         // z304-address-2 (for <z304-address-type>01</z304-address-type>)

    public String zip;         // z304-zip

    public String contactAddress0;         // z304-address-0 (for <z304-address-type>02</z304-address-type>)

    public String contactAddress1;         // z304-address-1 (for <z304-address-type>02</z304-address-type>)

    public String contactAddress2;         // z304-address-2 (for <z304-address-type>02</z304-address-type>)

    public String contactZip;         // z304-zip

    public String smsNumber;         // z304-sms-number

    public String barcode;         // z308-key-data

    public String idCardName;         // nazev obcanskeho prukazu - napr. "ID CZ"

    public String idCardNumber;         // cislo obcanskeho prukazu

    public String idCardDetail;         // detail obcanskeho prukazu

    public PatronBoolean exportConsent = PatronBoolean.N;         // z303-export-consent

    public boolean isCasEmployee = Boolean.FALSE;         // is CAS employee

    public String rfid;

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public boolean getIsCasEmployee() {
        return isCasEmployee;
    }
}