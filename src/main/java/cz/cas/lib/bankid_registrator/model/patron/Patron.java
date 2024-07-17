package cz.cas.lib.bankid_registrator.model.patron;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronAction;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.entities.patron.PatronStatus;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import java.util.Optional;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patron")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Patron {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="is_new")
    public boolean isNew = true;         // Is this a new or existing Aleph patron?

    @Column(name="home_library")
    public String homeLibrary = "KNAV";         // z303-home-library

    @Column(name="patron_id")
    public String patronId;         // z303-id, z303.match-id

    @Column
    public String firstname;         // z303-first-name

    @Column
    public String lastname;         // z303-last-name

    @Column
    public String name;         // z303-name

    @Column
    public String email;         // z304-email-address

    @Column(name="birth_date")
    public String birthDate;         // z303-birth-date, format dd-mm-yyyy

    @Column(name="con_lng")
    @Enumerated(EnumType.STRING)
    public PatronLanguage conLng = PatronLanguage.CZE;         // z303-con-lng

    @Column(name="address_0")
    public String address0;         // z304-address-0 (for <z304-address-type>01</z304-address-type>)

    @Column(name="address_1")
    public String address1;         // z304-address-1 (for <z304-address-type>01</z304-address-type>)

    @Column(name="address_2")
    public String address2;         // z304-address-2 (for <z304-address-type>01</z304-address-type>)

    @Column
    public String zip;         // z304-zip

    @Column(name="contact_address_0")
    public String contactAddress0;         // z304-address-0 (for <z304-address-type>02</z304-address-type>)

    @Column(name="contact_address_1")
    public String contactAddress1;         // z304-address-1 (for <z304-address-type>02</z304-address-type>)

    @Column(name="contact_address_2")
    public String contactAddress2;         // z304-address-2 (for <z304-address-type>02</z304-address-type>)

    @Column(name="contact_zip")
    public String contactZip;         // z304-zip

    @Column(name="sms_number")
    public String smsNumber;         // z304-sms-number

    @Column
    public String status;         // z305-bor-status

    @Column
    public String barcode;         // z308-key-data

    @Column(name="id_card_name")
    public String idCardName;         // nazev obcanskeho prukazu - napr. "ID CZ"

    @Column(name="id_card_number")
    public String idCardNumber;         // cislo obcanskeho prukazu

    @Column(name="id_card_detail")
    public String idCardDetail;         // detail obcanskeho prukazu

    @Column
    public String verification;         // z308-verification

    @Column(name="bank_id_sub")
    public String bankIdSub;         // bankIdSub

    @Column
    @Enumerated(EnumType.STRING)
    public PatronAction action;          // record-action

    @Column(name="export_consent")
    @Enumerated(EnumType.STRING)
    public PatronBoolean exportConsent = PatronBoolean.N;         // z303-export-consent

    @Column(name="is_cas_employee")
    public boolean isCasEmployee = false;         // is CAS employee

    @Column
    public String rfid;

    @Column(name="expiry_date", nullable = true)
    public String expiryDate;      // z305-expiry-date, format dd/mm/yyyy

    public Long getSysId() {
        return id;
    }

    public String getId() {
        return patronId;
    }

    public void setId(String patronId) {
        this.patronId = patronId;
    }

    public String getPatronId() {
        return patronId;
    }
    
    public void setPatronId(String patronId) {
        this.patronId = patronId;
    }

    public boolean getIsCasEmployee() {
        return isCasEmployee;
    }

    public void setIsCasEmployee(boolean isCasEmployee) {
        this.isCasEmployee = isCasEmployee;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    /**
     * Update the patron with the given DTO data.
     * @param patron
     */
    public void update(PatronDTO patron) {
        Optional.ofNullable(patron.email).ifPresent(e -> this.email = e);
        Optional.ofNullable(patron.smsNumber).ifPresent(e -> this.smsNumber = e);
        Optional.ofNullable(patron.contactAddress0).ifPresent(e -> this.contactAddress0 = e);
        Optional.ofNullable(patron.contactAddress1).ifPresent(e -> this.contactAddress1 = e);
        Optional.ofNullable(patron.contactAddress2).ifPresent(e -> this.contactAddress2 = e);
        Optional.ofNullable(patron.contactZip).ifPresent(e -> this.contactZip = e);
        Optional.ofNullable(patron.conLng).ifPresent(e -> this.conLng = e);
        Optional.ofNullable(patron.exportConsent).ifPresent(e -> this.exportConsent = e);
        Optional.ofNullable(patron.rfid).ifPresent(e -> this.rfid = e);
        Optional.ofNullable(patron.isCasEmployee).ifPresent(e -> {
            this.isCasEmployee = e;

            PatronStatus patronStatus = e ? PatronStatus.STATUS_03 : PatronStatus.STATUS_16;
            this.status = patronStatus.getId();

            if (this.expiryDate != null && !this.expiryDate.isEmpty()) {
                this.expiryDate = DateUtils.addDaysToDateString(this.expiryDate, patronStatus.getMembershipLength(), "dd/MM/yyyy", "dd/MM/yyyy");
            } else {
                this.expiryDate = DateUtils.addDaysToToday(patronStatus.getMembershipLength(), "dd/MM/yyyy");
            }
        });
    }
}