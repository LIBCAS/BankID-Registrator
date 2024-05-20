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
package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.mariadb.PatronBarcodeRepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.PatronDTORepository;
import cz.cas.lib.bankid_registrator.dao.mariadb.MediaRepository;
import cz.cas.lib.bankid_registrator.dto.PatronBoolean;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.model.media.Media;
import cz.cas.lib.bankid_registrator.model.patron_barcode.PatronBarcode;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.EmailService;
import cz.cas.lib.bankid_registrator.services.MainService;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotEmpty;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author iok
 */
@Controller
public class MainController extends MainControllerAbstract
{
    private final MainConfiguration mainConfig;
    private final MainService mainService;
    private final AlephService alephService;
    private final ServletContext servletContext;
    private final PatronBarcodeRepository patronBarcodeRepository;
    private final PatronDTORepository patronDTORepository;
    private final MediaRepository mediaRepository;
    private final AccessTokenContainer accessTokenContainer;
    private final EmailService emailService;

    @NotEmpty
    @Value("${spring.application.name}")
    private String appName;

    public MainController(
        MainConfiguration mainConfig,
        MainService mainService,
        AlephService alephService,
        ServletContext servletContext, 
        PatronBarcodeRepository patronBarcodeRepository,
        PatronDTORepository patronDTORepository,
        MediaRepository mediaRepository,
        AccessTokenContainer accessTokenContainer,
        EmailService emailService
    ) {
        super();
        this.mainConfig = mainConfig;
        this.mainService = mainService;
        this.alephService = alephService;
        this.servletContext = servletContext;
        this.patronBarcodeRepository = patronBarcodeRepository;
        this.patronDTORepository = patronDTORepository;
        this.mediaRepository = mediaRepository;
        this.accessTokenContainer = accessTokenContainer;
        this.emailService = emailService;
        init();
    }

    @Override
    @RequestMapping(value="/", method=RequestMethod.GET)
    public String RootEntry() {
        return "redirect:/welcome";
    }

    @Override
    @RequestMapping(value="/index", method=RequestMethod.GET)
    public String IndexEntry() {
        return "redirect:/welcome";
    }

