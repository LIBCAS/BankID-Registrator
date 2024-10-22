package cz.cas.lib.bankid_registrator.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.dao.mariadb.PatronRepository;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.AlephServiceIface;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.IdentityActivityService;
import cz.cas.lib.bankid_registrator.services.EmailService;
import cz.cas.lib.bankid_registrator.services.MainService;
import cz.cas.lib.bankid_registrator.services.MediaService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import cz.cas.lib.bankid_registrator.util.StringUtils;
import cz.cas.lib.bankid_registrator.validators.PatronDTOValidator;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class MainController extends ControllerAbstract
{
    private final MainConfiguration mainConfig;
    private final MainService mainService;
    private final AlephService alephService;
    private final AlephServiceIface envAlephService;
    private final ServletContext servletContext;
    private final PatronRepository patronRepository;
    private final PatronService patronService;
    private final PatronDTOValidator patronDTOValidator;
    private final MediaService mediaService;
    private final IdentityService identityService;
    private final IdentityActivityService identityActivityService;
    private final AccessTokenContainer accessTokenContainer;
    private final EmailService emailService;
    private final TokenService tokenService;

    public MainController(
        MessageSource messageSource,
        MainConfiguration mainConfig,
        MainService mainService,
        AlephService alephService,
        AlephServiceIface envAlephService,
        ServletContext servletContext,
        PatronRepository patronRepository,
        PatronService patronService,
        PatronDTOValidator patronDTOValidator,
        MediaService mediaService,
        IdentityService identityService,
        IdentityActivityService identityActivityService,
        AccessTokenContainer accessTokenContainer,
        EmailService emailService,
        TokenService tokenService
    ) {
        super(messageSource);
        this.mainConfig = mainConfig;
        this.mainService = mainService;
        this.alephService = alephService;
        this.envAlephService = envAlephService;
        this.servletContext = servletContext;
        this.patronRepository = patronRepository;
        this.patronDTOValidator = patronDTOValidator;
        this.patronService = patronService;
        this.mediaService = mediaService;
        this.identityService = identityService;
        this.identityActivityService = identityActivityService;
        this.accessTokenContainer = accessTokenContainer;
        this.emailService = emailService;
        this.tokenService = tokenService;

        init();
    }

    @RequestMapping(value="/", method=RequestMethod.GET)
    public String RootEntry(Locale locale) {
        return "redirect:/welcome";
    }

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
    public String WelcomeEntry(Model model, Locale locale) throws Exception {
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
        model.addAttribute("loginEndpoint", this.servletContext.getContextPath().concat("/login"));

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
    @RequestMapping(value = "/callback", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String CallbackEntry(
        @RequestParam(value = "code", required = false) String code, 
        Model model, 
        Locale locale, 
        HttpSession session
    ) {
        if (code == null) {
            model.addAttribute("error", this.messageSource.getMessage("error.session.expired", null, locale));
            return "error";
        }

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));

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

        Identity identity;

        // Mapping BankID user data to a Patron entity (so-called "BankId patron")
        Map<String, Object> bankIdPatronCreation = this.envAlephService.newPatron(userInfo, userProfile);

        if (bankIdPatronCreation.containsKey("error")) {
            model.addAttribute("error", "Registrace byla zamítnuta: " + (String) bankIdPatronCreation.get("error"));
            return "error";
        }

        Patron bankIdPatron = (Patron) bankIdPatronCreation.get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

        try {
            getLogger().info("patron: {}", bankIdPatron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting patron to JSON", e);
        }

        if (bankIdPatron.isNew()) {
            identity = new Identity(UUID.randomUUID().toString());
            identity.setCheckedByAdmin(false);
            this.identityService.save(identity);

            session.setAttribute("identity", identity.getId());
            this.identityActivityService.logBankIdVerificationSuccess(identity);
            this.identityActivityService.logNewRegistrationInitiation(identity);

            bankIdPatron = this.patronRepository.save(bankIdPatron);

            session.setAttribute("patron", bankIdPatron.getSysId());

            session.setAttribute("bankIdPatron", bankIdPatron.getSysId());
            session.setAttribute("bankIdPatronDTO", bankIdPatronDTO);

            model.addAttribute("patronId", bankIdPatron.getSysId());
            model.addAttribute("patron", bankIdPatronDTO);

            return "callback_registration_new";
        } else {
            String patronAlephId = bankIdPatron.getPatronId();

            if (patronAlephId == null) {
                getLogger().error("Patron exists in Aleph but has no Aleph ID");
                model.addAttribute("error", "Registrace byla zamítnuta: Chyba identifikace.");
                return "error";
            } else {
                Optional<Identity> identityByAlephId = this.identityService.findByAlephId(patronAlephId);

                if (identityByAlephId.isPresent()) {
                    identity = identityByAlephId.get();

                    if (identity.getBankId() == null) {
                        identity.setBankId(UUID.randomUUID().toString());
                        this.identityService.save(identity);
                        getLogger().debug("Identity found for Aleph ID: {} but has no BankIdSub. Assigned a new BankIdSub.", patronAlephId);
                    }
                } else {
                    identity = new Identity(UUID.randomUUID().toString());
                    identity.setAlephId(patronAlephId);
                    identity.setCheckedByAdmin(false);
                    this.identityService.save(identity);
                    getLogger().debug("Identity not found for Aleph ID: {}. Created a new identity.", patronAlephId);
                }
            }

            session.setAttribute("identity", identity.getId());
            this.identityActivityService.logBankIdVerificationSuccess(identity);
            this.identityActivityService.logMembershipRenewalInitiation(identity);

            // Mapping Aleph patron data to a Patron entity (so-called "Aleph patron")
            Map<String, Object> alephPatronCreation = this.alephService.getAlephPatron(patronAlephId, true);

            if (alephPatronCreation.containsKey("error")) {
                getLogger().error("Error getting patron from Aleph: {}", alephPatronCreation.get("error"));
                model.addAttribute("error", "Registrace byla zamítnuta: Chyba identifikace.");
                return "error";
            }

            Patron alephPatron = (Patron) alephPatronCreation.get("patron");
            PatronDTO alephPatronDTO = this.patronService.getPatronDTO(alephPatron);

            alephPatron = this.patronRepository.save(alephPatron);

            identity.setAlephBarcode(alephPatron.getBarcode());
            identity.setIsCasEmployee(alephPatron.getIsCasEmployee());
            identity.setUpdatedAt(LocalDateTime.now());
            this.identityService.save(identity);

            // Patron expiry date data from alephPatron
            String alephPatronExpiryDate = alephPatron.getExpiryDate();
            boolean membershipHasExpired = DateUtils.isDateExpired(alephPatronExpiryDate, "dd/MM/yyyy");
            boolean membershipExpiresToday = DateUtils.isDateToday(alephPatronExpiryDate, "dd/MM/yyyy");
            boolean expiryDateIn1MonthOrLess = DateUtils.isLessThanOrEqualToOneMonthFromToday(alephPatronExpiryDate, "dd/MM/yyyy");

            // Merging BankId patron and Aleph patron into a Patron with the latest data (so-called "the latest patron")
            Patron latestPatron = PatronService.mergePatrons(bankIdPatron, alephPatron);
            PatronDTO latestPatronDTO = this.patronService.getPatronDTO(latestPatron);

            latestPatron = this.patronRepository.save(latestPatron);

            try {
                getLogger().info("latestPatron: {}", latestPatron.toJson());
            } catch (JsonProcessingException e) {
                getLogger().error("Error converting latestPatron to JSON", e);
            }

            session.setAttribute("patron", latestPatron.getSysId());
            session.setAttribute("alephPatron", alephPatron.getSysId());

            session.setAttribute("latestPatron", latestPatron.getSysId());
            session.setAttribute("latestPatronDTO", latestPatronDTO);
            session.setAttribute("bankIdPatronDTO", bankIdPatronDTO);
            session.setAttribute("alephPatronDTO", alephPatronDTO);
            session.setAttribute("membershipExpiryDate", alephPatronExpiryDate);
            session.setAttribute("membershipHasExpired", membershipHasExpired);
            session.setAttribute("membershipExpiresToday", membershipExpiresToday);
            session.setAttribute("expiryDateIn1MonthOrLess", expiryDateIn1MonthOrLess);

            model.addAttribute("patronId", latestPatron.getSysId());
            model.addAttribute("patron", latestPatronDTO);
            model.addAttribute("bankIdPatron", bankIdPatronDTO);
            model.addAttribute("alephPatron", alephPatronDTO);
            model.addAttribute("membershipExpiryDate", alephPatronExpiryDate);
            model.addAttribute("membershipHasExpired", membershipHasExpired);
            model.addAttribute("membershipExpiresToday", membershipExpiresToday);
            model.addAttribute("expiryDateIn1MonthOrLess", expiryDateIn1MonthOrLess);

            return "callback_registration_renewal";
        }
    }

    /**
     * Creating new Aleph patron (new registration)
     * @param editedPatron - user-edited patron data
     * @param bindingResult - validation result
     * @param session
     * @param model
     * @param media
     * @return String
     */
    @PostMapping("/new-registration")
    public String newRegistrationEntry(
        @Valid @ModelAttribute PatronDTO editedPatron, 
        BindingResult bindingResult, 
        HttpSession session, 
        Model model, 
        Locale locale, 
        @RequestParam("media") MultipartFile[] mediaFiles
    ) {
        PatronDTO beforeEditedPatron = (PatronDTO) session.getAttribute("bankIdPatronDTO");
        if (beforeEditedPatron == null) {
            return "error_session_expired";
        }

        this.patronDTOValidator.validate(editedPatron, bindingResult, null, mediaFiles);

        if (bindingResult.hasErrors()) {
            editedPatron.restoreDefaults(beforeEditedPatron);

            model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
            model.addAttribute("code", session.getAttribute("code"));

            model.addAttribute("patronId", session.getAttribute("bankIdPatron"));
            model.addAttribute("patron", editedPatron);

            model.addAttribute("org.springframework.validation.BindingResult.patron", bindingResult);

            return "callback_registration_new";
        }

        // getLogger().info("Session ID: {}", session.getId());

        Long patronSysId = (Long) session.getAttribute("patron");
        Patron patron = patronRepository.findById(patronSysId).orElse(null);  // original patron data
        Identify userProfile = (Identify) session.getAttribute("userProfile");
        String code = (String) session.getAttribute("code");
        Identity identity = this.identityService.findById((Long) session.getAttribute("identity")).orElse(null);

        session.removeAttribute("patron");
        session.removeAttribute("userProfile");
        session.removeAttribute("code");
        session.removeAttribute("identity");
        session.removeAttribute("bankIdPatron");
        session.removeAttribute("bankIdPatronDTO");

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

        this.patronRepository.deleteById(patronSysId);

        patron.update(editedPatron);

        try {
            getLogger().info("new-registration - finalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting finalPatron to JSON", e);
        }

        synchronized (this) {
            Map<String, Object> patronCreation = this.alephService.createPatron(patron);
            if (patronCreation.containsKey("error")) {
                getLogger().info("RESULT: {}", patronCreation);
                getLogger().error("Error creating patron: {}", patronCreation.get("error"));
                return "error";
            }
            // model.addAttribute("xml", patronCreation.get("xml-patron"));
        }

        String alephPatronBarcode = patron.getBarcode();
        Boolean patronIsCasEmployee = patron.getIsCasEmployee();
        String patronEmail = patron.getEmail();
        boolean patronHasEmail = !StringUtils.isEmpty(patronEmail);

        identity.setAlephId(patron.getId());
        identity.setAlephBarcode(alephPatronBarcode);
        identity.setIsCasEmployee(patronIsCasEmployee);
        identity.setUpdatedAt(LocalDateTime.now());
        this.identityService.save(identity);

        if (patronIsCasEmployee) {
            int mediaFilesCount = 0;
            if (mediaFiles != null) {
                mediaFilesCount = (int) Arrays.stream(mediaFiles).filter(file -> file != null && !file.isEmpty()).count();

                boolean hasMediaFiles = mediaFiles != null && mediaFilesCount > 0;

                if (hasMediaFiles) {
                    for (MultipartFile file : mediaFiles) {
                        Map<String, Object> uploadResult = this.mediaService.uploadMedia(file, identity);
                        if (uploadResult.containsKey("error")) {
                            getLogger().error("Error uploading media file: {}", uploadResult.get("error"));
                        }
                    }
                }
            }
        }

        this.identityActivityService.logNewRegistrationSuccess(identity);

        String membershipExpiryDate = patron.getExpiryDate();

        try {
            if (patronHasEmail) {
                this.emailService.sendEmailNewRegistration(patronEmail, alephPatronBarcode, patronIsCasEmployee, membershipExpiryDate, locale);
                this.identityActivityService.logNewRegistrationEmailSent(identity);
            }
        } catch (Exception e) {
            getLogger().error("Failed to send new registration confirmation email to " + patronEmail, e);
        }

        session.setAttribute("alephBarcode", alephPatronBarcode);

        model.addAttribute("patronIsCasEmployee", patronIsCasEmployee);
        model.addAttribute("patronHasEmail", patronHasEmail);
        model.addAttribute("membershipExpiryDate", membershipExpiryDate);
        model.addAttribute("alephBarcode", alephPatronBarcode);
        model.addAttribute("token", this.tokenService.createIdentityToken(identity));
        model.addAttribute("apiToken", this.tokenService.createApiToken(identity.getId().toString()));
        model.addAttribute("patronLdapSynced", false);
        model.addAttribute("passwordDTO", new PatronPasswordDTO());

        return "new_registration_success";
    }

    /**
     * Updating Aleph patron (membership renewal)
     * @param editedPatron - user-edited patron data based on the latest patron data
     * @param bindingResult - validation result
     * @param session
     * @param model
     * @param media
     * @return
     */
    @PostMapping("/membership-renewal")
    public String membershipRenewalEntry(
        @Valid @ModelAttribute PatronDTO editedPatron, 
        BindingResult bindingResult, 
        HttpSession session, 
        Model model, 
        Locale locale, 
        @RequestParam("media") MultipartFile[] mediaFiles
    ) {
        Long alephPatronSysId = (Long) session.getAttribute("alephPatron");

        if (alephPatronSysId == null) {
            return "error_session_expired";
        }

        Patron alephPatron = patronRepository.findById(alephPatronSysId).orElse(null);  // original Aleph patron

        this.patronDTOValidator.validate(editedPatron, bindingResult, alephPatron.getId(), mediaFiles);

        if (bindingResult.hasErrors()) {
            PatronDTO beforeEditedPatron = (PatronDTO) session.getAttribute("latestPatronDTO");

            if (beforeEditedPatron == null) {
                return "error_session_expired";
            }

            editedPatron.restoreDefaults(beforeEditedPatron);

            model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
            model.addAttribute("code", session.getAttribute("code"));

            model.addAttribute("patronId", session.getAttribute("latestPatron"));
            model.addAttribute("patron", editedPatron);
            model.addAttribute("bankIdPatron", (PatronDTO) session.getAttribute("bankIdPatronDTO"));
            model.addAttribute("alephPatron", (PatronDTO) session.getAttribute("alephPatronDTO"));
            model.addAttribute("membershipExpiryDate", session.getAttribute("membershipExpiryDate"));
            model.addAttribute("membershipHasExpired", session.getAttribute("membershipHasExpired"));
            model.addAttribute("membershipExpiresToday", session.getAttribute("membershipExpiresToday"));
            model.addAttribute("expiryDateIn1MonthOrLess", session.getAttribute("expiryDateIn1MonthOrLess"));

            model.addAttribute("org.springframework.validation.BindingResult.patron", bindingResult);

            return "callback_registration_renewal";
        }

        Long patronSysId = (Long) session.getAttribute("patron");
        Patron patron = patronRepository.findById(patronSysId).orElse(null);    // Original Latest patron (i.e. patron created by merging BankId patron with Aleph patron)
        Identify userProfile = (Identify) session.getAttribute("userProfile");
        String code = (String) session.getAttribute("code");
        Identity identity = this.identityService.findById((Long) session.getAttribute("identity")).orElse(null);

        session.removeAttribute("alephPatron");
        session.removeAttribute("patron");
        session.removeAttribute("userProfile");
        session.removeAttribute("code");
        session.removeAttribute("identity");
        session.removeAttribute("latestPatron");
        session.removeAttribute("latestPatronDTO");
        session.removeAttribute("bankIdPatronDTO");
        session.removeAttribute("alephPatronDTO");
        session.removeAttribute("membershipExpiryDate");
        session.removeAttribute("membershipHasExpired");
        session.removeAttribute("membershipExpiresToday");
        session.removeAttribute("expiryDateIn1MonthOrLess");

        try {
            getLogger().info("membership-renewal - originalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting originalPatron to JSON", e);
        }
        try {
            getLogger().info("membership-renewal - submitted patron: {}", editedPatron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting submitted patron to JSON", e);
        }

        if (alephPatronSysId == null || alephPatron == null || patronSysId == null || patron == null || userProfile == null || code == null || identity == null) {
            return "error_session_expired";
        }

        if (editedPatron.getExportConsent() != PatronBoolean.Y) {
            return "error_export_consent";
        }

        this.identityActivityService.logMembershipRenewalSubmission(identity);

        this.patronRepository.deleteById(patronSysId);
        this.patronRepository.deleteById(alephPatronSysId);

        patron.update(editedPatron);

        try {
            getLogger().info("membership-renewal - finalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting finalPatron to JSON", e);
        }

        Map<String, Object> patronUpdate = this.alephService.updatePatron(patron, alephPatron);
        if (patronUpdate.containsKey("error")) {
            getLogger().info("RESULT: {}", patronUpdate);
            getLogger().error("Error updating patron: {}", patronUpdate.get("error"));
            return "error";
        }

        String alephPatronBarcode = patron.getBarcode();
        Boolean patronIsCasEmployee = patron.getIsCasEmployee();
        String patronEmail = patron.getEmail();
        boolean patronHasEmail = !StringUtils.isEmpty(patronEmail);

        identity.setIsCasEmployee(patronIsCasEmployee);
        identity.setCheckedByAdmin(false);
        identity.setUpdatedAt(LocalDateTime.now());
        this.identityService.save(identity);

        if (patronIsCasEmployee) {
            int mediaFilesCount = 0;
            if (mediaFiles != null) {
                mediaFilesCount = (int) Arrays.stream(mediaFiles).filter(file -> file != null && !file.isEmpty()).count();

                boolean hasMediaFiles = mediaFiles != null && mediaFilesCount > 0;

                if (hasMediaFiles) {
                    for (MultipartFile file : mediaFiles) {
                        Map<String, Object> uploadResult = this.mediaService.uploadMedia(file, identity);
                        if (uploadResult.containsKey("error")) {
                            getLogger().error("Error uploading media file: {}", uploadResult.get("error"));
                        }
                    }
                }
            }
        }

        this.identityActivityService.logMembershipRenewalSuccess(identity);

        String membershipExpiryDate = patron.getExpiryDate();

        try {
            if (patronHasEmail) {
                this.emailService.sendEmailMembershipRenewal(patronEmail, alephPatronBarcode, patronIsCasEmployee, membershipExpiryDate, locale);
                this.identityActivityService.logNewRegistrationEmailSent(identity);
            }
        } catch (Exception e) {
            getLogger().error("Failed to send a membership renewal confirmation email to " + patronEmail, e);
        }

        session.setAttribute("alephBarcode", alephPatronBarcode);

        model.addAttribute("patronIsCasEmployee", patronIsCasEmployee);
        model.addAttribute("patronHasEmail", patronHasEmail);
        model.addAttribute("membershipExpiryDate", membershipExpiryDate);
        // model.addAttribute("xml", patronUpdate.get("xml-patron"));
        model.addAttribute("alephBarcode", alephPatronBarcode);

        return "membership_renewal_success";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/guide", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String AboutEntry(Model model) {
        return "guide";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/tos", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String TermsOfServiceEntry(Model model) {
        return "terms_of_service";
    }

    /**
     * 
     * @param model
     * @return 
     */
    @RequestMapping(value="/privacy-policy", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String UsagePolicyEntry(Model model) {
        return "privacy_policy";
    }
}
