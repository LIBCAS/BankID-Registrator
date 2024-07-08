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

    /**
     * Merges two patrons into one. If the fields are the same, the value is taken from the first patron.
     * @param patron1 - patron with data provided by the BankID
     * @param patron2 - patron with data provided by the user during the initial registration
     * @return
     */
    public static PatronDTO mergePatrons(PatronDTO patron1, PatronDTO patron2) {
        PatronDTO patronFinal = new PatronDTO();

        patronFinal.setFirstname(patron1.getFirstname());
        patronFinal.setLastname(patron1.getLastname());
        patronFinal.setName(patron1.getName());
        patronFinal.setEmail(mergeField(patron1.getEmail(), patron2.getEmail()));
        patronFinal.setBirthDate(mergeField(patron1.getBirthDate(), patron2.getBirthDate()));
        patronFinal.setConLng(patron2.getConLng());
        patronFinal.setAddress0(patron1.getAddress0());
        patronFinal.setAddress1(patron1.getAddress1());
        patronFinal.setAddress2(patron1.getAddress2());
        patronFinal.setZip(patron1.getZip());
        patronFinal.setContactAddress0(mergeField(patron1.getContactAddress0(), patron2.getContactAddress0()));
        patronFinal.setContactAddress1(mergeField(patron1.getContactAddress1(), patron2.getContactAddress1()));
        patronFinal.setContactAddress2(mergeField(patron1.getContactAddress2(), patron2.getContactAddress2()));
        patronFinal.setContactZip(mergeField(patron1.getContactZip(), patron2.getContactZip()));
        patronFinal.setSmsNumber(mergeField(patron1.getSmsNumber(), patron2.getSmsNumber()));
        patronFinal.setBarcode(mergeField(patron1.getBarcode(), patron2.getBarcode()));
        patronFinal.setIdCardName(mergeField(patron1.getIdCardName(), patron2.getIdCardName()));
        patronFinal.setIdCardNumber(mergeField(patron1.getIdCardNumber(), patron2.getIdCardNumber()));
        patronFinal.setIdCardDetail(mergeField(patron1.getIdCardDetail(), patron2.getIdCardDetail()));
        patronFinal.setExportConsent(PatronBoolean.N);
        patronFinal.isCasEmployee = Boolean.FALSE;
        patronFinal.setRfid(mergeField(patron1.getRfid(), patron2.getRfid()));

        return patronFinal;
    }

    private static String mergeField(String field1, String field2) {
        if (field1 == null || field1.isEmpty()) {
            return field2;
        } else if (field2 == null || field2.isEmpty()) {
            return field1;
        } else if (field1.equals(field2)) {
            return field1;
        } else {
            return null;
        }
    }
}