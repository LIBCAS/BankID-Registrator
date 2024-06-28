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
import cz.cas.lib.bankid_registrator.dao.mariadb.PatronBarcodeRepository;
import cz.cas.lib.bankid_registrator.dao.oracle.OracleRepository;
import cz.cas.lib.bankid_registrator.entities.entity.Address;
import cz.cas.lib.bankid_registrator.entities.entity.AddressType;
import cz.cas.lib.bankid_registrator.entities.entity.IDCard;
import cz.cas.lib.bankid_registrator.entities.entity.IDCardType;
import cz.cas.lib.bankid_registrator.entities.patron.PatronAction;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBorXOp;
import cz.cas.lib.bankid_registrator.entities.patron.PatronHold;
import cz.cas.lib.bankid_registrator.entities.patron.PatronItem;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.entities.patron.PatronStatus;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.util.TimestampToDate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author iok
 */
@Service
public class AlephService extends AlephServiceAbstract
{
    private final MainConfiguration mainConfig;
    private final AlephServiceConfig alephServiceConfig;
    private final IdentityService identityService;
    private final OracleRepository oracleRepository;
    private final String[] borXOpsNoSuccessMsg; // A list of Aleph API ops whose response does not contain a success message

    public AlephService(MainConfiguration mainConfig, AlephServiceConfig alephServiceConfig, IdentityService identityService, OracleRepository oracleRepository)
    {
        this.mainConfig = mainConfig;
        this.alephServiceConfig = alephServiceConfig;
        this.identityService = identityService;
        this.oracleRepository = oracleRepository;
        this.borXOpsNoSuccessMsg = new String[] {
            PatronBorXOp.BOR_INFO.getValue()
        };
    }

    /**
     * Retrieves a (existing) patron from Aleph by ID
     * @param patronId
     * @return Map<String, Object>
     */
    public Map<String, Object> getAlephPatron(String patronId)
    {
        Assert.notNull(patronId, "\"patronId\" is required");

        Map<String, Object> result = new HashMap<>();

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("bor_id", patronId);
        urlParams.put("library", this.alephServiceConfig.getAdmLibrary());
        urlParams.put("loans", "N");
        urlParams.put("cash", "N");
        urlParams.put("hold", "N");
        urlParams.put("format", "1");

        Map<String, Object> alephPatron = this.doXRequestUsingPost(PatronBorXOp.BOR_INFO, urlParams);

        if (alephPatron.containsKey("error")) {
            result.put("error", alephPatron.get("error"));
            return result;
        }

        result.put("success", Boolean.TRUE);

        result.put("patronData", alephPatron.get("response"));

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader((String) alephPatron.get("response")));
            Document doc = builder.parse(is);

            Map<String, Object> existingPatron = this.existingPatron(doc);

            if (existingPatron.containsKey("error")) {
                result.put("error", existingPatron.get("error"));
                return result;
            }