    /**
     * 
     * @param model
     * @return 
     * @throws Exception 
     */
    @RequestMapping(value="/welcome", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String WelcomeEntry(Model model) throws Exception {
        getLogger().info("ACCESSING WELCOME PAGE ...");
        model.addAttribute("appName", this.appName);
        model.addAttribute("loginEndpoint", this.servletContext.getContextPath().concat("/login"));

        return "welcome";
    }

    /**
     * 
     * @return 
     */
    @RequestMapping(value="/login", method=RequestMethod.GET)
    public String InitiateLoginEntry() {
        getLogger().info("ACCESSING LOGIN PAGE ...");
        StringBuilder strTmp = new StringBuilder(0);

        URI authorizationEndpoint = this.mainService.getBankIDAuthorizationEndpoint(this.mainConfig.getIssuer_url());
        getLogger().info("authorizationEndpoint: {}", authorizationEndpoint);
        if (authorizationEndpoint == null) {
            // redirect to error page
            return "error";
        }

        strTmp.append(authorizationEndpoint.toString().concat("?"));

        String loginURL = this.mainService.getBankIDLoginURL(authorizationEndpoint.toString());
        getLogger().info("loginURL: {}", loginURL);
        if (loginURL == null) {
            // redirect to error page
            return "error";
        }

        strTmp.append(loginURL);

        return "redirect:".concat(strTmp.toString());
    }

    /**
     * 
     * @param code
     * @param model
     * @param session
     * @return 
     */
    @RequestMapping(value="/callback", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String CallbackEntry(@RequestParam("code") String code, Model model, HttpSession session) {

        model.addAttribute("appName", appName);

        Assert.notNull(code, "\"code\" is required");

        if (!accessTokenContainer.getCodeTokenMap().containsKey(code)) {
            accessTokenContainer.setAccessToken(code, mainService.getTokenExchange(code).getAccessToken());
        } else {
            // TODO valid access token
        }

        session.setAttribute("code", code);
        model.addAttribute("code", code);
        
        Connect userInfo = mainService.getUserInfo(accessTokenContainer.getAccessToken(code));

        Identify userProfile = mainService.getProfile(accessTokenContainer.getAccessToken(code));
        session.setAttribute("userProfile", userProfile);

        if (userProfile.getLimited_legal_capacity()) {
            // TODO limited_legal_capacity => go out
            model.addAttribute("errorMessage", "Registrace byla zamítnuta: Osoba s omezenou způsobilostí k právním úkonům.");
            return "error";
        }

        // Mapping BankID user data to Aleph patron
        Map<String, Object> patronObjCreation = alephService.newPatronTest(userInfo, userProfile);

        if (patronObjCreation.containsKey("error")) {
            model.addAttribute("errorMessage", "Registrace byla zamítnuta: " + (String) patronObjCreation.get("error"));
            return "error";
        }

        PatronDTO patron = (PatronDTO) patronObjCreation.get("patron");
        patron = patronDTORepository.save(patron);
        session.setAttribute("patron", patron.getSysId());
        model.addAttribute("patron", patron);
        try {
            getLogger().info("patron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting patron to JSON", e);
        }

        // Remove this code after testing
        model.addAttribute("userInfo", userInfo);
        model.addAttribute("userProfile", userProfile);
        model.addAttribute("bankIDForSep", "https://developer.bankid.cz/docs/api/bankid-for-sep");

        return "callback";
    }

    /**
     * Creating new Aleph patron
     * @param editedPatron - user-edited patron data
     * @param session
     * @param model
     * @param media
     * @return String
     */
    @PostMapping("/new-registration")
    public String newRegistrationEntry(@ModelAttribute PatronDTO editedPatron, HttpSession session, Model model, @RequestParam("media") MultipartFile[] mediaFiles) {
        Long patronSysId = (Long) session.getAttribute("patron");
        PatronDTO patron = patronDTORepository.findById(patronSysId).orElse(null);  // original patron data
        Identify userProfile = (Identify) session.getAttribute("userProfile");
        String code = (String) session.getAttribute("code");

        try {
            getLogger().info("new-registration - originalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting originalPatron to JSON", e);
        }
        try {
            getLogger().info("new-registration - submitted patron: {}", editedPatron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting submitted patron to JSON", e);
        }
        getLogger().info("new-registration - userProfile: {}", userProfile);
        getLogger().info("new-registration - code: {}", code);

        if (patronSysId == null || patron == null || userProfile == null || code == null) {
            return "error_session_expired";
        }

        if (editedPatron.getExportConsent() != PatronBoolean.Y) {
            return "error_export_consent";
        }

        patron.update(editedPatron);
        if (patron.getPatronId() == null) {
            patron.setId(this.alephService.generatePatronId());
        }
        if (patron.getBarcode() == null) {
            patron.setBarcode(this.alephService.generatePatronBarcode());
        }
        try {
            getLogger().info("new-registration - finalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting finalPatron to JSON", e);
        }

        Map<String, Object> patronCreation = alephService.createPatron(patron);
        if (patronCreation.containsKey("error")) {
            getLogger().info("RESULT: {}", patronCreation);
            getLogger().error("Error creating patron: {}", patronCreation.get("error"));
            return "error";
        }

        model.addAttribute("xml", patronCreation.get("xml-patron"));
        getLogger().info("Trying saving patron barcode");
        try {
            PatronBarcode barcode = new PatronBarcode();
            String patronBarcode = patron.getBarcode();
            Long patronBarcodeNoPrefix = Long.valueOf(patronBarcode.substring(5));
            barcode.setSub(patron.getBankIdSub());
            barcode.setBarcode(mainConfig.getBarcode_prefix().concat(code));
            barcode.setBarcodeAleph(patronBarcodeNoPrefix);
            this.patronBarcodeRepository.save(barcode);
            getLogger().info("Successful patron barcode saving");
        } catch (Exception e) {
            getLogger().error("Error saving patron barcode", e);
        }

        if (patron.isCasEmployee) {
            this.patronDTORepository.save(patron);
            for (MultipartFile file : mediaFiles) {
                Map<String, Object> uploadResult = this.uploadMedia(file, patronSysId);
                if (uploadResult.containsKey("error")) {
                    getLogger().error("Error uploading media file: {}", uploadResult.get("error"));
                }
            }
        }

        return "new_registration_success";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/tos", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String TermsOfServiceEntry(Model model) {

        model.addAttribute("appName", this.appName);

        return "terms_of_service";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/privacy-policy", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String UsagePolicyEntry(Model model) {

        model.addAttribute("appName", this.appName);

        return "data_usage_policy";
    }

    /**
     * Upload a media file
     * @param file
     * @param patronSysId
     * @return Map<String, Object>
     */
    public Map<String, Object> uploadMedia(MultipartFile file, Long patronSysId)
    {getLogger().info("uploadMedia - patronSysId: {}", patronSysId);
        Map<String, Object> result = new HashMap<>();

        String contentType = file.getContentType(); getLogger().info("QAZWSX - contentType: {}", contentType);
        String fileName = file.getOriginalFilename(); getLogger().info("QAZWSX - fileName: {}", fileName);

        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("application/pdf")) {
            result.put("error", "Unsupported file type: " + contentType);
            return result;
        }

        PatronDTO patron = patronDTORepository.findById(patronSysId).orElse(null);
        if (patron == null) {
            result.put("error", "User not found.");
            return result;
        }

        Path path = Paths.get(mainConfig.getStorage_path(), fileName);
        try {
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            result.put("error", "Failed to save file: " + e.getMessage());
            return result;
        }

        Media media = new Media();
        media.setName(fileName);
        media.setType(contentType);
        media.setPath(path.toString());
        media.setPatronDTO(patron);

        if (mediaRepository.save(media) != null) {
            result.put("success", Boolean.TRUE);
        } else {
            result.put("error", "Error uploading media file " + fileName + ".");
        }

        return result;
    }
}
