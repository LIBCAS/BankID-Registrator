package cz.cas.lib.bankid_registrator.services;

import cz.cas.lib.bankid_registrator.configurations.AlephServiceConfig;
import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.oracle.OracleRepository;
import cz.cas.lib.bankid_registrator.entities.patron.PatronAction;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBorXOp;
import cz.cas.lib.bankid_registrator.entities.patron.PatronHold;
import cz.cas.lib.bankid_registrator.entities.patron.PatronItem;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.entities.patron.PatronStatus;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import cz.cas.lib.bankid_registrator.util.StringUtils;
import cz.cas.lib.bankid_registrator.util.TimestampToDate;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

public class AlephService extends AlephServiceAbstract
{
    protected final MainConfiguration mainConfig;
    protected final AlephServiceConfig alephServiceConfig;
    protected final IdentityService identityService;
    protected final OracleRepository oracleRepository;
    protected String[] borXOpsNoSuccessMsg; // A list of Aleph API ops whose response does not contain a success message

    public AlephService(MainConfiguration mainConfig, AlephServiceConfig alephServiceConfig, IdentityService identityService, OracleRepository oracleRepository)
    {
        this.mainConfig = mainConfig;
        this.alephServiceConfig = alephServiceConfig;
        this.identityService = identityService;
        this.oracleRepository = oracleRepository;
        this.borXOpsNoSuccessMsg = new String[] {
            PatronBorXOp.BOR_INFO.getValue(),
            PatronBorXOp.BOR_AUTH.getValue()
        };
    }

    /**
     * Retrieves a (existing) patron from Aleph by ID
     * @param patronId
     * @param includeAdvancedData - whether to include advanced patron data such as patron's membership expiry date
     * @return Map<String, Object>
     */
    public Map<String, Object> getAlephPatron(String patronId, boolean includeAdvancedData)
    {
        Assert.notNull(patronId, "getAlephPatron: \"patronId\" is required");
        Assert.notNull(includeAdvancedData, "getAlephPatron: \"includeAdvancedData\" is required");

        Map<String, Object> result = new HashMap<>();

        // Get basic patron data
        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("bor_id", patronId);
        urlParams.put("library", this.alephServiceConfig.getAdmLibrary());
        urlParams.put("loans", "N");
        urlParams.put("cash", "N");
        urlParams.put("hold", "N");
        urlParams.put("format", "1");

        Map<String, Object> patronDataGet = this.doXRequestUsingPost(PatronBorXOp.BOR_INFO, urlParams);

        if (patronDataGet.containsKey("error")) {
            result.put("error", patronDataGet.get("error"));
            return result;
        }

        String patronDataResponse = patronDataGet.get("response").toString();
        result.put("patronData", patronDataResponse);

        Patron patron;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(patronDataResponse));
            Document doc = builder.parse(is);

            Map<String, Object> patronDataParse = this.parseAlephPatron(doc);

            if (patronDataParse.containsKey("error")) {
                result.put("error", patronDataParse.get("error"));
                return result;
            }

