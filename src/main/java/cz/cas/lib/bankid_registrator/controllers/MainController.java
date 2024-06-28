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
import cz.cas.lib.bankid_registrator.dao.mariadb.PatronRepository;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.IdentityActivityService;
import cz.cas.lib.bankid_registrator.services.EmailService;
import cz.cas.lib.bankid_registrator.services.MainService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import cz.cas.lib.bankid_registrator.services.MediaService;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.MediaType;
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
    private final PatronRepository patronRepository;
    private final PatronService patronService;
    private final MediaService mediaService;
    private final IdentityService identityService;
    private final IdentityActivityService identityActivityService;
    private final AccessTokenContainer accessTokenContainer;
    private final EmailService emailService;
    private final MessageSource messageSource;

    @NotEmpty
    @Value("${spring.application.name}")
    private String appName;

    public MainController(
        MainConfiguration mainConfig,
        MainService mainService,
        AlephService alephService,
        ServletContext servletContext,
        PatronRepository patronRepository,
        PatronService patronService,
        MediaService mediaService,
        IdentityService identityService,
        IdentityActivityService identityActivityService,
        AccessTokenContainer accessTokenContainer,
        EmailService emailService,
        MessageSource messageSource
    ) {
        super();
        this.mainConfig = mainConfig;
        this.mainService = mainService;
        this.alephService = alephService;
        this.servletContext = servletContext;
        this.patronRepository = patronRepository;
        this.patronService = patronService;
        this.mediaService = mediaService;
        this.identityService = identityService;
        this.identityActivityService = identityActivityService;
        this.accessTokenContainer = accessTokenContainer;
        this.emailService = emailService;
        this.messageSource = messageSource;
        init();
    }

    @Override
    @RequestMapping(value="/", method=RequestMethod.GET)
    public String RootEntry(Locale locale) {
        return "redirect:/welcome";
    }

    @Override
    @RequestMapping(value="/index", method=RequestMethod.GET)
    public String IndexEntry(Locale locale) {
        return "redirect:/welcome";
    }

    /**
     * 
     * @param model
     * @return 
     * @throws Exception 
     */
    @RequestMapping(value="/welcome", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String WelcomeEntry(Model model, Locale locale, HttpSession session) throws Exception {
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
        model.addAttribute("appName", this.appName);
        model.addAttribute("loginEndpoint", this.servletContext.getContextPath().concat("/login"));
        model.addAttribute("lang", locale.getLanguage());

        return "welcome";
    }

    /**
     * 
     * @return 
     */
    @RequestMapping(value="/login", method=RequestMethod.GET)
    public String InitiateLoginEntry(Locale locale) {
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
    public String CallbackEntry(@RequestParam("code") String code, Model model, Locale locale, HttpSession session)
    {
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
        model.addAttribute("appName", this.appName);
        model.addAttribute("lang", locale.getLanguage());

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
            model.addAttribute("error", "Registrace byla zamítnuta: Osoba s omezenou způsobilostí k právním úkonům.");
            return "error";
        }

        String bankId = userInfo.getSub();
        Identity identity;

        Optional<Identity> identitySearch = this.identityService.findByBankId(bankId);
        if (!identitySearch.isPresent()) {
            identity = new Identity(bankId);
            this.identityService.save(identity);
            session.setAttribute("identity", identity.getId());
        } else {
            identity = identitySearch.get();
        }

        this.identityActivityService.logBankIdVerificationSuccess(identity);

        // Mapping BankID user data to a Patron entity (so-called "BankId patron")
        Map<String, Object> patronObjCreation = this.alephService.newPatronTest(userInfo, userProfile);

        if (patronObjCreation.containsKey("error")) {
            model.addAttribute("error", "Registrace byla zamítnuta: " + (String) patronObjCreation.get("error"));
            return "error";
        }

        Patron patron = (Patron) patronObjCreation.get("patron");
        patron = patronRepository.save(patron);
        session.setAttribute("patron", patron.getSysId());
        model.addAttribute("patronId", patron.getSysId());
        model.addAttribute("patron", this.patronService.getPatronDTO(patron));

        try {
            getLogger().info("patron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting patron to JSON", e);
        }

        if (patron.isNew()) {
            this.identityActivityService.logNewRegistrationInitiation(identity);

            return "callback_registration_new";
        } else {
            Map<String, Object> patronAlephObjCreation = this.alephService.getAlephPatron(patron.getId());

            if (patronAlephObjCreation.containsKey("error")) {
                getLogger().error("Error getting patron from Aleph: {}", patronAlephObjCreation.get("error"));
                model.addAttribute("error", "Registrace byla zamítnuta: " + (String) patronAlephObjCreation.get("error"));
                return "error";
            }

            Patron patronAleph = (Patron) patronAlephObjCreation.get("patron");
            patronAleph = patronRepository.save(patronAleph);
            session.setAttribute("patronAleph", patronAleph.getSysId());
            model.addAttribute("patronAleph", this.patronService.getPatronDTO(patronAleph));

            try {
                getLogger().info("patronAleph: {}", patronAleph.toJson());
            } catch (JsonProcessingException e) {
                getLogger().error("Error converting patronAleph to JSON", e);
            }

            this.identityActivityService.logMembershipRenewalInitiation(identity);

            return "callback_registration_renewal";
        }
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
        Patron patron = patronRepository.findById(patronSysId).orElse(null);  // original patron data
        Identify userProfile = (Identify) session.getAttribute("userProfile");
        String code = (String) session.getAttribute("code");
        Identity identity = this.identityService.findById((Long) session.getAttribute("identity")).orElse(null);

        this.patronRepository.deleteById(patronSysId);

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

        if (patronSysId == null || patron == null || userProfile == null || code == null || identity == null) {
            return "error_session_expired";
        }

        if (editedPatron.getExportConsent() != PatronBoolean.Y) {
            return "error_export_consent";
        }

        this.identityActivityService.logNewRegistrationSubmission(identity);

        patron.update(editedPatron);

        try {
            getLogger().info("new-registration - finalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting finalPatron to JSON", e);
        }

        Map<String, Object> patronCreation = this.alephService.createPatron(patron);
        if (patronCreation.containsKey("error")) {
            getLogger().info("RESULT: {}", patronCreation);
            getLogger().error("Error creating patron: {}", patronCreation.get("error"));
            return "error";
        }

        identity.setAlephId(patron.getId());
        identity.setAlephBarcode(patron.getBarcode());
        identity.setUpdatedAt(LocalDateTime.now());
        this.identityService.save(identity);

        if (patron.isCasEmployee) {
            for (MultipartFile file : mediaFiles) {
                Map<String, Object> uploadResult = this.mediaService.uploadMedia(file, identity);
                if (uploadResult.containsKey("error")) {
                    getLogger().error("Error uploading media file: {}", uploadResult.get("error"));
                }
            }
        }

        this.identityActivityService.logNewRegistrationSuccess(identity);

        try {
            this.emailService.sendEmailNewRegistration(patron.getEmail(), patron.getId());
            this.identityActivityService.logNewRegistrationEmailSent(identity);
        } catch (Exception e) {
            getLogger().error("Error sending email", e);
        }

        session.setAttribute("alephBarcode", patron.getBarcode());

        model.addAttribute("xml", patronCreation.get("xml-patron"));
        model.addAttribute("alephBarcode", patron.getBarcode());

        return "new_registration_success";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/guide", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String AboutEntry(Model model) {

        model.addAttribute("appName", this.appName);

        return "guide";
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

        return "privacy_policy";
    }
}