            result.put("patron", existingPatron.get("patron"));
        } catch (Exception e) {
            logger.error("Failed to parse patron data: {}", e.getMessage());
            result.put("error", "Failed to parse patron data");
        }

        return result;
    }

    /**
     * Creates a new patron in Aleph
     * @param patron
     * @return Map<String, Object>
     */
    public Map<String, Object> createPatron(Patron patron)
    {
        Map<String, Object> result = new HashMap<>();

        Boolean patronIsCasEmployee = patron.isCasEmployee();

        // Patron status (Aleph reader membership status)
        if (patronIsCasEmployee) {
            patron.setStatus(PatronStatus.STATUS_03.getId());
        } else {
            patron.setStatus(PatronStatus.STATUS_16.getId());
        }

        // Patron Aleph ID and Aleph barcode
        patron.setId(this.generatePatronId());
        patron.setBarcode(this.generatePatronBarcode());

        // Create a patron
        Map<String, String> patronXmlCreation = this.createPatronXML(patron);
        if (patronXmlCreation.containsKey("error")) {
            result.put("error", patronXmlCreation.get("error"));
            return result;
        }
        String patronXml = patronXmlCreation.get("xml");
        result.put("xml-patron", patronXml);
        Map<String, Object> patronCreation = this.updateBorX(patronXml);
        if (patronCreation.containsKey("error")) {
            result.put("error", patronCreation.get("error"));
            return result;
        }

        // Create libraries
        for (String library : this.alephServiceConfig.getLibraries()) {
            Map<String, String> libraryXmlCreation = this.createLibraryXml(patron, library);

            if (libraryXmlCreation.containsKey("error")) {
                result.put("error", libraryXmlCreation.get("error"));
                return result;
            }

            String libraryXml = libraryXmlCreation.get("xml");
            result.put("xml-library", libraryXml);

            Map<String, Object> libraryCreation = this.updateBorX(libraryXml);

            if (libraryCreation.containsKey("error")) {
                result.put("error", libraryCreation.get("error"));
                return result;
            }
        }

        // Create an item (Aleph registration fee)
        if (!patronIsCasEmployee) {
            Map<String, Object> newItem = this.newItem(patron);
            if (newItem.containsKey("error")) {
                result.put("error", newItem.get("error"));
                return result;
            }
            PatronItem item = (PatronItem) newItem.get("item");
            Map<String, String> itemXmlCreation = this.createItemXml(item);
            if (itemXmlCreation.containsKey("error")) {
                result.put("error", itemXmlCreation.get("error"));
                return result;
            }
            String itemXml = itemXmlCreation.get("xml");
            result.put("xml-item", itemXml);
            Map<String, Object> itemCreation = this.createItem(itemXml, this.alephServiceConfig.getSysno());
            if (itemCreation.containsKey("error")) {
                result.put("error", itemCreation.get("error"));
                return result;
            }
            Map<String, String> itemDetails = this.getItemDetails((String) itemCreation.get("response"));
            if (itemDetails.containsKey("error")) {
                result.put("error", itemDetails.get("error"));
                return result;
            }
            String itemId = itemDetails.get("id");
            String itemSequence = itemDetails.get("sequence");
            String itemBarcode = itemDetails.get("barcode");
            String itemIdLong = itemDetails.get("idLong");
            if (itemId == null || itemSequence == null || itemBarcode == null || itemIdLong == null) {
                result.put("error", "chybí údaje o vytvořené jednotce");
                return result;
            }
    
            // Place a hold request
            Map<String, Object> holdRequestPlacement = this.placeHoldRequest(patron, itemId, itemIdLong);
            if (holdRequestPlacement.containsKey("error")) {
                result.put("error", holdRequestPlacement.get("error"));
                return result;
            }
    
            // Cancel the hold request
            Map<String, Object> holdRequestCancellation = this.cancelHoldRequest(itemId, itemSequence);
            if (holdRequestCancellation.containsKey("error")) {
                result.put("error", holdRequestCancellation.get("error"));
                return result;
            }
    
            // Delete the item
            Map<String, Object> itemDeletion = this.deleteItem(itemId, itemSequence, itemBarcode);
            if (itemDeletion.containsKey("error")) {
                result.put("error", itemDeletion.get("error"));
                return result;
            }
        }

        result.put("success", Boolean.TRUE);

        return result;
    }


    /**
     * Updates a (existing) patron in Aleph
     * @param patron
     * @return Map<String, Object>
     */
    public Map<String, Object> updatePatron(Patron patron)
    {
        Map<String, Object> result = new HashMap<>();

        Boolean patronIsCasEmployee = patron.isCasEmployee();

        // Update a patron
        Map<String, String> patronXmlUpdate = this.updatePatronXML(patron);
        if (patronXmlUpdate.containsKey("error")) {
            result.put("error", patronXmlUpdate.get("error"));
            return result;
        }
        String patronXml = patronXmlUpdate.get("xml");
        result.put("xml-patron", patronXml);
        Map<String, Object> patronUpdate = this.updateBorX(patronXml);
        if (patronUpdate.containsKey("error")) {
            result.put("error", patronUpdate.get("error"));
            return result;
        }

        result.put("success", Boolean.TRUE);

        return result;
    }

    /** Deletes an item in Aleph
     * @param itemId - the item ID
     * @param itemSequence - the item sequence
     * @param itemBarcode - the item barcode
     * @return Map<String, Object>
     */
    public Map<String, Object> deleteItem(String itemId, String itemSequence, String itemBarcode) {
        Assert.notNull(itemId, "\"itemId\" is required");
        Assert.notNull(itemSequence, "\"itemSequence\" is required");

        Map<String, Object> result = new HashMap<>();

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("library", this.alephServiceConfig.getAdmLibrary());
        urlParams.put("doc_number", itemId);
        urlParams.put("item_sequence", itemSequence);
        urlParams.put("item_barcode", itemBarcode);

        Map<String, Object> itemDeletion = this.doXRequestUsingPost(PatronBorXOp.DELETE_ITEM, urlParams);

        if (itemDeletion.containsKey("error")) {
            result.put("error", itemDeletion.get("error"));
            return result;
        }

        result.put("success", Boolean.TRUE);

        return result;
    }

    /** Cancels a hold request in Aleph 
     * @param itemId - the item ID
     * @param itemSequence - the item sequence
     * @return Map<String, Object>
     */
    public Map<String, Object> cancelHoldRequest(String itemId, String itemSequence) {
        Assert.notNull(itemId, "\"itemId\" is required");
        Assert.notNull(itemSequence, "\"itemSequence\" is required");

        Map<String, Object> result = new HashMap<>();

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("library", this.alephServiceConfig.getAdmLibrary());
        urlParams.put("doc_number", itemId);
        urlParams.put("item_sequence", itemSequence);
        urlParams.put("sequence", "0001");

        Map<String, Object> holdCancellation = this.doXRequestUsingPost(PatronBorXOp.HOLD_REQ_CANCEL, urlParams);

        if (holdCancellation.containsKey("error")) {
            result.put("error", holdCancellation.get("error"));
            return result;
        }

        result.put("success", Boolean.TRUE);

        return result;
    }


    /** Places a hold request in Aleph
     * @param patron - the patron
     * @param itemId - the item ID
     * @param itemIdLong - the item ID long
     * @return Map<String, Object>
     */
    public Map<String, Object> placeHoldRequest(Patron patron, String itemId, String itemIdLong) {
        Assert.notNull(patron, "\"patron\" is required");
        Assert.notNull(itemId, "\"itemId\" is required");
        Assert.notNull(itemIdLong, "\"itemIdLong\" is required");

        Map<String, Object> result = new HashMap<>();

        String[] urlPathParts = new String[] {
            "patron", patron.getId(), 
            "record", this.alephServiceConfig.getBibLibrary() + itemId, 
            "items", itemIdLong, 
            "hold"
        };

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("lang", patron.getConLng().toString());

        Map<String, Object> newHold = this.newHold("Registrace");
        if (newHold.containsKey("error")) {
            result.put("error", newHold.get("error"));
            return result;
        }
        PatronHold hold = (PatronHold) newHold.get("hold");
        Map<String, String> holdXmlCreation = this.createHoldXml(hold);
        if (holdXmlCreation.containsKey("error")) {
            result.put("error", holdXmlCreation.get("error"));
            return result;
        }
        String holdXml = holdXmlCreation.get("xml");

        Map<String, Object> holdCreation = this.doRestDlfRequest(urlPathParts, urlParams, HttpMethod.PUT, holdXml);

        if (holdCreation.containsKey("error")) {logger.info("BACHA");
            result.put("error", holdCreation.get("error"));
            return result;
        }
        logger.info("OKACKO");
        result.put("success", Boolean.TRUE);

        return result;
    }

    /**
     * Initializes a hold request
     * @param description - the hold request description
     * @return Map<String, Object>
     */
    public Map<String, Object> newHold(String description) {
        Assert.notNull(description, "\"description\" is required");

        Map<String, Object> result = new HashMap<>();

        PatronHold hold = new PatronHold();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String oneMonthFromToday = LocalDate.now().plusMonths(1).format(formatter);

        hold.setPickUpLocation(this.alephServiceConfig.getHomeLibrary());
        hold.setLastInterestDate(oneMonthFromToday);
        hold.setNote1(description);

        result.put("hold", hold);

        return result;
    }

    /**
     * Generates an XML string for a hold request
     * @param hold - the hold request
     * @return Map<String, String>
     */
    public Map<String, String> createHoldXml(PatronHold hold) {
        Assert.notNull(hold, "\"hold\" is required");

        Map<String, String> result = new HashMap<>();

        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/CreateHold.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);

            transformer.setParameter("pickup-location", hold.getPickUpLocation());
            transformer.setParameter("last-interest-date", hold.getLastInterestDate());
            transformer.setParameter("note-1", hold.getNote1());

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
     * Sends a REST-DLF request to Aleph
     * @param urlPathParts - URL path parts
     * @param urlParams - URL parameters
     * @param method - HTTP method
     * @param body - request body
     */
    public Map<String, Object> doRestDlfRequest(String[] urlPathParts, Map<String, String> urlParams, HttpMethod method, String body) {
        Assert.notNull(urlPathParts, "\"urlPathParts\" is required");
        Assert.notNull(urlParams, "\"urlParams\" is required");
        Assert.notNull(method, "\"method\" is required");
        Assert.notNull(body, "\"body\" is required");

        Map<String, Object> result = new HashMap<>();

        String url = this.alephServiceConfig.getRestApiUri() + "/rest-dlf/" + String.join("/", urlPathParts);

        if (!urlParams.isEmpty()) {
            url += "?";
            for (Map.Entry<String, String> entry : urlParams.entrySet()) {
                url += entry.getKey() + "=" + entry.getValue() + "&";
            }
            url = url.substring(0, url.length() - 1);
        }

        Map<String, Object> sendingHttpRequest = this.doHttpRequest(url, method, body);

        if (sendingHttpRequest.containsKey("error")) {
            result.put("error", sendingHttpRequest.get("error"));
            return result;
        }

        String httpRequestResponse = (String) sendingHttpRequest.get("response");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(httpRequestResponse)));
            document.getDocumentElement().normalize();

            String replyCode = document.getElementsByTagName("reply-code").item(0).getTextContent();
  
            if (!replyCode.equals("0000")) {
                String replyText = document.getElementsByTagName("reply-text").item(0).getTextContent();

                result.put("error", replyText + " code " + replyCode);
                return result;
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            result.put("error", e.getMessage());
            getLogger().error(MainService.class.getName(), e);
            return result;
        }

        result.put("success", Boolean.TRUE);
        result.put("response", httpRequestResponse);

        return result;
    }

    /** Retrieves item details from the item creation response body
     * @param response - the response body from the item creation request
     * return Map<String, String>
     */
    public Map<String, String> getItemDetails(String response) {
        Assert.notNull(response, "\"response\" is required");

        Map<String, String> result = new HashMap<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(response)));
            document.getDocumentElement().normalize();

            Element z30 = (Element) document.getElementsByTagName("z30").item(0);
            String idPart = z30.getElementsByTagName("z30-doc-number").item(0).getTextContent();
            String sequenceTmp = z30.getElementsByTagName("z30-item-sequence").item(0).getTextContent();
            String barcode = z30.getElementsByTagName("z30-barcode").item(0).getTextContent();
            String id = String.format("%09d", Integer.parseInt(idPart));
            String sequence = String.format("%06d", Integer.parseInt(sequenceTmp));
            String idLong =  this.alephServiceConfig.getAdmLibrary() + id + sequence;
            
            result.put("barcode", barcode);
            result.put("id", id);
            result.put("sequence", sequence);
            result.put("idLong", idLong);
            logger.info("barcode: {}", barcode);
            logger.info("id: {}", id);
            logger.info("sequence: {}", sequence);
            logger.info("idLong: {}", idLong);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            result.put("error", e.getMessage());
            getLogger().error(MainService.class.getName(), e);
        }

        return result;
    }

    /** 
     * Creates a new Aleph item
     * @param xml - the XML string to be sent to Aleph
     * @param docNumber - the bibliographic document number
     */
    public Map<String, Object> createItem(String xml, String docNumber) {
        Assert.notNull(xml, "\"xml\" is required");
        Assert.notNull(docNumber, "\"docNumber\" is required");

        Map<String, String> params = new HashMap<>();
        params.put("Xml_Full_Req", xml);
        params.put("Adm_Library", this.alephServiceConfig.getAdmLibrary());
        params.put("Adm_Doc_Number", docNumber);
        params.put("Bib_Library", this.alephServiceConfig.getBibLibrary());
        params.put("Bib_Doc_Number", docNumber);

        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> response = this.doXRequestUsingPost(PatronBorXOp.CREATE_ITEM, params);
            if (response.containsKey("error")) {
                result.put("error", response.get("error"));
            } else {
                result.put("success", Boolean.TRUE);
                result.put("response", response.get("response"));
            }
        } catch (RuntimeException e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /** Sends a HTTP request
     * @param url - URL
     * @param method - HTTP method
     * @param body - request body
     * @return Map<String, Object>
     */
    public Map<String, Object> doHttpRequest(String url, HttpMethod method, String body) {
        Assert.notNull(url, "\"url\" is required");
        Assert.notNull(method, "\"method\" is required");
        Assert.notNull(body, "\"body\" is required");

        Map<String, Object> result = new HashMap<>();

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> entity = new HttpEntity<>("post_xml=" + body, headers);
logger.info("AAA doHttpRequest url: {}", url);
logger.info("AAA doHttpRequest body: {}", body);
logger.info("AAA doHttpRequest method: {}", method);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, method, entity, String.class);

            result.put("response", response.getBody().replace("xmlns=", "ns="));
        } catch (HttpServerErrorException e) {
            logger.error("Server error occurred: " + e.getStatusCode() + " " + e.getMessage(), e);
            result.put("error", e.getMessage());
        } catch (HttpClientErrorException e) {
            logger.error("Client error occurred: " + e.getStatusCode() + " " + e.getMessage(), e);
            result.put("error", e.getMessage());
        } catch (RestClientException e) {
            logger.error("HTTP request failed: " + e.getMessage(), e);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /** Sends a POST request to Aleph X-Services
     * @param op - operation to be performed
     * @param params - request parameters
     */
    public Map<String, Object> doXRequestUsingPost(PatronBorXOp op, Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
    
        String url = this.alephServiceConfig.getHost() + ":" + this.alephServiceConfig.getPort() + "/X";
    
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("op", op.getValue());
        body.add("user_name", this.alephServiceConfig.getWwwuser());
        body.add("user_password", this.alephServiceConfig.getWwwpasswd());
    
        for (Map.Entry<String, String> entry : params.entrySet()) {
            body.add(entry.getKey(), entry.getValue());
        }
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
    
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = null;
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (RestClientException e) {
            result.put("error", "HTTP request failed with message: " + e.getMessage() + ", URL: " + url);
            return result;
        }
    
        if (response.getStatusCode().is2xxSuccessful()) {
            String responseBody = response.getBody();
            // logger.info("Complete response body: {}", responseBody);
            
            if (responseBody.contains("Login record belongs to another user") || 
                responseBody.contains("Match found for ID") || 
                responseBody.contains("Failed to generate new User ID")) {
                result.put("error", "insert_fail_login_data");
            } else if (responseBody.contains("Can not ins/upd record")) {
                result.put("error", "insert_fail_z30x");
            } else if (responseBody.contains("Error retrieving Patron System Key")) {
                result.put("error", "patron_not_found");
            } else if (responseBody.contains("Error retrieving Local Patron Record")) {
                result.put("error", "local_patron_not_found");
            } else if (!responseBody.contains("Succeeded") && 
                       !responseBody.contains("success") && 
                       !responseBody.contains("spě") &&   // Czech "úspěch"
                       !responseBody.contains("<reply>ok</reply>") &&
                       !Arrays.asList(this.borXOpsNoSuccessMsg).contains(op.getValue())) {
                logger.info("OP = " + op.getValue());
                logger.error("HTTP request failed with response QQ: " + responseBody);
                result.put("error", "HTTP request failed with response: " + responseBody);
            } else {
                result.put("success", Boolean.TRUE);
                result.put("response", responseBody);
            }
        } else {
            result.put("error", "HTTP request failed with status code: " + response.getStatusCode());
        }
    
        return result;
    }

    /**
     * Sends a request to Aleph X-Services to create/update/delete a patron.
     * @param xml - the XML string to be sent to Aleph
     * @return - true if the request was successful, false otherwise
     */
    public Map<String, Object> updateBorX(String xml) {
        Assert.notNull(xml, "\"xml\" is required");

        Map<String, String> params = new HashMap<>();
        params.put("library", this.alephServiceConfig.getAdmLibrary());
        params.put("update_flag", PatronBoolean.Y.toString());
        params.put("xml_full_req", xml);

        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> response = this.doXRequestUsingPost(PatronBorXOp.UPDATE_BOR, params);
            if (response.containsKey("error")) {
                result.put("error", response.get("error"));
            } else {
                result.put("success", Boolean.TRUE);
            }
        } catch (RuntimeException e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Generates an XML string for a new Aleph patron
     * @param patron - the patron to be created
     * @return 
     */
    public Map<String, String> createPatronXML(Patron patron) {

        Map<String, String> result = new HashMap<>();

        Assert.notNull(patron, "\"patron\" is required");

        String today = TimestampToDate.getTimestampToDate("yyyyMMdd");
        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/CreatePatron.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);
            String patronId = patron.getId();
            String patronName = patron.getName();
            String patronPassword = patron.getVerification();
            Optional<Identity> identity = identityService.findByBankId(patron.getBankIdSub());

            // z303
            transformer.setParameter("z303-match-id", patronId);
            transformer.setParameter("z303-id", patronId);
            transformer.setParameter("z303-name-key", patronName + " " + patronId);
            transformer.setParameter("z303-open-date", today);
            transformer.setParameter("z303-update-date", today);
            transformer.setParameter("z303-con-lng", patron.getConLng().toString());
            transformer.setParameter("z303-name", patronName);
            transformer.setParameter("z303-birth-date", patron.getBirthDate());
            transformer.setParameter("z303-export-consent", patron.getExportConsent());
            transformer.setParameter("z303-last-name", patron.getLastname());
            transformer.setParameter("z303-first-name", patron.getFirstname());

            // z304 - sequence 01
            transformer.setParameter("z304-seq01-id", patronId);
            transformer.setParameter("z304-seq01-address-0", patron.getAddress0());
            transformer.setParameter("z304-seq01-address-1", patron.getAddress1());
            transformer.setParameter("z304-seq01-address-2", patron.getAddress2());
            transformer.setParameter("z304-seq01-zip", patron.getZip());
            transformer.setParameter("z304-seq01-email-address", patron.getEmail());
            transformer.setParameter("z304-seq01-date-from", today);
            transformer.setParameter("z304-seq01-sms-number", patron.getSmsNumber());
            // transformer.setParameter("z304-seq01-telephone-2", patron.getIdCardName());
            // transformer.setParameter("z304-seq01-telephone-3", patron.getIdCardDetail());
            // transformer.setParameter("z304-seq01-telephone-4", patron.getIdCardNumber());

            // z304 - sequence 02
            transformer.setParameter("z304-seq02-id", patronId);
            transformer.setParameter("z304-seq02-address-0", patron.getContactAddress0());
            transformer.setParameter("z304-seq02-address-1", patron.getContactAddress1());
            transformer.setParameter("z304-seq02-address-2", patron.getContactAddress2());
            transformer.setParameter("z304-seq02-zip", patron.getContactZip());
            transformer.setParameter("z304-seq02-date-from", today);

            // z305
            transformer.setParameter("z305-id", patronId);
            transformer.setParameter("z305-open-date", today);
            transformer.setParameter("z305-update-date", today);
            transformer.setParameter("z305-bor-status", patron.getStatus());

            // z308 - ID
            transformer.setParameter("z308-key-type-00-key-data", patronId);
            transformer.setParameter("z308-key-type-00-verification", patronPassword);
            transformer.setParameter("z308-key-type-00-id", patronId);

            // z308 - Barcode
            transformer.setParameter("z308-key-type-01-key-data", patron.getBarcode());
            transformer.setParameter("z308-key-type-01-verification", patronPassword);
            transformer.setParameter("z308-key-type-01-id", patronId);

            // z308 - RFID
            if (patron.getRfid().equals("") == Boolean.FALSE) {
                transformer.setParameter("is-z308-key-type-03", Boolean.TRUE);
                transformer.setParameter("z308-key-type-03-key-data", patron.getRfid());
                transformer.setParameter("z308-key-type-03-id", patronId);
            }

            // z308 - BankID
            if (identity.isPresent()) {
                Long identityId = identity.get().getId();
                transformer.setParameter("z308-key-type-07-key-data", identityId.toString());
                transformer.setParameter("z308-key-type-07-verification", patronPassword);
                transformer.setParameter("z308-key-type-07-id", patronId);
            }

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
     * Generates an XML string for a new Aleph patron
     * @param patron - the patron to be created
     * @return 
     */
    public Map<String, String> updatePatronXML(Patron patron) {

        Map<String, String> result = new HashMap<>();

        Assert.notNull(patron, "\"patron\" is required");

        String today = TimestampToDate.getTimestampToDate("yyyyMMdd");
        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/UpdatePatron.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);
            String patronId = patron.getId();
            String patronName = patron.getName();

            // z303
            transformer.setParameter("z303-match-id", patronId);
            transformer.setParameter("z303-id", patronId);
            transformer.setParameter("z303-name-key", patronName + " " + patronId);
            transformer.setParameter("z303-open-date", today);
            transformer.setParameter("z303-update-date", today);
            transformer.setParameter("z303-con-lng", patron.getConLng().toString());
            transformer.setParameter("z303-name", patronName);
            transformer.setParameter("z303-birth-date", patron.getBirthDate());
            transformer.setParameter("z303-export-consent", patron.getExportConsent());
            transformer.setParameter("z303-last-name", patron.getLastname());
            transformer.setParameter("z303-first-name", patron.getFirstname());

            // z304 - sequence 01
            transformer.setParameter("z304-seq01-id", patronId);
            transformer.setParameter("z304-seq01-address-0", patron.getAddress0());
            transformer.setParameter("z304-seq01-address-1", patron.getAddress1());
            transformer.setParameter("z304-seq01-address-2", patron.getAddress2());
            transformer.setParameter("z304-seq01-zip", patron.getZip());
            transformer.setParameter("z304-seq01-email-address", patron.getEmail());
            transformer.setParameter("z304-seq01-date-from", today);
            transformer.setParameter("z304-seq01-sms-number", patron.getSmsNumber());
            // transformer.setParameter("z304-seq01-telephone-2", patron.getIdCardName());
            // transformer.setParameter("z304-seq01-telephone-3", patron.getIdCardDetail());
            // transformer.setParameter("z304-seq01-telephone-4", patron.getIdCardNumber());

            // z304 - sequence 02
            transformer.setParameter("z304-seq02-id", patronId);
            transformer.setParameter("z304-seq02-address-0", patron.getContactAddress0());
            transformer.setParameter("z304-seq02-address-1", patron.getContactAddress1());
            transformer.setParameter("z304-seq02-address-2", patron.getContactAddress2());
            transformer.setParameter("z304-seq02-zip", patron.getContactZip());
            transformer.setParameter("z304-seq02-date-from", today);

            // z308 - RFID
            if (patron.getRfid().equals("") == Boolean.FALSE) {
                transformer.setParameter("is-z308-key-type-03", Boolean.TRUE);
                transformer.setParameter("z308-key-type-03-key-data", patron.getRfid());
                transformer.setParameter("z308-key-type-03-id", patronId);
            }

            // z308 - BankID
            // transformer.setParameter("z308-key-type-07-key-data", patron.getBankIdSub());
            // transformer.setParameter("z308-key-type-07-verification", patronPassword);
            // transformer.setParameter("z308-key-type-07-id", patronId);

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
    public Map<String, Object> newPatron(Connect userInfo, Identify userProfile)
    {
        Assert.notNull(userInfo, "\"userInfo\" is required");
        Assert.notNull(userProfile, "\"userProfile\" is required");

        Map<String, Object> result = new HashMap<>();

        Patron patron = new Patron();

        // patron.setId(generatePatronId());
        // patron.setBarcode(generatePatronBarcode());

        String fname = userInfo.getGiven_name();      // Joanne
        String mname = Optional.ofNullable(userInfo.getMiddle_name()).orElse("");     // Kathleen
        String lname = userInfo.getFamily_name();     // Rowling

        patron.setFirstname(fname);
        patron.setLastname(Stream.of(mname, lname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Kathleen Rowling
        patron.setName(Stream.of(mname, lname, fname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Kathleen Rowling Joanne

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
                patron.setAddress2(addressCity + " - " + addressCityarea);
            } else {
                patron.setAddress2(addressCity);
            }
            patron.setZip(addressZip);
        } else {
            result.put("error", "chybí adresa trvalého bydliště");
            return result;
        }

        // Patron contact address
        Address contactAddress = userProfile.getAddresses().stream()
            .filter(a -> a.getType() == AddressType.SECONDARY_RESIDENCE)
            .findFirst()
            .orElse(null);
        if (contactAddress != null) {
            String contactAddressStreet = Optional.ofNullable(contactAddress.getStreet()).orElse("");
            String contactAddressNumber = Optional.ofNullable(contactAddress.getEvidencenumber()).orElse("");
            String contactAddressCity = contactAddress.getCity();
            if (contactAddressCity == null) {
                result.put("error", "chybí město kontaktní adresy");
                return result;
            }
            String contactAddressCityarea = Optional.ofNullable(contactAddress.getCityarea()).orElse("");
            String contactAddressZip = contactAddress.getZipcode();
            if (contactAddressZip == null) {
                result.put("error", "chybí PSČ kontaktní adresy");
                return result;
            }

            patron.setContactAddress0(patron.getName());
            patron.setContactAddress1(Stream.of(!contactAddressStreet.equals("") ? contactAddressStreet : contactAddressCityarea, contactAddressNumber)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" ")));
            
            if (!contactAddressCityarea.equals("") && contactAddressCityarea.equals(contactAddressCity)) {
                patron.setContactAddress2(contactAddressCity);
            } else if (!contactAddressCityarea.equals("") && !contactAddressCityarea.equals(contactAddressCity)) {
                patron.setContactAddress2(contactAddressCity + " - " + contactAddressCityarea);
            } else {
                patron.setContactAddress2(contactAddressCity);
            }
            patron.setContactZip(contactAddressZip);
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
        patron.isNew = isNewAlephPatron;
        patron.setAction(isNewAlephPatron ? PatronAction.I : PatronAction.A);

        // BankID
        patron.setBankIdSub(userInfo.getSub());

        result.put("patron", patron);

        return result;
    }

    /**
     * Initializes patron's data based on the Aleph patron data
     * @param alephPatronXml - existing patron data in the Aleph system
     * @return  Map<String, Object>
     */
    public Map<String, Object> existingPatron(Document alephPatronXml)
    {
        Assert.notNull(alephPatronXml, "\"alephPatronXml\" is required");

        Map<String, Object> result = new HashMap<>();
        Patron patron = new Patron();

        try {
            String id = getXmlElementValue(alephPatronXml, "z303-id");
            String status = PatronStatus.getByName(getXmlElementValue(alephPatronXml, "z305-bor-status")).getId();
            String fullName = getXmlElementValue(alephPatronXml, "z303-name");
            String firstName = getXmlElementValue(alephPatronXml, "z303-first-name");
            String lastName = getXmlElementValue(alephPatronXml, "z303-last-name");
            String conLng = getXmlElementValue(alephPatronXml, "z303-con-lng");
            String birthDate = getXmlElementValue(alephPatronXml, "z303-birth-date");
            String exportConsent = getXmlElementValue(alephPatronXml, "z303-export-consent");
            String homeLibrary = getXmlElementValue(alephPatronXml, "z303-home-library");

            // Setting patron details
            patron.setPatronId(id);
            patron.setStatus(status);
            patron.setFirstname(firstName);
            patron.setLastname(lastName);
            patron.setName(fullName);
            patron.setConLng(conLng.equals("CZE") ? PatronLanguage.CZE : PatronLanguage.ENG);
            patron.setBirthDate(birthDate.replace("/", "-"));
            patron.setExportConsent(exportConsent.equals("Y") ? PatronBoolean.Y : PatronBoolean.N);
            patron.setHomeLibrary(homeLibrary);

            // Address details
            NodeList z304Nodes = alephPatronXml.getElementsByTagName("z304");
            for (int i = 0; i < z304Nodes.getLength(); i++) {
                Node z304Node = z304Nodes.item(i);
                String addressType = getXmlElementValue(z304Node, "z304-address-type");
                if ("01".equals(addressType)) {
                    patron.setAddress0(getXmlElementValue(z304Nodes.item(i), "z304-address-0"));
                    patron.setAddress1(getXmlElementValue(z304Nodes.item(i), "z304-address-1"));
                    patron.setAddress2(getXmlElementValue(z304Nodes.item(i), "z304-address-2"));
                    patron.setZip(getXmlElementValue(z304Nodes.item(i), "z304-zip"));
                    patron.setSmsNumber(getXmlElementValue(z304Nodes.item(i), "z304-sms-number"));
                } else if ("02".equals(addressType)) {
                    patron.setContactAddress0(getXmlElementValue(z304Nodes.item(i), "z304-address-0"));
                    patron.setContactAddress1(getXmlElementValue(z304Nodes.item(i), "z304-address-1"));
                    patron.setContactAddress2(getXmlElementValue(z304Nodes.item(i), "z304-address-2"));
                    patron.setContactZip(getXmlElementValue(z304Nodes.item(i), "z304-zip"));
                }
            }

            // Barcode and verification
            NodeList z308Nodes = alephPatronXml.getElementsByTagName("z308");
            for (int i = 0; i < z308Nodes.getLength(); i++) {
                String keyType = getXmlElementValue(z308Nodes.item(i), "z308-key-type");
                if ("01".equals(keyType)) {
                    patron.setBarcode(getXmlElementValue(z308Nodes.item(i), "z308-key-data"));
                    patron.setVerification(getXmlElementValue(z308Nodes.item(i), "z308-verification"));
                } else if ("03".equals(keyType)) {
                    patron.setRfid(getXmlElementValue(z308Nodes.item(i), "z308-key-data"));
                } else if ("05".equals(keyType)) {
                    patron.setIdCardNumber(getXmlElementValue(z308Nodes.item(i), "z308-key-data"));
                }
            }

            result.put("patron", patron);
            logger.info("Existing patron: {}", patron.toJson());
        } catch (Exception e) {
            logger.error("Failed to parse existing patron data: {}", e.getMessage());
            result.put("error", "Failed to parse existing patron data");
        }

        return result;
    }

    private String getXmlElementValue(Document doc, String tagName) {
        return doc.getElementsByTagName(tagName).item(0).getTextContent();
    }

    private String getXmlElementValue(Node node, String tagName) {
        NodeList nodeList = ((Element) node).getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    /**
     * Initializes an item based on the patron's data
     * @param patron
     * @return Map<String, Object>
     */
    public Map<String, Object> newItem(Patron patron) {
        Assert.notNull(patron, "\"patron\" is required");

        Map<String, Object> result = new HashMap<>();

        PatronItem item = new PatronItem();

        String today = TimestampToDate.getTimestampToDate("yyyyMMdd");
        String itemDocNumber = this.alephServiceConfig.getSysno();
        String itemBarcode = this.alephServiceConfig.getItemBarcodePrefix() + itemDocNumber + today;
        String itemStatus = PatronStatus.STATUS_16.getRegistrationItemStatusId();

        item.setDocNumber(itemDocNumber);
        item.setBarcode(itemBarcode);
        item.setStatus(itemStatus);

        result.put("item", item);

        return result;
    }

    /**
     * Generates an XML string for an item
     * @param item
     * @return
     */
    public Map<String, String> createItemXml(PatronItem item) {
        Map<String, String> result = new HashMap<>();

        Assert.notNull(item, "\"item\" is required");

        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/CreateItem.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);

            // z30
            transformer.setParameter("z30-doc-number", item.getDocNumber());
            transformer.setParameter("z30-barcode", item.getBarcode());
            transformer.setParameter("z30-item-status", item.getStatus());

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
     * Generates an XML string for a patron-related library
     * @patron patron
     * @library library
     * @return Map<String, String>
     */
    public Map<String, String> createLibraryXml(Patron patron, String library) {
        Map<String, String> result = new HashMap<>();

        Assert.notNull(patron, "\"patron\" is required");
        Assert.notNull(library, "\"library\" is required");

        String today = TimestampToDate.getTimestampToDate("yyyyMMdd");
        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/CreateLibrary.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);
            String patronId = patron.getId();

            // z303
            transformer.setParameter("z303-match-id", patronId);
            transformer.setParameter("z303-id", patronId);

            // z305
            transformer.setParameter("z305-id", patronId);
            transformer.setParameter("z305-sub-library", library);
            transformer.setParameter("z305-open-date", today);
            transformer.setParameter("z305-update-date", today);
            transformer.setParameter("z305-bor-status", patron.getStatus());

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
     * Checks if a RFID is already in use by any patron except the given one (if any).
     * @param rfid
     * @param patronId
     * @return Boolean
     */
    public Boolean isRfidInUse(String rfid, String patronId) {
        Assert.notNull(rfid, "\"rfid\" is required");

        // TRUE if RFID is already in use by any patron except the given one (if any)
        return this.oracleRepository.getRFIDRowsCount(rfid, patronId) > 0;
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

        Patron patron = new Patron();

        // patron.setId(generatePatronId());
        // patron.setBarcode(generatePatronBarcode());

        String fname = userInfo.getGiven_name();      // Joanne
        String mname = this.generateTestingMname();   // Kathleen
        String lname = userInfo.getFamily_name();     // Rowling
        patron.setFirstname(fname);
        patron.setLastname(Stream.of(mname, lname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Kathleen Rowling
        patron.setName(Stream.of(mname, lname, fname)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining(" ")));       // Kathleen Rowling Joanne

        patron.setEmail(Optional.ofNullable(this.generateTestingEmail()).orElse(""));

        String birthdate = this.generateTestingBirthday();   // 2003-07-25
        if (birthdate == null) {
            result.put("error", "chybí datum narození");
            return result;
        }
        patron.setBirthDate(birthdate.replace("-", ""));  // 2003-07-25 => 20030725

        String smsNumber = this.generateTestingPhone(); logger.info("smsNumber: {}", smsNumber);
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
                patron.setAddress2(addressCity + " - " + addressCityarea);
            } else {
                patron.setAddress2(addressCity);
            }
            patron.setZip(addressZip);
        } else {
            result.put("error", "chybí adresa trvalého bydliště");
            return result;
        }

        // Patron contact address
        Address contactAddress = userProfile.getAddresses().stream()
            .filter(a -> a.getType() == AddressType.SECONDARY_RESIDENCE)
            .findFirst()
            .orElse(null);
        if (contactAddress != null) {
            String contactAddressStreet = Optional.ofNullable(contactAddress.getStreet()).orElse("");
            String contactAddressNumber = Optional.ofNullable(contactAddress.getEvidencenumber()).orElse("");
            String contactAddressCity = contactAddress.getCity();
            if (contactAddressCity == null) {
                result.put("error", "chybí město kontaktní adresy");
                return result;
            }
            String contactAddressCityarea = Optional.ofNullable(contactAddress.getCityarea()).orElse("");
            String contactAddressZip = contactAddress.getZipcode();
            if (contactAddressZip == null) {
                result.put("error", "chybí PSČ kontaktní adresy");
                return result;
            }

            patron.setContactAddress0(patron.getName());
            patron.setContactAddress1(Stream.of(!contactAddressStreet.equals("") ? contactAddressStreet : contactAddressCityarea, contactAddressNumber)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" ")));
            
            if (!contactAddressCityarea.equals("") && contactAddressCityarea.equals(contactAddressCity)) {
                patron.setContactAddress2(contactAddressCity);
            } else if (!contactAddressCityarea.equals("") && !contactAddressCityarea.equals(contactAddressCity)) {
                patron.setContactAddress2(contactAddressCity + " - " + contactAddressCityarea);
            } else {
                patron.setContactAddress2(contactAddressCity);
            }
            patron.setContactZip(contactAddressZip);
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
        patron.isNew = isNewAlephPatron;
        patron.setAction(isNewAlephPatron ? PatronAction.I : PatronAction.A); logger.info("patron.getAction(): {}", patron.getAction());

        // BankID
        patron.setBankIdSub(userInfo.getSub());

        result.put("patron", patron);

        return result;
    }

    /**
     * Generates patron's id, at least 9 characters long
     */
    public String generatePatronId() {
        String prefix = this.mainConfig.getId_prefix();
        Long maxVal = this.identityService.getMaxId();
        Long newVal = 1L;

        if (maxVal != null) {
            newVal = maxVal + 1;
        }

        return String.format("%s%05d", prefix, newVal);
    }

    /**
     * Generates patron's barcode, at least 10 characters long
     */
    public String generatePatronBarcode() {
        String prefix = mainConfig.getBarcode_prefix();
        Long maxVal = this.identityService.getMaxId();
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
     * Checks if a patron already exists in the Aleph Oracle DB (based on the name and birthdate)
     * or if the patron was already verified via Bank ID.
     * @param name Patron's name
     * @param birthDate Patron's birthdate
     * @return true if patron exists, false otherwise
     */
    public boolean isNewAlephPatron(Patron patron) {
        boolean isNewInOracle = (oracleRepository.getPatronRowsCount(patron.getName(), patron.getBirthDate()) == 0);
        boolean isVerifiedAndAlephLinked = identityService.findAlephLinkedByBankId(patron.getBankIdSub()).isPresent();
        return isNewInOracle || !isVerifiedAndAlephLinked;
    }

    private String generateTestingMname() {
        return "Test";
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

    private String generateTestingPhone() {
        Random random = new Random();
        int randomNum = 100000000 + random.nextInt(900000000);
        return "+420" + randomNum;
    }
}