            patron = (Patron) patronDataParse.get("patron");
        } catch (Exception e) {
            logger.error("Failed to parse patron data: {}", e.getMessage());
            result.put("error", "Failed to parse patron data");
            return result;
        }

        if (includeAdvancedData) {
            // Get advanced patron data (expiry date)
            urlParams = new HashMap<>();
            urlParams.put("bor_id", patron.getBarcode());
            urlParams.put("library", this.alephServiceConfig.getAdmLibrary());
            urlParams.put("verification", patron.getVerification());
            urlParams.put("sub_library", this.alephServiceConfig.getHomeLibrary());
            urlParams.put("lang", "cze");

            patronDataGet = this.doXRequestUsingPost(PatronBorXOp.BOR_AUTH, urlParams);

            if (patronDataGet.containsKey("error")) {
                result.put("error", patronDataGet.get("error"));
                return result;
            }
    
            patronDataResponse = patronDataGet.get("response").toString();
            result.put("patronDataAdvanced", patronDataResponse);
    
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(patronDataResponse));
                Document doc = builder.parse(is);
    
                Map<String, Object> patronDataParse = this.parseAlephPatronAdvanced(doc, patron);
    
                if (patronDataParse.containsKey("error")) {
                    result.put("error", patronDataParse.get("error"));
                    return result;
                }
    
                patron = (Patron) patronDataParse.get("patron");
            } catch (Exception e) {
                logger.error("Failed to parse patron data: {}", e.getMessage());
                result.put("error", "Failed to parse patron data");
                return result;
            }
        }

        result.put("patron", patron);
        result.put("success", Boolean.TRUE);

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

        boolean patronIsCasEmployee = patron.getIsCasEmployee();

        // Patron action
        patron.setAction(PatronAction.I);

        // Patron status (Aleph reader membership status)
        if (patronIsCasEmployee) {
            patron.setStatus(PatronStatus.STATUS_03.getId());
        } else {
            patron.setStatus(PatronStatus.STATUS_16.getId());
        }

        // Patron Aleph ID and Aleph barcode
        Long maxBankidPatronId = this.getMaxBankIdZ303RecKey();
        patron.setId(this.generatePatronId(maxBankidPatronId));
        patron.setBarcode(this.generatePatronBarcode(maxBankidPatronId));

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
            String actionDescription = "Registrace";

            Map<String, Object> newItem = this.newItem(patron, PatronStatus.STATUS_16.getRegistrationItemStatusId());
            if (newItem.containsKey("error")) {
                result.put("error", newItem.get("error"));
                return result;
            }
            PatronItem item = (PatronItem) newItem.get("item");
            item.setDescription(actionDescription);
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
            Map<String, Object> holdRequestPlacement = this.placeHoldRequest(patron, itemId, itemIdLong, actionDescription);
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
     * @param patron - new patron with updated data
     * @param alephPatron - old patron with data from Aleph
     * @return Map<String, Object>
     */
    public Map<String, Object> updatePatron(Patron patron, Patron alephPatron)
    {
        Map<String, Object> result = new HashMap<>();

        boolean patronIsCasEmployee = patron.getIsCasEmployee();

        // Patron action
        patron.setAction(PatronAction.A);

        // Patron status (Aleph reader membership status)
        if (patronIsCasEmployee) {
            patron.setStatus(PatronStatus.STATUS_03.getId());
        } else {
            patron.setStatus(PatronStatus.STATUS_16.getId());
        }

        // Update the patron
        Map<String, String> patronXmlUpdate = this.updatePatronXML(patron, alephPatron);
        if (patronXmlUpdate.containsKey("error")) {
            result.put("error", patronXmlUpdate.get("error"));
            return result;
        }
        String patronXml = patronXmlUpdate.get("xml");
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
            String actionDescription = "Obnovení registrace";

            Map<String, Object> newItem = this.newItem(patron, PatronStatus.STATUS_16.getRenewalItemStatusId());
            if (newItem.containsKey("error")) {
                result.put("error", newItem.get("error"));
                return result;
            }
            PatronItem item = (PatronItem) newItem.get("item");
            item.setDescription(actionDescription);
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
            Map<String, Object> holdRequestPlacement = this.placeHoldRequest(patron, itemId, itemIdLong, actionDescription);
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

        // Update patron's status (patron membership status + membership expiry date)
        List<String> libraries = new ArrayList<>(Arrays.asList(this.alephServiceConfig.getLibraries()));
        libraries.add(this.alephServiceConfig.getHomeLibrary());
        String updatePatronStatusXml = this.updatePatronStatusXml(patron, libraries);

        Map<String, Object> updatePatronStatus = this.updateBorX(updatePatronStatusXml);

        if (updatePatronStatus.containsKey("error")) {
            result.put("error", updatePatronStatus.get("error"));
            return result;
        }

        result.put("success", Boolean.TRUE);

        return result;
    }

    /**
     * Deletes patron's BankIdSub identifier in Aleph
     * TODO: This method is only for testing purposes to delete patron's BankIdSub identifier in Aleph because you cannot create a new patron with the same BankIdSub identifier in Aleph
     * @param patronId
     * @return void
     * @throws Exception
     */
    public void deletePatronBankIdSub(String patronId) throws Exception
    {
        try {
            Integer deletedRows = this.oracleRepository.deleteZ308RecordType07(patronId);
            getLogger().info("deletedRows: {}", deletedRows);
        } catch (Exception e) {
            getLogger().error(MainService.class.getName(), e);
            throw new Exception("Failed to delete patron's (" + patronId + ") BankIdSub identifier in Aleph");
        }
    }

    /** Deletes an item in Aleph
     * @param itemId - the item ID
     * @param itemSequence - the item sequence
     * @param itemBarcode - the item barcode
     * @return Map<String, Object>
     */
    public Map<String, Object> deleteItem(String itemId, String itemSequence, String itemBarcode)
    {
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
     * @param description - the hold request description
     * @return Map<String, Object>
     */
    public Map<String, Object> placeHoldRequest(Patron patron, String itemId, String itemIdLong, String description) {
        Assert.notNull(patron, "\"patron\" is required");
        Assert.notNull(itemId, "\"itemId\" is required");
        Assert.notNull(itemIdLong, "\"itemIdLong\" is required");
        Assert.notNull(description, "\"description\" is required");

        Map<String, Object> result = new HashMap<>();

        String[] urlPathParts = new String[] {
            "patron", patron.getId(), 
            "record", this.alephServiceConfig.getBibLibrary() + itemId, 
            "items", itemIdLong, 
            "hold"
        };

        Map<String, String> urlParams = new HashMap<>();
        urlParams.put("lang", patron.getConLng().toString());

        Map<String, Object> newHold = this.newHold(description);
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

        if (holdCreation.containsKey("error")) {
            result.put("error", holdCreation.get("error"));
            return result;
        }

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
    public Map<String, String> createPatronXML(Patron patron)
    {
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
            if (!StringUtils.isEmpty(patron.getContactAddress1(), patron.getContactAddress2(), patron.getContactZip())) {
                // Set the contact address if it is provided
                transformer.setParameter("is-z304-seq02", Boolean.TRUE);
                transformer.setParameter("z304-seq02-id", patronId);
                transformer.setParameter("z304-seq02-address-0", patron.getContactAddress0());
                transformer.setParameter("z304-seq02-address-1", patron.getContactAddress1());
                transformer.setParameter("z304-seq02-address-2", patron.getContactAddress2());
                transformer.setParameter("z304-seq02-zip", patron.getContactZip());
                transformer.setParameter("z304-seq02-email-address", patron.getEmail());
                transformer.setParameter("z304-seq02-date-from", today);
                transformer.setParameter("z304-seq02-sms-number", patron.getSmsNumber());
            }

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
            getLogger().info("createPatronXML: {}", xmlString);
            result.put("xml", xmlString);
        } catch (TransformerException ex) {
            result.put("error", ex.getMessage());
            getLogger().error(MainService.class.getName(), ex);
        }

        return result;
    }

    /**
     * Generates an XML string for a new Aleph patron
     * @param patron - new patron with updated data
     * @param alephPatron - old patron with data from Aleph
     * @return 
     */
    public Map<String, String> updatePatronXML(Patron patron, Patron alephPatron)
    {
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
            if (!StringUtils.isEmpty(patron.getContactAddress1(), patron.getContactAddress2(), patron.getContactZip())) {
                // Set the contact address if it is provided
                transformer.setParameter("is-z304-seq02", Boolean.TRUE);
                transformer.setParameter("z304-seq02-id", patronId);
                transformer.setParameter("z304-seq02-address-0", patron.getContactAddress0());
                transformer.setParameter("z304-seq02-address-1", patron.getContactAddress1());
                transformer.setParameter("z304-seq02-address-2", patron.getContactAddress2());
                transformer.setParameter("z304-seq02-zip", patron.getContactZip());
                transformer.setParameter("z304-seq02-email-address", patron.getEmail());
                transformer.setParameter("z304-seq02-date-from", today);
                transformer.setParameter("z304-seq02-sms-number", patron.getSmsNumber());
            } else {
                if (!StringUtils.isEmpty(alephPatron.getContactAddress1(), alephPatron.getContactAddress2(), alephPatron.getContactZip())) {
                    // Delete the contact address if it is not provided and it is present in Aleph
                    transformer.setParameter("is-z304-seq02", Boolean.TRUE);
                    transformer.setParameter("z304-seq02-record-action", "D");
                } else {
                    // Do not set the contact address if it is not provided and it is not present in Aleph
                    transformer.setParameter("is-z304-seq02", Boolean.FALSE);
                }
            }

            // z308 - RFID
            // If the new RFID is provided and the old RFID was also provided, delete the old RFID ...
            if (patron.getRfid().equals("") == Boolean.FALSE && alephPatron.getRfid().equals("") == Boolean.FALSE) {
                transformer.setParameter("is-z308-key-type-03-d", Boolean.TRUE);
                transformer.setParameter("z308-key-type-03-d-key-data", alephPatron.getRfid());
                transformer.setParameter("z308-key-type-03-d-id", patronId);
            }
            // ... and create the new RFID
            if (patron.getRfid().equals("") == Boolean.FALSE) {
                transformer.setParameter("is-z308-key-type-03", Boolean.TRUE);
                transformer.setParameter("z308-key-type-03-key-data", patron.getRfid());
                transformer.setParameter("z308-key-type-03-id", patronId);
            }

            StringWriter stringWriter = new StringWriter();
            Result streamResult = new StreamResult(stringWriter);
            transformer.transform(stylesheetSource, streamResult);
            if (this.mainConfig.getRewrite_aleph_batch_xml_header()) {
                xmlString = stringWriter.getBuffer().toString().replaceFirst("\\<\\?(xml){1}\\s{1,}.*\\?\\>", xmlString);
            } else {
                xmlString = stringWriter.getBuffer().toString();
            }
            getLogger().info("updatePatronXML: {}", xmlString);
            result.put("xml", xmlString);
        } catch (TransformerException ex) {
            result.put("error", ex.getMessage());
            getLogger().error(MainService.class.getName(), ex);
        }

        return result;
    }

    /**
     * Initializes patron's data based on the Aleph patron data
     * @param alephPatronXml - existing patron data in the Aleph system
     * @return  Map<String, Object>
     */
    public Map<String, Object> parseAlephPatron(Document alephPatronXml)
    {
        Assert.notNull(alephPatronXml, "\"alephPatronXml\" is required");

        Map<String, Object> result = new HashMap<>();
        Patron patron = new Patron();

        try {
            String id = getXmlElementValue(alephPatronXml, "z303-id");
            String status = PatronStatus.getByName(getXmlElementValue(alephPatronXml, "z305-bor-status")).getId();
            String fullName = getXmlElementValue(alephPatronXml, "z303-name");
            String conLng = getXmlElementValue(alephPatronXml, "z303-con-lng");
            String birthDate = getXmlElementValue(alephPatronXml, "z303-birth-date");
            String exportConsent = getXmlElementValue(alephPatronXml, "z303-export-consent");
            String homeLibrary = getXmlElementValue(alephPatronXml, "z303-home-library");

            // Retrieving the first and last name in this way is impossible because our Aleph is configured to store both the first and last name in the z303-name field:
            // String firstName = getXmlElementValue(alephPatronXml, "z303-first-name");
            // String lastName = getXmlElementValue(alephPatronXml, "z303-last-name");

            // To retrieve the first and last name, we need to split the z303-name field in the same way as it was stored (i.e. the middle name is a part of the last name):
            String[] nameParts = fullName.split(" ");
            String firstName = nameParts[nameParts.length - 1];
            String lastName = fullName.substring(0, fullName.length() - firstName.length() - 1);

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
                    patron.setEmail(getXmlElementValue(z304Nodes.item(i), "z304-email-address"));
                    patron.setContactAddress0(getXmlElementValue(z304Nodes.item(i), "z304-address-0"));
                } else if ("02".equals(addressType)) {
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
                } else if ("07".equals(keyType)) {
                    patron.setBankIdSub(getXmlElementValue(z308Nodes.item(i), "z308-key-data").toLowerCase());  // We need to convert to lowercase because Aleph z308-key-data store values in uppercase and BankIdSub is in lowercase (BankIdSub is in a standard UUID)
                }
            }

            result.put("patron", patron);
            logger.info("Aleph patron: {}", patron.toJson());
        } catch (Exception e) {
            logger.error("Failed to parse Aleph patron data: {}", e.getMessage());
            result.put("error", "Failed to parse Aleph patron data");
        }

        return result;
    }

    /**
     * Initializes patron's advanced data based on the Aleph patron data
     * @param alephPatronXml - existing advanced patron data in the Aleph system
     * @param patron - the patron to be updated with advanced data
     * @return  Map<String, Object>
     */
    public Map<String, Object> parseAlephPatronAdvanced(Document alephPatronXml, Patron patron)
    {
        Assert.notNull(alephPatronXml, "\"alephPatronXml\" is required");
        Assert.notNull(patron, "\"patron\" is required");

        Map<String, Object> result = new HashMap<>();

        try {
            String expiryDate = getXmlElementValue(alephPatronXml, "z305-expiry-date");
            String membershipTypeName = getXmlElementValue(alephPatronXml, "z305-bor-status");

            patron.setExpiryDate(expiryDate);
            patron.setIsCasEmployee(PatronStatus.getByName(membershipTypeName).isEmployee());

            result.put("patron", patron);
            logger.info("Aleph patron (advanced): {}", patron.toJson());
        } catch (Exception e) {
            logger.error("Failed to parse Aleph patron advanced data: {}", e.getMessage());
            result.put("error", "Failed to parse Aleph patron advanced data");
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
     * @param itemStatus
     * @return Map<String, Object>
     */
    public Map<String, Object> newItem(Patron patron, String itemStatus) {
        Assert.notNull(patron, "\"patron\" is required");

        Map<String, Object> result = new HashMap<>();

        PatronItem item = new PatronItem();

        String today = TimestampToDate.getTimestampToDate("yyyyMMdd");
        String itemDocNumber = this.alephServiceConfig.getSysno();
        String itemBarcode = this.alephServiceConfig.getItemBarcodePrefix() + itemDocNumber + today;

        item.setDocNumber(itemDocNumber);
        item.setBarcode(itemBarcode);
        item.setStatus(itemStatus);

        result.put("item", item);

        return result;
    }

    /**
     * Generates an XML string for updating an Aleph patron status (i.e. patron membership status + membership expiry date)
     * @param patron
     * @param libraries - an array of libraries where the update should be applied
     * @return
     */
    private String updatePatronStatusXml(Patron patron, List<String> libraries)
    {
        String patronId = patron.getId();
        String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String expiryDate = DateUtils.convertDateFormat(patron.getExpiryDate(), "dd/MM/yyyy", "yyyyMMdd");
        String patronStatus = patron.getStatus();

        StringBuilder xmlBuilder = new StringBuilder();

        xmlBuilder.append("<?xml version='1.0'?>")
            .append("<p-file-20>")
            .append("<patron-record>")
            .append("<z303>")
                .append("<match-id-type>00</match-id-type>")
                .append("<match-id>").append(patronId).append("</match-id>")
                .append("<record-action>A</record-action>")
                .append("<z303-id>").append(patronId).append("</z303-id>")
            .append("</z303>");

        for (String library : libraries) {
            xmlBuilder.append("<z305>")
                    .append("<record-action>A</record-action>")
                    .append("<z305-id>").append(patronId).append("</z305-id>")
                    .append("<z305-sub-library>").append(library).append("</z305-sub-library>")
                    .append("<z305-open-date>").append(todayDate).append("</z305-open-date>")
                    .append("<z305-update-date>").append(todayDate).append("</z305-update-date>")
                    .append("<z305-bor-status>").append(patronStatus).append("</z305-bor-status>")
                    .append("<z305-expiry-date>").append(expiryDate).append("</z305-expiry-date>")
                .append("</z305>");
        }

        xmlBuilder.append("</patron-record>")
            .append("</p-file-20>");

        return xmlBuilder.toString();
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
            transformer.setParameter("z30-description", item.getDescription());

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
    public boolean isRfidInUse(String rfid, String patronId) {
        Assert.notNull(rfid, "\"rfid\" is required");

        // TRUE if RFID is already in use by any patron except the given one (if any)
        return this.oracleRepository.getRFIDRowsCount(rfid, patronId) > 0;
    }

    /**
     * Checks if an email is already in use by any patron except the given one (if any).
     * @param email
     * @param patronId
     * @return Boolean
     */
    public boolean isEmailInUse(String email, String patronId) {
        Assert.notNull(email, "\"email\" is required");

        // TRUE if email is already in use by any patron except the given one (if any)
        return this.oracleRepository.isExistingPatronEmail(email, patronId);
    }

    /**
     * Generates an XML string for a patron's password update
     * @param patronId
     * @param passwordOld - old password
     * @param passwordNew - new password
     * @return
     */
    public Map<String, String> createPatronPswUpdateXml(String patronId, String passwordOld, String passwordNew) {
        Map<String, String> result = new HashMap<>();

        Assert.notNull(patronId, "createPatronPswUpdateXml: \"patronId\" is required");
        Assert.notNull(passwordOld, "createPatronPswUpdateXml: \"passwordOld\" is required");
        Assert.notNull(passwordNew, "createPatronPswUpdateXml: \"passwordNew\" is required");

        String xmlString = "<?xml version=\"1.0\"?>";

        Source stylesheetSource = new StreamSource();
        stylesheetSource.setSystemId("classpath:/xml/AlephPatronPswUpdate.xsl");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer(stylesheetSource);
            String encodedPasswordOld = URLEncoder.encode(passwordOld, StandardCharsets.UTF_8.toString());
            String encodedPasswordNew = URLEncoder.encode(passwordNew, StandardCharsets.UTF_8.toString());

            transformer.setParameter("oldPassword", encodedPasswordOld);
            transformer.setParameter("newPassword", encodedPasswordNew);            

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
        } catch (UnsupportedEncodingException ex) {
            result.put("error", "Encoding error: " + ex.getMessage());
            getLogger().error("UnsupportedEncodingException in createPatronPswUpdateXml", ex);
        }

        return result;
    }

    /**
     * Updates a patron's password in Aleph
     * @param patronId - patron ID
     * @param passwordNew - new password
     * @return
     */
    public Map<String, Object> updatePatronPassword(String patronId, String passwordNew) {
        Assert.notNull(patronId, "updatePatronPassword: \"patronId\" is required");
        Assert.notNull(passwordNew, "updatePatronPassword: \"passwordNew\" is required");

        Map<String, Object> result = new HashMap<>();

        Map<String, Object> alephPatronGet = this.getAlephPatron(patronId, false);

        if (alephPatronGet.containsKey("error")) {
            result.put("error", alephPatronGet.get("error"));
            return result;
        }

        Patron alephPatron = (Patron) alephPatronGet.get("patron");
        String passwordOld = alephPatron.getVerification();

        String[] urlPathParts = new String[] {
            "patron", patronId, 
            "patronInformation",
            "password"
        };

        Map<String, String> urlParams = new HashMap<>();

        Map<String, String> patronPswUpdateXml = this.createPatronPswUpdateXml(patronId, passwordOld, passwordNew);
        if (patronPswUpdateXml.containsKey("error")) {
            result.put("error", patronPswUpdateXml.get("error"));
            return result;
        }

        Map<String, Object> patronPswUpdate = this.doRestDlfRequest(urlPathParts, urlParams, HttpMethod.POST, patronPswUpdateXml.get("xml"));

        if (patronPswUpdate.containsKey("error")) {
            result.put("error", patronPswUpdate.get("error"));
            return result;
        }

        result.put("success", Boolean.TRUE);

        return result;
    }

    /**
     * Get the maximum Aleph patron ID created so far via the Bank ID
     * @return Long
     */
    public Long getMaxBankIdZ303RecKey() {
        // return this.identityService.getMaxId();  // TODO: Use this line in production
        return this.oracleRepository.getMaxBankIdZ303RecKey();
    }

    /**
     * Generates patron's id, at least 9 characters long
     * @param maxVal - the maximum Aleph patron ID created so far via the Bank ID
     */
    public String generatePatronId(Long maxVal) {
        String prefix = this.mainConfig.getId_prefix();
        Long newVal = 1L;

        if (maxVal != null) {
            newVal = maxVal + 1;
        }

        return String.format("%s%05d", prefix, newVal);
    }

    /**
     * Generates patron's barcode, at least 10 characters long
     * @param maxVal - the maximum Aleph patron ID created so far via the Bank ID
     */
    public String generatePatronBarcode(Long maxVal) {
        String prefix = this.mainConfig.getBarcode_prefix();
        Long newVal = 1L;

        if (maxVal != null) {
            newVal = maxVal + 1;
        }

        return String.format("%s%05d", prefix, newVal);
    }

    /**
     * Generates patron's password, 8 characters long
     */
    protected String generatePatronPassword() {
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
     * Retrieves the Aleph patron's ID based on name and birth date.
     * @param patron Patron's data
     * @return An Optional containing the Aleph patron's ID if found, or empty if not.
     */
    public Optional<String> getAlephPatronIdByNameAndBirth(Patron patron) {
        return oracleRepository.getPatronIdByNameAndBirth(patron.getName(), patron.getBirthDate());
    }

    /**
     * Checks if a patron exists in Aleph based on the name and birth date.
     * @param patron Patron's data
     * @return true if patron exists in Aleph, false otherwise
     */
    public boolean isNewAlephPatron(Patron patron) {
        boolean isNewInOracle = (oracleRepository.getPatronRowsCount(patron.getName(), patron.getBirthDate()) == 0);
        return isNewInOracle;
    }
}
