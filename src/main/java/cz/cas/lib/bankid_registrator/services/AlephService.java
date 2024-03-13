/*
 * Copyright (C) 2022 Academy of Sciences Library
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.mariadb.MariaDBRepository;
import cz.cas.lib.bankid_registrator.dao.oracle.OracleRepository;
import cz.cas.lib.bankid_registrator.dto.PatronAction;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.dto.PatronLanguage;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.AddressType;
import cz.cas.lib.bankid_registrator.entities.entity.IDCard;
import cz.cas.lib.bankid_registrator.entities.entity.IDCardType;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.util.TimestampToDate;

import java.time.LocalDate;

import java.io.StringWriter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author iok
 */
@Service
public class AlephService extends AlephServiceAbstract {

    private final MainConfiguration mainConfig;
    private final AlephServiceConfig alephServiceConfig;
    private final MariaDBRepository mariaDbRepository;
    private final OracleRepository oracleRepository;

    public AlephService(MainConfiguration mainConfig, AlephServiceConfig alephServiceConfig, MariaDBRepository mariaDbRepository, OracleRepository oracleRepository)
    {
        this.mainConfig = mainConfig;
        this.alephServiceConfig = alephServiceConfig;
        this.mariaDbRepository = mariaDbRepository;
        this.oracleRepository = oracleRepository;
    }

    public Map<String, Object> createPatron(PatronDTO patron)
    {
        Map<String, Object> result = new HashMap<>();

        Map<String, String> createPatronXml = this.createPatronXML(patron);

        if (createPatronXml.containsKey("error")) {
            result.put("error", createPatronXml.get("error"));
            return result;
        }

        String newPatronXml = createPatronXml.get("xml");
        result.put("xml", newPatronXml);

        boolean newPatronIsCreated = this.updateBorX(newPatronXml);
        result.put("success", newPatronIsCreated);

        return result;
    }

    /**
     * Sends a request to Aleph X-Services to create/update/delete a patron.
     * @param xml - the XML string to be sent to Aleph
     * @return - true if the request was successful, false otherwise
     */
    private boolean updateBorX(String xml) {
        String url = this.alephServiceConfig.getHost() + ":" + this.alephServiceConfig.getPort() + "/X";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("op", "update-bor");
        body.add("user_name", this.alephServiceConfig.getWwwuser());
        body.add("user_password", this.alephServiceConfig.getWwwpasswd());
        body.add("library", "KNA50");
        body.add("update_flag", "Y");
        body.add("xml_full_req", xml);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        getLogger().info("updateBorX's response: {}", response);

        return response.getStatusCode().is2xxSuccessful();
    }

    /**
     * Generates an XML string for a new Aleph patron
     * @param patron - the patron to be created
     * @return 
     */
    public Map<String, String> createPatronXML(PatronDTO patron) {

        Map<String, String> result = new HashMap<>();

        Assert.notNull(patron, "\"patron\" is required");

        String now_yyyyMMdd = TimestampToDate.getTimestampToDate("yyyyMMdd");
        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/CreatePatron.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);
            String patronId = patron.getId();

            // z303
            transformer.setParameter("z303-match-id", patronId);
            transformer.setParameter("z303-id", patronId);
            transformer.setParameter("z303-name-key", patron.getName() + " " + patronId);
            transformer.setParameter("z303-last-name", patron.getLastname());
            transformer.setParameter("z303-first-name", patron.getFirstname());
            transformer.setParameter("z303-name", patron.getName());
            transformer.setParameter("z303-con-lng", patron.getConLng().toString());
            transformer.setParameter("z303-birth-date", patron.getBirthDate());

            // z304
            transformer.setParameter("z304-seq01-id", patronId);
            transformer.setParameter("z304-seq01-address-0", patron.getAddress0());
            transformer.setParameter("z304-seq01-address-1", patron.getAddress1());
            transformer.setParameter("z304-seq01-address-2", patron.getAddress2());
            transformer.setParameter("z304-seq01-zip", patron.getZip());
            transformer.setParameter("z304-seq01-email-address", patron.getEmail());
            transformer.setParameter("z304-seq01-telephone-2", patron.getIdCardName());
            transformer.setParameter("z304-seq01-telephone-3", patron.getIdCardDetail());
            transformer.setParameter("z304-seq01-telephone-4", patron.getIdCardNumber());
            transformer.setParameter("z304-seq01-date-from", now_yyyyMMdd);

