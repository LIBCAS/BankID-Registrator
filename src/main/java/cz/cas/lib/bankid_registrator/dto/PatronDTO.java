package cz.cas.lib.bankid_registrator.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PatronDTO {
    public boolean isNew;                       // Is this a new or existing Aleph patron?
    public String homeLibrary;                  // z303-home-library
    public String id;                           // z303-id, z303.match-id
    public String firstname;                    // z303-first-name
    public String lastname;                     // z303-last-name
    public String name;                         // z303-name
    public String email;                        // z304-email-address
    public String birthDate;                    // z303-birth-date
    public PatronLanguage conLng;               // z303-con-lng
    public String address0;                     // z304-address-0 (for <z304-address-type>01</z304-address-type>)
    public String address1;                     // z304-address-1 (for <z304-address-type>01</z304-address-type>)
    public String address2;                     // z304-address-2 (for <z304-address-type>01</z304-address-type>)
    public String zip;                          // z304-zip
    public String contactAddress0;              // z304-address-0 (for <z304-address-type>02</z304-address-type>)
    public String contactAddress1;              // z304-address-1 (for <z304-address-type>02</z304-address-type>)
    public String contactAddress2;              // z304-address-2 (for <z304-address-type>02</z304-address-type>)
    public String contactZip;                   // z304-zip
    public String smsNumber;                    // z304-sms-number
    public String status;                       // z305-bor-status
    public String barcode;                      // z308-key-data
    public String idCardName;                   // nazev obcanskeho prukazu - napr. "ID CZ"
    public String idCardNumber;                 // cislo obcanskeho prukazu
    public String idCardDetail;                 // detail obcanskeho prukazu
    public String verification;                 // z308-verification
    public String bankIdSub;                    // bankIdSub
    public PatronAction action;                 // record-action

    public boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public PatronLanguage getConLng() {
        return conLng;
    }

    public void setConLng(PatronLanguage conLng) {
        this.conLng = conLng;
    }

    public String getAddress0() {
        return address0;
    }

    public void setAddress0(String address0) {
        this.address0 = address0;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PatronAction getAction() {
        return action;
    }

    public void setAction(PatronAction action) {
        this.action = action;
    }

    public String getContactAddress0() {
        return contactAddress0;
    }

    public void setContactAddress0(String contactAddress0) {
        this.contactAddress0 = contactAddress0;
    }

    public String getContactAddress1() {
        return contactAddress1;
    }

    public void setContactAddress1(String contactAddress1) {
        this.contactAddress1 = contactAddress1;
    }

    public String getContactAddress2() {
        return contactAddress2;
    }

    public void setContactAddress2(String contactAddress2) {
        this.contactAddress2 = contactAddress2;
    }

    public String getContactZip() {
        return contactZip;
    }

    public void setContactZip(String contactZip) {
        this.contactZip = contactZip;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public String getVerification() {
        return verification;
    }

    public void setVerification(String verification) {
        this.verification = verification;
    }

    public String getIdCardName() {
        return idCardName;
    }

    public void setIdCardName(String idCardName) {
        this.idCardName = idCardName;
    }

    public String getIdCardNumber() {
        return idCardNumber;
    }

    public void setIdCardNumber(String idCardNumber) {
        this.idCardNumber = idCardNumber;
    }

    public String getIdCardDetail() {
        return idCardDetail;
    }

    public void setIdCardDetail(String idCardDetail) {
        this.idCardDetail = idCardDetail;
    }

    public String getHomeLibrary() {
        return homeLibrary;
    }

    public void setHomeLibrary(String homeLibrary) {
        this.homeLibrary = homeLibrary;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBankIdSub() {
        return bankIdSub;
    }

    public void setBankIdSub(String bankIdSub) {
        this.bankIdSub = bankIdSub;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}