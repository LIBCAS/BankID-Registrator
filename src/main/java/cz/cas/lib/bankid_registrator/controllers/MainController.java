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
import cz.cas.lib.bankid_registrator.dao.mariadb.MariaDBRepository;
import cz.cas.lib.bankid_registrator.dto.PatronBoolean;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.model.patron_barcode.PatronBarcode;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.MainService;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;
import java.net.URI;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author iok
 */
@Controller
public class MainController extends MainControllerAbstract {

    private final MainConfiguration mainConfig;
    private final MainService mainService;
    private final AlephService alephService;
    private final ServletContext servletContext;
    private final MariaDBRepository mariaDBRepository;
    private final AccessTokenContainer accessTokenContainer;

    @NotEmpty
    @Value("${spring.application.name}")
    private String appName;

    public MainController(MainConfiguration mainConfig, MainService mainService, AlephService alephService, ServletContext servletContext, MariaDBRepository mariaDBRepository, AccessTokenContainer accessTokenContainer)
    {
        super();
        this.mainConfig = mainConfig;
        this.mainService = mainService;
        this.alephService = alephService;
        this.servletContext = servletContext;
        this.mariaDBRepository = mariaDBRepository;
        this.accessTokenContainer = accessTokenContainer;
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
     */
    @RequestMapping(value="/welcome", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String WelcomeEntry(Model model) {
        getLogger().info("ACCESSING WELCOME PAGE ...");
        model.addAttribute("appName", this.appName);
        model.addAttribute("loginEndpoint", this.servletContext.getContextPath().concat("/login"));

        // Map<String, Object> itemDeletion = this.alephService.deleteItem("002299434", "148670", "261100229943420240318");
        // if (itemDeletion.containsKey("error")) {
        //     logger.error("Error deleting item: {}", itemDeletion.get("error"));
        // }

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
        session.setAttribute("patron", patron);
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
     * @return String
     */
    @PostMapping("/new-registration")
    public String newRegistrationEntry(@ModelAttribute PatronDTO editedPatron, HttpSession session, Model model) {
        PatronDTO patron = (PatronDTO) session.getAttribute("patron");  // original patron data
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

        if (patron == null || userProfile == null || code == null) {
            return "error_session_expired";
        }

        if (editedPatron.getExportConsent() != PatronBoolean.Y) {
            return "error_export_consent";
        }

        patron.update(editedPatron);
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

        // jpa test - todo lock/synchronized
        PatronBarcode barcode = new PatronBarcode();
        String patronBarcode = patron.getBarcode();
        Long patronBarcodeNoPrefix = Long.valueOf(patronBarcode.substring(5));
        barcode.setSub(patron.getBankIdSub());
        barcode.setBarcode(mainConfig.getBarcode_prefix().concat(code));
        barcode.setBarcodeAleph(patronBarcodeNoPrefix);
        this.mariaDBRepository.save(barcode);

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

}