            // z305 / KNA50, KNAV, KNAVD, KNAVP
            transformer.setParameter("z305-kna50-id", patronId);
            transformer.setParameter("z305-kna50-registration-date", now_yyyyMMdd);

            transformer.setParameter("z305-knav-id", patronId);
            transformer.setParameter("z305-knav-registration-date", now_yyyyMMdd);

            transformer.setParameter("z305-knavd-id", patronId);
            transformer.setParameter("z305-knavd-registration-date", now_yyyyMMdd);

            transformer.setParameter("z305-knavp-id", patronId);
            transformer.setParameter("z305-knavp-registration-date", now_yyyyMMdd);

            // z308 / id
            transformer.setParameter("z308-key-type-00-id", patronId);
            transformer.setParameter("z308-key-type-00-key-data", patronId);

            // z308 / barcode
            transformer.setParameter("z308-key-type-01-id", patronId);
            transformer.setParameter("z308-key-type-01-key-data", patron.getBarcode());

            // z308 / bankid
            transformer.setParameter("z308-key-type-07-id", patronId);
            transformer.setParameter("z308-key-type-07-key-data", patron.getBankIdSub());

            StringWriter stringWriter = new StringWriter();
            Result streamResult = new StreamResult(stringWriter);
            transformer.transform(stylesheetSource, streamResult);
            if (this.mainConfig.getRewrite_aleph_batch_xml_header()) {
                xmlString = stringWriter.getBuffer().toString().replaceFirst("\\<\\?(xml){1}\\s{1,}.*\\?\\>", xmlString);
            } else {
                xmlString = stringWriter.getBuffer().toString();
            }
            result.put("xml", xmlString);
        } catch (TransformerException ex) {
            result.put("error", ex.getMessage());
            getLogger().error(MainService.class.getName(), ex);
        }

        return result;
    }

    /**
     * Initializes patron's data based on the Connect and Identify objects
     * @param userInfo
     * @param userProfile
     * @return  Map<String, Object>
     */
    public Map<String, Object> newPatron(Connect userInfo, Identify userProfile) {
        Assert.notNull(userInfo, "\"userInfo\" is required");
        Assert.notNull(userProfile, "\"userProfile\" is required");

        Map<String, Object> result = new HashMap<>();

        PatronDTO patron = new PatronDTO();

        patron.setId(generatePatronId());
        patron.setBarcode(generatePatronBarcode());

        String fname = userInfo.getGiven_name();      // Joanne
        String mname = Optional.ofNullable(userInfo.getMiddle_name()).orElse("");     // Kathleen
        String lname = userInfo.getFamily_name();     // Rowling

        patron.setFirstname(fname);
        patron.setLastname(Stream.of(mname, lname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Kathleen Rowling
        patron.setName(Stream.of(fname, mname, lname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Joanne Kathleen Rowling

        patron.setEmail(Optional.ofNullable(userInfo.getEmail()).orElse(""));

        String birthdate = userInfo.getBirthdate();   // 2003-07-25
        if (birthdate == null) {
            result.put("error", "chybí datum narození");
            return result;
        }
        patron.setBirthDate(birthdate.replace("-", ""));  // 2003-07-25 => 20030725

        patron.setConLng(userInfo.getLocale() == "cs_CZ" ? PatronLanguage.CZE : PatronLanguage.ENG);

        // Patron address (permanent residence)
        Address address = userProfile.getAddresses().stream()
            .filter(a -> a.getType() == AddressType.PERMANENT_RESIDENCE)
            .findFirst()
            .orElse(null);
        if (address != null) {
            String addressStreet = Optional.ofNullable(address.getStreet()).orElse("");
            String addressNumber = Optional.ofNullable(address.getEvidencenumber()).orElse("");
            String addressCity = address.getCity();
            if (addressCity == null) {
                result.put("error", "chybí město");
                return result;
            }
            String addressCityarea = Optional.ofNullable(address.getCityarea()).orElse("");
            String addressZip = address.getZipcode();
            if (addressZip == null) {
                result.put("error", "chybí PSČ");
                return result;
            }

            patron.setAddress0(patron.getName());
            patron.setAddress1(Stream.of(!addressStreet.equals("") ? addressStreet : addressCityarea, addressNumber)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" ")));
            
            if (!addressCityarea.equals("") && addressCityarea.equals(addressCity)) {
                patron.setAddress2(addressCity);
            } else if (!addressCityarea.equals("") && !addressCityarea.equals(addressCity)) {
                patron.setAddress2(addressCity + '-' + addressCityarea);
            } else {
                patron.setAddress2(addressCity);
            }
            patron.setZip(addressZip);
        } else {
            result.put("error", "chybí adresa trvalého bydliště");
            return result;
        }

        // Patron ID card
        IDCard idCard = userProfile.getIdcards().stream()
            .filter(i -> i.getType() == IDCardType.ID)
            .findFirst()
            .orElse(null);
        if (idCard != null) {
            String idCardName = idCard.getType().toString().concat(" ").concat(idCard.getCountry());
            patron.setIdCardName(idCardName);
            patron.setIdCardNumber(idCard.getNumber());
            patron.setIdCardDetail("Občanský průkaz");
        } else {
            result.put("error", "chybí občanský průkaz");
            return result;
        }

        patron.setVerification(generatePatronPassword());

        // New registration or registration renewal
        Boolean isNewAlephPatron = this.isNewAlephPatron(patron);
        patron.setIsNew(isNewAlephPatron);
        patron.setAction(isNewAlephPatron ? PatronAction.I : PatronAction.A);

        // BankID
        patron.setBankIdSub(userInfo.getSub());

        result.put("patron", patron);

        return result;
    }

    /**
     * Initializes patron's testing data based on the Connect and Identify objects
     * @param userInfo
     * @param userProfile
     * @return  Map<String, Object>
     */
    public Map<String, Object> newPatronTest(Connect userInfo, Identify userProfile) {
        Assert.notNull(userInfo, "\"userInfo\" is required");
        Assert.notNull(userProfile, "\"userProfile\" is required");

        Map<String, Object> result = new HashMap<>();

        PatronDTO patron = new PatronDTO();

        patron.setId(generatePatronId());
        patron.setBarcode(generatePatronBarcode());

        String fname = userInfo.getGiven_name();      // Joanne
        String mname = this.generateTestingMname();   // Kathleen
        String lname = userInfo.getFamily_name();     // Rowling
        patron.setFirstname(fname);
        patron.setLastname(Stream.of(mname, lname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Kathleen Rowling
        patron.setName(Stream.of(fname, mname, lname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Joanne Kathleen Rowling

        patron.setEmail(Optional.ofNullable(this.generateTestingEmail()).orElse(""));

        String birthdate = this.generateTestingBirthday();   // 2003-07-25
        if (birthdate == null) {
            result.put("error", "chybí datum narození");
            return result;
        }
        patron.setBirthDate(birthdate.replace("-", ""));  // 2003-07-25 => 20030725

        String smsNumber = userProfile.getPhone_number(); logger.info("smsNumber: {}", smsNumber);
        patron.setSmsNumber(Optional.ofNullable(smsNumber).orElse(""));

        String conLng = userInfo.getLocale(); logger.info("conLng: {}", conLng);
        patron.setConLng(conLng.equals("cs_CZ") ? PatronLanguage.CZE : PatronLanguage.ENG); logger.info("patron.getConLng() == {}", patron.getConLng());
        if (patron.getConLng().equals(PatronLanguage.CZE)) {
            logger.info("patron.getConLng() == PatronLanguage.CZE");
        } else if (patron.getConLng().equals(PatronLanguage.ENG)) {
            logger.info("patron.getConLng() == PatronLanguage.ENG");
        } else {
            logger.info("patron.getConLng() == null");
        }

        // Patron address (permanent residence)
        Address address = userProfile.getAddresses().stream()
            .filter(a -> a.getType() == AddressType.PERMANENT_RESIDENCE)
            .findFirst()
            .orElse(null);
        if (address != null) {
            String addressStreet = Optional.ofNullable(address.getStreet()).orElse("");
            String addressNumber = Optional.ofNullable(address.getEvidencenumber()).orElse("");
            String addressCity = address.getCity();
            if (addressCity == null) {
                result.put("error", "chybí město");
                return result;
            }
            String addressCityarea = Optional.ofNullable(address.getCityarea()).orElse("");
            String addressZip = address.getZipcode();
            if (addressZip == null) {
                result.put("error", "chybí PSČ");
                return result;
            }

            patron.setAddress0(patron.getName());
            patron.setAddress1(Stream.of(!addressStreet.equals("") ? addressStreet : addressCityarea, addressNumber)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" ")));
            
            if (!addressCityarea.equals("") && addressCityarea.equals(addressCity)) {
                patron.setAddress2(addressCity);
            } else if (!addressCityarea.equals("") && !addressCityarea.equals(addressCity)) {
                patron.setAddress2(addressCity + '-' + addressCityarea);
            } else {
                patron.setAddress2(addressCity);
            }
            patron.setZip(addressZip);
        } else {
            result.put("error", "chybí adresa trvalého bydliště");
            return result;
        }

        // Patron ID card
        IDCard idCard = userProfile.getIdcards().stream()
            .filter(i -> i.getType() == IDCardType.ID)
            .findFirst()
            .orElse(null);
        if (idCard != null) {
            String idCardName = idCard.getType().toString().concat(" ").concat(idCard.getCountry());
            patron.setIdCardName(idCardName);
            patron.setIdCardNumber(idCard.getNumber());
            patron.setIdCardDetail("Občanský průkaz");
        } else {
            result.put("error", "chybí občanský průkaz");
            return result;
        }

        patron.setVerification(generatePatronPassword());

        // New registration or registration renewal
        Boolean isNewAlephPatron = this.isNewAlephPatron(patron);logger.info("isNewAlephPatron: {}", isNewAlephPatron);
        patron.setIsNew(isNewAlephPatron);
        patron.setAction(isNewAlephPatron ? PatronAction.I : PatronAction.A); logger.info("patron.getAction(): {}", patron.getAction());

        // BankID
        patron.setBankIdSub(userInfo.getSub());

        result.put("patron", patron);

        return result;
    }

    /**
     * Generates patron's id, at least 9 characters long
     */
    private String generatePatronId() {
        String prefix = this.mainConfig.getId_prefix();
        Long maxVal = mariaDbRepository.findMaxId();
        Long newVal = 1L;

        if (maxVal != null) {
            newVal = maxVal + 1;
        }

        return String.format("%s%05d", prefix, newVal);
    }

    /**
     * Generates patron's barcode, at least 10 characters long
     */
    private String generatePatronBarcode() {
        String prefix = mainConfig.getBarcode_prefix();
        Long maxVal = mariaDbRepository.findMaxBarcode();
        Long newVal = 1L;

        if (maxVal != null) {
            newVal = maxVal + 1;
        }

        return String.format("%s%04d", prefix, newVal);
    }

    /**
     * Generates patron's password, 8 characters long
     */
    private String generatePatronPassword() {
        int passwordLength = 8;
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < passwordLength; i++) {
            int randomIndex = ThreadLocalRandom.current().nextInt(chars.length());
            result.append(chars.charAt(randomIndex));
        }
        return result.toString();
    }

    /**
     * Checks if patron already exists in the Aleph Oracle DB
     * @param name Patron's name
     * @param birthDate Patron's birthdate
     * @return true if patron exists, false otherwise
     */
    public boolean isNewAlephPatron(PatronDTO patron) {
        return (oracleRepository.getPatronRowsCount(patron.getName(), patron.getBirthDate()) == 0);
    }

    public int testika(String name, String birthDate) {
        getLogger().info("isNewAlephPatron params: {} {}", name, birthDate);
        int result = oracleRepository.getPatronRowsCount(name, birthDate);
        getLogger().info("isNewAlephPatron: {}", result);
        return result;
    }

    private String generateTestingMname() {
        return "Testovací";
    }

    private String generateTestingBirthday() {
        Random random = new Random();
        int minDay = (int) LocalDate.of(1950, 1, 1).toEpochDay();
        int maxDay = (int) LocalDate.of(2010, 12, 31).toEpochDay();
        long randomDay = minDay + random.nextInt(maxDay - minDay);
        return LocalDate.ofEpochDay(randomDay).toString();
    }

    private String generateTestingEmail() {
        Random random = new Random();
        int randomNum = 1000 + random.nextInt(9000);
        return "test" + randomNum + "@testing.com";
    }

    /**
     * Checks if we can proceed with the action associated with the patron
     * @return
     */
    public boolean isActionReady() {
        return true;
    }
}
