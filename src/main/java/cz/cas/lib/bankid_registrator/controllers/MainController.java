package cz.cas.lib.bankid_registrator.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;

import cz.cas.lib.bankid_registrator.configurations.MainConfiguration;
import cz.cas.lib.bankid_registrator.services.RegistrationFeeService;
import cz.cas.lib.bankid_registrator.configurations.SessionTimerConfig;
import cz.cas.lib.bankid_registrator.dao.mariadb.PatronRepository;
import cz.cas.lib.bankid_registrator.dto.AlertDTO;
import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import cz.cas.lib.bankid_registrator.entities.media.MediaSubmissionType;
import cz.cas.lib.bankid_registrator.entities.patron.PatronBoolean;
import cz.cas.lib.bankid_registrator.entities.patron.PatronLanguage;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
import cz.cas.lib.bankid_registrator.exceptions.IdentityAuthException;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import cz.cas.lib.bankid_registrator.model.voucher.Voucher;
import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.AppSettingsService;
import cz.cas.lib.bankid_registrator.services.AlephServiceIface;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.IdentityActivityService;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import cz.cas.lib.bankid_registrator.services.EmailService;
import cz.cas.lib.bankid_registrator.services.MainService;
import cz.cas.lib.bankid_registrator.services.MediaService;
import cz.cas.lib.bankid_registrator.services.PatronService;
import cz.cas.lib.bankid_registrator.services.PaymentService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import cz.cas.lib.bankid_registrator.services.VoucherService;
import cz.cas.lib.bankid_registrator.services.TestSettingsService;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;
import cz.cas.lib.bankid_registrator.util.DateUtils;
import cz.cas.lib.bankid_registrator.util.SessionSubmissionGuard;
import cz.cas.lib.bankid_registrator.util.StringUtils;
import cz.cas.lib.bankid_registrator.validators.PatronDTOValidator;

import java.net.URI;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
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
    private final AppSettingsService appSettingsService;
    private final TokenService tokenService;
    private final PaymentService paymentService;
    private final VoucherService voucherService;
    private final RegistrationFeeService registrationFeeService;

    @Autowired(required = false)
    private TestSettingsService testSettingsService;

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
        IdentityAuthService identityAuthService,
        AccessTokenContainer accessTokenContainer,
        EmailService emailService,
        AppSettingsService appSettingsService,
        TokenService tokenService,
        PaymentService paymentService,
        VoucherService voucherService,
        RegistrationFeeService registrationFeeService,
        SessionTimerConfig sessionTimerConfig
    ) {
        super(messageSource, identityAuthService, sessionTimerConfig);
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
        this.appSettingsService = appSettingsService;
        this.tokenService = tokenService;
        this.paymentService = paymentService;
        this.voucherService = voucherService;
        this.registrationFeeService = registrationFeeService;

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
     * Main page
     * 
     * @param session
     * @param model
     * @param locale
     * @param request
     * @return String
     * @throws Exception
     */
    @RequestMapping(value="/welcome", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String WelcomeEntry(
        @RequestParam(value = "session", required = false) String session,
        Model model,
        Locale locale,
        HttpServletRequest request
    ) throws Exception {
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
        model.addAttribute("loginEndpoint", this.servletContext.getContextPath().concat("/login"));

        if ("expired".equals(session) && !this.identityAuthService.isLoggedin(request)) {
            String alertMsg = this.messageSource.getMessage("alert.sessionExpired", null, locale);
            AlertDTO alert = new AlertDTO(alertMsg, "warning", 0);
            model.addAttribute("alert", alert);
        }

        return "welcome";
    }

    /**
     * 
     * @return 
     */
    @RequestMapping(value="/login", method=RequestMethod.GET)
    public String InitiateLoginEntry(Locale locale, HttpSession session, HttpServletRequest request)
    {
        if (!this.identityAuthService.isLoggedin(request)) {
            StringBuilder strTmp = new StringBuilder(0);

            URI authorizationEndpoint = this.mainService.getBankIDAuthorizationEndpoint(this.mainConfig.getIssuer_url());
            getLogger().info("authorizationEndpoint: {}", authorizationEndpoint);
            if (authorizationEndpoint == null) {
                return "error";
            }

            strTmp.append(authorizationEndpoint.toString().concat("?"));

            String loginURL = this.mainService.getBankIDLoginURL(authorizationEndpoint.toString());
            getLogger().info("loginURL: {}", loginURL);
            if (loginURL == null) {
                return "error";
            }

            strTmp.append(loginURL);

            return "redirect:".concat(strTmp.toString());
        } else {
            return "redirect:/callback?code=" + session.getAttribute("code");
        }
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
        @RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "error_description", required = false) String errorDescription,
        @RequestParam(value = "traceId", required = false) String traceId,
        @RequestParam(value = "state", required = false) String state,
        Model model, 
        Locale locale, 
        HttpSession session, 
        HttpServletRequest request
    ) {
        if (error != null || errorDescription != null) {
            return handleBankIdCbError(error, errorDescription, traceId, state, model, locale, session, request);
        }

        if (code == null && session.getAttribute("code") == null) {
            throw new HttpErrorException(HttpStatus.NOT_FOUND, null);
        }

        code = code == null ? ((String) session.getAttribute("code")) : code;

        boolean isIdentityLoggedIn = this.identityAuthService.isLoggedin(request);

        if (!isIdentityLoggedIn) {
            // If this is the first time the Bank iD verified identity is accessing the callback page, log them in
            try {
                this.identityAuthService.login(request, code, locale);
            } catch (IdentityAuthException e) {
                if (isInvalidGrantTokenExchange(e)) {
                    return handleBankIdCbError(
                        "invalid_grant",
                        "Authorization code expired or already used",
                        null,
                        state,
                        model,
                        locale,
                        session,
                        request
                    );
                }

                throw e;
            }

            // addCommonAttributes() ran before login, so the session timer config was not set.
            // Set it now so the timer renders correctly on the callback page.
            Long loginTimestamp = (Long) session.getAttribute("loginTimestamp");
            model.addAttribute("sessionTimerInactivityTimeout", this.sessionTimerConfig.getInactivityTimeoutSeconds());
            model.addAttribute("sessionTimerHardCap", this.sessionTimerConfig.getHardCapSeconds());
            model.addAttribute("sessionTimerWarningBefore", this.sessionTimerConfig.getWarningBeforeSeconds());
            model.addAttribute("sessionTimerKeepAliveInterval", this.sessionTimerConfig.getKeepAliveIntervalSeconds());
            model.addAttribute("sessionTimerLoginTimestamp", loginTimestamp);
        } else {
            // If the Bank iD verified identity is already logged in, check if the access token is still valid and log them out if not
            String existingAccessToken = (String) session.getAttribute("accessToken");
            if (!this.mainService.isTokenValid(existingAccessToken)) {
                this.identityAuthService.logout(request);
                return "redirect:/welcome";
            }
        }

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.welcome.title", null, locale));
        
        Connect userInfo = mainService.getUserInfo(accessTokenContainer.getAccessToken(code));

        Identify userProfile = mainService.getProfile(accessTokenContainer.getAccessToken(code));
        session.setAttribute("userProfile", userProfile);

        Identity identity;

        // Mapping BankID user data to a Patron entity (so-called "BankId patron")
        Map<String, Object> bankIdPatronCreation = this.envAlephService.newPatron(userInfo, userProfile);

        if (bankIdPatronCreation.containsKey("error")) {
            this.identityAuthService.logout(request);
            model.addAttribute("error", "Registrace byla zamítnuta: " + (String) bankIdPatronCreation.get("error"));
            return "error";
        }

        Patron bankIdPatron = (Patron) bankIdPatronCreation.get("patron");
        PatronDTO bankIdPatronDTO = this.patronService.getPatronDTO(bankIdPatron);

        // Setting bankIdPatronDTO's conLng to the current locale
        bankIdPatronDTO.setConLng(locale.getLanguage().equals("en") ? PatronLanguage.ENG : PatronLanguage.CZE);

        try {
            getLogger().info("patron: {}", bankIdPatron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting patron to JSON", e);
        }

        model.addAttribute("isIdentityLoggedIn", true);

        if (bankIdPatron.isNew()) {
            identity = new Identity(UUID.randomUUID().toString());
            identity.setCheckedByAdmin(false);
            identity.setPasswordSet(false);
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
                this.identityAuthService.logout(request);
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

            // Check if the returning identity has completed the initial password-setting step.
            // Show the password form only when passwordSet is explicitly FALSE
            // (i.e. a new registration that hasn't set the password yet).
            // null = legacy/pre-existing identity → treat as already set (skip form).
            // true = password was set → skip form.
            if (Boolean.FALSE.equals(identity.getPasswordSet())) {
                getLogger().info("Returning user has not set their password yet. Showing password-setting form. Identity: {}", identity.getId());

                Boolean patronIsCasEmployee = identity.getIsCasEmployee() != null ? identity.getIsCasEmployee() : false;

                model.addAttribute("token", this.tokenService.createIdentityToken(identity));
                model.addAttribute("passwordDTO", new PatronPasswordDTO());
                model.addAttribute("patronIsCasEmployee", patronIsCasEmployee);
                model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordSetting.title", null, locale));

                return "identity_set_password";
            }

            // Check if the user has an unpaid payment from a previous session
            // (e.g. they verified via BankID yesterday but didn't complete the payment)
            Optional<Payment> existingPayment = this.paymentService.getLatestPaymentByIdentity(identity);
            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                PaymentStatus paymentStatus = payment.getStatus();

                if (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.FAILED) {
                    // Verify the patron still has outstanding fees in Aleph
                    boolean stillHasOutstandingFees = !this.paymentService.verifyPaymentStatus(identity.getAlephId());

                    if (stillHasOutstandingFees) {
                        getLogger().info("Returning user with unpaid {} payment. Redirecting to payment page. Identity: {}, PaymentId: {}",
                            payment.getType(), identity.getId(), payment.getId());
                        // Redirect to the payment page which shows username, fee, voucher field, pay button
                        return "redirect:/payment";
                    } else {
                        // Fee was paid through other means (e.g. library catalog) — mark payment as SUCCESS
                        getLogger().info("Outstanding fee already paid (via other means). Marking payment as SUCCESS. Identity: {}, PaymentId: {}",
                            identity.getId(), payment.getId());
                        this.paymentService.updatePaymentStatus(payment, PaymentStatus.SUCCESS);

                        // Confirm voucher usage if a voucher was applied to this payment
                        if (payment.getVoucherCode() != null && !payment.getVoucherCode().isEmpty()) {
                            this.voucherService.findByCode(payment.getVoucherCode()).ifPresent(voucher ->
                                this.voucherService.confirmOrCreateUsage(voucher, identity, payment.getDiscountAmount()));
                            getLogger().info("Confirmed voucher usage for externally paid payment. Voucher: {}, Identity: {}",
                                payment.getVoucherCode(), identity.getId());
                        }
                    }
                }
            }

            // Mapping Aleph patron data to a Patron entity (so-called "Aleph patron")
            Map<String, Object> alephPatronCreation = this.alephService.getAlephPatron(patronAlephId, true);

            if (alephPatronCreation.containsKey("error")) {
                this.identityAuthService.logout(request);
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
            BigDecimal outstandingFinesAmount = paymentService.getPatronTotalDueCash(identity.getAlephId());

            // Tester's Toolkit: override expiryDateIn1MonthOrLess when forceRenewal is enabled (local/testing profiles only)
            if (this.testSettingsService != null && this.testSettingsService.isForceRenewal()) {
                expiryDateIn1MonthOrLess = true;
                getLogger().info("Tester's Toolkit: forceRenewal is ON — overriding expiryDateIn1MonthOrLess to true");
            }

            // Merging BankId patron and Aleph patron into a Patron with the latest data (so-called "the latest patron")
            Patron latestPatron = PatronService.mergePatrons(bankIdPatron, alephPatron);
            PatronDTO latestPatronDTO = this.patronService.getPatronDTO(latestPatron);

            latestPatron = this.patronRepository.save(latestPatron);

            // Setting latestPatron's conLng to the current locale
            latestPatron.setConLng(locale.getLanguage().equals("en") ? PatronLanguage.ENG : PatronLanguage.CZE);

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
            session.setAttribute("outstandingFinesAmount", outstandingFinesAmount);

            model.addAttribute("patronId", latestPatron.getSysId());
            model.addAttribute("patron", latestPatronDTO);
            model.addAttribute("bankIdPatron", bankIdPatronDTO);
            model.addAttribute("alephPatron", alephPatronDTO);
            model.addAttribute("membershipExpiryDate", alephPatronExpiryDate);
            model.addAttribute("membershipHasExpired", membershipHasExpired);
            model.addAttribute("membershipExpiresToday", membershipExpiresToday);
            model.addAttribute("expiryDateIn1MonthOrLess", expiryDateIn1MonthOrLess);
            model.addAttribute("outstandingFinesAmount", outstandingFinesAmount);
            model.addAttribute("standardRenewalFeeAmount", registrationFeeService.getRenewalFee(alephPatron));

            return "callback_registration_renewal";
        }
    }

    private String handleBankIdCbError(
        String error,
        String errorDescription,
        String traceId,
        String state,
        Model model,
        Locale locale,
        HttpSession session,
        HttpServletRequest request
    ) {
        String normalizedError = error != null ? error.trim() : "";
        String normalizedErrorDescription = errorDescription != null ? errorDescription.trim() : "";
        String messageKeySuffix = resolveBankIdCbErrorMessageKeySuffix(normalizedError, normalizedErrorDescription);
        String supportTicketId = StringUtils.generateSupportTicketId();
        String customerLanguage = locale.getLanguage();
        String pageTitle = this.messageSource.getMessage("page.bankidCbError.title", null, locale);

        getLogger().warn(
            "{} >>> supportTicketId={}, customerLanguage={}, error={}, errorDescription={}, traceId={}, statePresent={}, sessionCodePresent={}, sessionId={}, remoteAddr={}",
            pageTitle,
            supportTicketId,
            customerLanguage,
            normalizedError,
            normalizedErrorDescription,
            traceId,
            state != null && !state.isBlank(),
            session.getAttribute("code") != null,
            session.getId(),
            request.getRemoteAddr()
        );

        sendBankIdCbErrorSupportNotification(
            pageTitle,
            supportTicketId,
            customerLanguage,
            normalizedError,
            normalizedErrorDescription,
            traceId,
            state,
            session,
            request
        );

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("loginEndpoint", this.servletContext.getContextPath().concat("/login"));
        model.addAttribute(
            "bankidCbErrorTitle",
            this.messageSource.getMessage("page.bankidCbError." + messageKeySuffix + ".title", null, locale)
        );
        model.addAttribute(
            "bankidCbErrorDescription",
            this.messageSource.getMessage("page.bankidCbError." + messageKeySuffix + ".description", null, locale)
        );
        this.appSettingsService.getPrimarySupportEmail().ifPresent(primarySupportEmail -> {
            model.addAttribute("bankidCbErrorPrimarySupportEmail", primarySupportEmail.getEmail());
            model.addAttribute("bankidCbErrorSupportTicketId", supportTicketId);
        });

        return "bankid_cb_error";
    }

    private void sendBankIdCbErrorSupportNotification(
        String pageTitle,
        String supportTicketId,
        String customerLanguage,
        String error,
        String errorDescription,
        String traceId,
        String state,
        HttpSession session,
        HttpServletRequest request
    ) {
        List<String> supportEmails = this.appSettingsService.getSupportEmails().stream()
            .map(supportEmail -> supportEmail.getEmail())
            .collect(Collectors.toList());

        if (supportEmails.isEmpty()) {
            return;
        }

        String subject = pageTitle + " - ref. ID: " + supportTicketId;
        String body = String.format(
            "%s%n%n"
                + "supportTicketId=%s%n"
                + "customerLanguage=%s%n"
                + "error=%s%n"
                + "errorDescription=%s%n"
                + "traceId=%s%n"
                + "statePresent=%s%n"
                + "sessionCodePresent=%s%n"
                + "sessionId=%s%n"
                + "remoteAddr=%s",
            pageTitle,
            supportTicketId,
            customerLanguage,
            error,
            errorDescription,
            traceId,
            state != null && !state.isBlank(),
            session.getAttribute("code") != null,
            session.getId(),
            request.getRemoteAddr()
        );

        try {
            this.emailService.sendPlainTextEmail(supportEmails, subject, body);
        } catch (Exception e) {
            getLogger().error("Failed to send Bank iD callback error support notification email", e);
        }
    }

    private String resolveBankIdCbErrorMessageKeySuffix(String error, String errorDescription) {
        switch (error) {
            case "access_denied":
                if ("User declined the authentication".equals(errorDescription)) {
                    return "accessDeniedAuthentication";
                }
                if ("User declined consent".equals(errorDescription)) {
                    return "accessDeniedConsent";
                }
                return "accessDenied";

            case "eid_doesnt_exist":
                if ("User not eligible".equals(errorDescription)) {
                    return "eidDoesntExistNotEligible";
                }
                if ("User disabled authentication".equals(errorDescription)) {
                    return "eidDoesntExistDisabledAuthentication";
                }
                return "eidDoesntExist";

            case "user_not_eligible":
                if ("User authentication method insufficient".equals(errorDescription)) {
                    return "userNotEligibleMethodInsufficient";
                }
                if ("Insufficient user data or age restriction".equals(errorDescription)) {
                    return "userNotEligibleDataOrAge";
                }
                return "userNotEligible";

            case "invalid_grant":
                return "invalidGrant";

            default:
                return "default";
        }
    }

    private boolean isInvalidGrantTokenExchange(IdentityAuthException e) {
        Throwable cause = e.getCause();

        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("\"invalid_grant\"")) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    /**
     * Creating new Aleph patron (new registration)
     * @param editedPatron - user-edited patron data
     * @param bindingResult - validation result
     * @param session
     * @param model
     * @param media
     * @param locale
     * @param request
     * @return String
     */
    @PostMapping("/new-registration")
    public String newRegistrationEntry(
        @Valid @ModelAttribute PatronDTO editedPatron, 
        BindingResult bindingResult, 
        HttpSession session, 
        Model model, 
        Locale locale, 
        @RequestParam("media") MultipartFile[] mediaFiles, 
        HttpServletRequest request
    ) {
        if (!this.identityAuthService.isLoggedin(request)) {
            throw new HttpErrorException(
                HttpStatus.UNAUTHORIZED, 
                this.messageSource.getMessage("error.identity.notLoggedIn", null, locale)
            );
        }

        PatronDTO beforeEditedPatron = (PatronDTO) session.getAttribute("bankIdPatronDTO");
        if (beforeEditedPatron == null) {
            this.identityAuthService.logout(request);
            throw new HttpErrorException(
                HttpStatus.BAD_REQUEST, 
                this.messageSource.getMessage("error.400.text", null, locale)
            );
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

        String code = (String) session.getAttribute("code");
        String sessionSubmissionKey = "new-registration:" + code;
        if (code == null || !SessionSubmissionGuard.claim(session, sessionSubmissionKey)) {
            throw duplicateSubmission(locale);
        }

        // getLogger().info("Session ID: {}", session.getId());

        Long patronSysId = (Long) session.getAttribute("patron");
        Patron patron = patronRepository.findById(patronSysId).orElse(null);  // original patron data
        Identify userProfile = (Identify) session.getAttribute("userProfile");
        Identity identity = this.identityService.findById((Long) session.getAttribute("identity")).orElse(null);
        String durableSubmissionKey = identity == null ? null : "submission:new-registration:" + identity.getId();

        if (durableSubmissionKey == null || !tryClaimDurably(session, sessionSubmissionKey, durableSubmissionKey)) {
            throw duplicateSubmission(locale);
        }

        session.removeAttribute("patron");
        session.removeAttribute("userProfile");
        // session.removeAttribute("identity");
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
            this.identityAuthService.logout(request);
            throw new HttpErrorException(
                HttpStatus.BAD_REQUEST, 
                this.messageSource.getMessage("error.400.text", null, locale)
            );
        }

        if (editedPatron.getExportConsent() != PatronBoolean.Y) {
            this.identityAuthService.logout(request);
            throw new HttpErrorException(
                HttpStatus.BAD_REQUEST, 
                this.messageSource.getMessage("error.400.text", null, locale)
            );
        }

        this.identityActivityService.logNewRegistrationSubmission(identity);

        this.patronRepository.deleteById(patronSysId);

        patron.update(editedPatron);
        patron.setStatus(this.patronService.determinePatronStatus(patron).getId());
        patron.setExpiryDate(this.patronService.determinePatronExpiryDate(patron));

        try {
            getLogger().info("new-registration - finalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting finalPatron to JSON", e);
        }

        synchronized (this) {
            Map<String, Object> patronCreation = this.alephService.createPatron(patron);
            if (patronCreation.containsKey("error")) {
                this.tokenService.releaseClaimKey(durableSubmissionKey);
                this.identityAuthService.logout(request);
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
                    String submissionBatchId = this.mediaService.createSubmissionBatchId();
                    int batchOrderIndex = 1;
                    for (MultipartFile file : mediaFiles) {
                        if (file == null || file.isEmpty()) {
                            continue;
                        }

                        Map<String, Object> uploadResult = this.mediaService.uploadMedia(
                            file,
                            identity,
                            MediaSubmissionType.REGISTRATION,
                            submissionBatchId,
                            batchOrderIndex++
                        );
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

        model.addAttribute("patronIsCasEmployee", patronIsCasEmployee);
        model.addAttribute("patronHasEmail", patronHasEmail);
        model.addAttribute("membershipExpiryDate", membershipExpiryDate);
        model.addAttribute("alephBarcode", alephPatronBarcode);
        model.addAttribute("token", this.tokenService.createIdentityToken(identity));
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
     * @param locale
     * @param request
     * @return
     */
    @PostMapping("/membership-renewal")
    public String membershipRenewalEntry(
        @Valid @ModelAttribute PatronDTO editedPatron, 
        BindingResult bindingResult, 
        HttpSession session, 
        Model model, 
        Locale locale, 
        @RequestParam("media") MultipartFile[] mediaFiles, 
        @RequestParam(value = "voucherCode", required = false) String voucherCode,
        @RequestParam(value = "renewalAction", required = false) String renewalAction,
        HttpServletRequest request
    ) {
        if (!this.identityAuthService.isLoggedin(request)) {
            throw new HttpErrorException(
                HttpStatus.UNAUTHORIZED, 
                this.messageSource.getMessage("error.identity.notLoggedIN", null, locale)
            );
        }

        Long alephPatronSysId = (Long) session.getAttribute("alephPatron");

        if (alephPatronSysId == null) {
            throw new HttpErrorException(
                HttpStatus.BAD_REQUEST, 
                this.messageSource.getMessage("error.400.text", null, locale)
            );
        }

        Patron alephPatron = patronRepository.findById(alephPatronSysId).orElse(null);  // original Aleph patron

        this.patronDTOValidator.validate(editedPatron, bindingResult, alephPatron.getId(), mediaFiles);

        if (bindingResult.hasErrors()) {
            PatronDTO beforeEditedPatron = (PatronDTO) session.getAttribute("latestPatronDTO");

            if (beforeEditedPatron == null) {
                throw new HttpErrorException(
                    HttpStatus.BAD_REQUEST, 
                    this.messageSource.getMessage("error.400.text", null, locale)
                );
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
            model.addAttribute("outstandingFinesAmount", session.getAttribute("outstandingFinesAmount"));
            model.addAttribute("standardRenewalFeeAmount", registrationFeeService.getRenewalFee(alephPatron));

            model.addAttribute("org.springframework.validation.BindingResult.patron", bindingResult);

            return "callback_registration_renewal";
        }

        String code = (String) session.getAttribute("code");
        String sessionSubmissionKey = "membership-renewal:" + code;
        if (code == null || !SessionSubmissionGuard.claim(session, sessionSubmissionKey)) {
            throw duplicateSubmission(locale);
        }

        Long patronSysId = (Long) session.getAttribute("patron");
        Patron patron = patronRepository.findById(patronSysId).orElse(null);    // Original Latest patron (i.e. patron created by merging BankId patron with Aleph patron)
        Identify userProfile = (Identify) session.getAttribute("userProfile");
        Identity identity = this.identityService.findById((Long) session.getAttribute("identity")).orElse(null);
        String durableSubmissionKey = identity == null || alephPatron == null
            ? null
            : "submission:membership-renewal:" + identity.getId() + ":" + alephPatron.getExpiryDate();

        if (durableSubmissionKey == null || !tryClaimDurably(session, sessionSubmissionKey, durableSubmissionKey)) {
            throw duplicateSubmission(locale);
        }

        session.removeAttribute("alephPatron");
        session.removeAttribute("patron");
        session.removeAttribute("userProfile");
        // session.removeAttribute("identity");
        session.removeAttribute("latestPatron");
        session.removeAttribute("latestPatronDTO");
        session.removeAttribute("bankIdPatronDTO");
        session.removeAttribute("alephPatronDTO");
        session.removeAttribute("membershipExpiryDate");
        session.removeAttribute("membershipHasExpired");
        session.removeAttribute("membershipExpiresToday");
        session.removeAttribute("expiryDateIn1MonthOrLess");
        session.removeAttribute("outstandingFinesAmount");

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
            throw new HttpErrorException(
                HttpStatus.BAD_REQUEST, 
                this.messageSource.getMessage("error.400.text", null, locale)
            );
        }

        if (editedPatron.getExportConsent() != PatronBoolean.Y) {
            throw new HttpErrorException(
                HttpStatus.BAD_REQUEST, 
                this.messageSource.getMessage("error.400.text", null, locale)
            );
        }

        boolean identityWasArchived = identity.isDeleted();
        this.identityActivityService.logMembershipRenewalSubmission(identity);

        this.patronRepository.deleteById(patronSysId);
        this.patronRepository.deleteById(alephPatronSysId);

        patron.update(editedPatron);
        patron.setStatus(this.patronService.determinePatronStatus(patron).getId());
        patron.setExpiryDate(this.patronService.determinePatronExpiryDate(patron));

        try {
            getLogger().info("membership-renewal - finalPatron: {}", patron.toJson());
        } catch (JsonProcessingException e) {
            getLogger().error("Error converting finalPatron to JSON", e);
        }

        Map<String, Object> patronUpdate = this.alephService.updatePatron(patron, alephPatron);
        if (patronUpdate.containsKey("error")) {
            this.tokenService.releaseClaimKey(durableSubmissionKey);
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
        identity.setDeleted(false);
        identity.setUpdatedAt(LocalDateTime.now());
        this.identityService.save(identity);
        if (identityWasArchived) {
            this.identityActivityService.logIdentityRestored(identity);
        }

        if (patronIsCasEmployee) {
            int mediaFilesCount = 0;
            if (mediaFiles != null) {
                mediaFilesCount = (int) Arrays.stream(mediaFiles).filter(file -> file != null && !file.isEmpty()).count();

                boolean hasMediaFiles = mediaFiles != null && mediaFilesCount > 0;

                if (hasMediaFiles) {
                    String submissionBatchId = this.mediaService.createSubmissionBatchId();
                    int batchOrderIndex = 1;
                    for (MultipartFile file : mediaFiles) {
                        if (file == null || file.isEmpty()) {
                            continue;
                        }

                        Map<String, Object> uploadResult = this.mediaService.uploadMedia(
                            file,
                            identity,
                            MediaSubmissionType.RENEWAL,
                            submissionBatchId,
                            batchOrderIndex++
                        );
                        if (uploadResult.containsKey("error")) {
                            getLogger().error("Error uploading media file: {}", uploadResult.get("error"));
                        }
                    }
                }
            }
        }

        this.identityActivityService.logMembershipRenewalSuccess(identity);

        String membershipExpiryDate = patron.getExpiryDate();
        BigDecimal outstandingFinesAmount = paymentService.getPatronTotalDueCash(identity.getAlephId());

        try {
            if (patronHasEmail) {
                this.emailService.sendEmailMembershipRenewal(patronEmail, alephPatronBarcode, patronIsCasEmployee, membershipExpiryDate, locale);
                this.identityActivityService.logNewRegistrationEmailSent(identity);
            }
        } catch (Exception e) {
            getLogger().error("Failed to send a membership renewal confirmation email to " + patronEmail, e);
        }

        // Check if user is an employee
        if (patronIsCasEmployee) {
            voucherCode = null;
            if (isSubmitAndPayAction(renewalAction) && outstandingFinesAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentService.createPayment(identity, PaymentType.RENEWAL_FINES_ONLY);
                getLogger().info("Created fines-only payment for employee renewal. Identity: {}, Barcode: {}, Outstanding fines: {}",
                    identity.getId(), identity.getAlephBarcode(), outstandingFinesAmount);
                return "redirect:/payment/initiate";
            }

            return renderMembershipRenewalSuccess(model, identity, patronIsCasEmployee, patronHasEmail,
                membershipExpiryDate, alephPatronBarcode, outstandingFinesAmount);
        } else {
            // Non-employees need to pay
            BigDecimal discountAmount = BigDecimal.ZERO;
            String appliedVoucherCode = null;
            Voucher appliedVoucher = null;

            // Validate and apply voucher if provided
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                BigDecimal feeAmount = registrationFeeService.getFee(patron);
                Map<String, Object> voucherResult = voucherService.validateVoucher(voucherCode.trim(), identity, feeAmount);

                if (Boolean.TRUE.equals(voucherResult.get("valid"))) {
                    discountAmount = (BigDecimal) voucherResult.get("discountAmount");
                    appliedVoucherCode = voucherCode.trim().toUpperCase();
                    appliedVoucher = (Voucher) voucherResult.get("voucher");
                    getLogger().info("Voucher '{}' applied for membership renewal. Identity: {}, Discount: {}",
                        appliedVoucherCode, identity.getId(), discountAmount);
                } else {
                    getLogger().warn("Invalid voucher '{}' for renewal. Identity: {}, Error: {}",
                        voucherCode, identity.getId(), voucherResult.get("error"));
                    // Proceed without voucher — don't block renewal
                }
            }

            boolean feeFullyCovered = appliedVoucherCode != null
                && discountAmount.compareTo(registrationFeeService.getFee(patron)) >= 0;

            if (feeFullyCovered) {
                voucherService.confirmOrCreateUsage(appliedVoucher, identity, discountAmount);

                if (outstandingFinesAmount.compareTo(BigDecimal.ZERO) > 0) {
                    if (isSubmitAndPayAction(renewalAction)) {
                        Payment payment = paymentService.createPayment(identity, PaymentType.RENEWAL_FINES_ONLY);
                        getLogger().info("Voucher fully covered renewal fee, redirecting to fines-only payment. Identity: {}, Barcode: {}, Outstanding fines: {}",
                            identity.getId(), identity.getAlephBarcode(), outstandingFinesAmount);
                        return "redirect:/payment/initiate";
                    }

                    getLogger().info("Voucher fully covered renewal fee and patron chose not to pay outstanding fines now. Identity: {}, Outstanding fines: {}",
                        identity.getId(), outstandingFinesAmount);
                    return renderMembershipRenewalSuccess(model, identity, patronIsCasEmployee, patronHasEmail,
                        membershipExpiryDate, alephPatronBarcode, outstandingFinesAmount);
                }

                getLogger().info("Fee fully covered by voucher. Completing renewal without payment gateway. Identity: {}", identity.getId());
                return renderMembershipRenewalSuccess(model, identity, patronIsCasEmployee, patronHasEmail,
                    membershipExpiryDate, alephPatronBarcode, outstandingFinesAmount);
            }

            synchronized (this) {
                Map<String, Object> feeCreation = this.alephService.createRenewalFee(patron);
                if (feeCreation.containsKey("error")) {
                    getLogger().error("Error creating renewal fee in Aleph: {}", feeCreation.get("error"));
                    return "error";
                }
            }

            Payment payment = paymentService.createPayment(identity, PaymentType.RENEWAL, appliedVoucherCode, discountAmount);
            getLogger().info("Created payment for membership renewal. Identity: {}, Barcode: {}, Discount: {}",
                identity.getId(), identity.getAlephBarcode(), discountAmount);

            // If partial discount, create pending usage and redirect to payment
            if (appliedVoucherCode != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                voucherService.createPendingUsage(appliedVoucher, identity, discountAmount);
            }

            return "redirect:/payment/initiate";
        }
    }

    private boolean isSubmitAndPayAction(String renewalAction) {
        return "submitAndPay".equalsIgnoreCase(renewalAction);
    }

    private HttpErrorException duplicateSubmission(Locale locale) {
        return new HttpErrorException(
            HttpStatus.CONFLICT,
            this.messageSource.getMessage("error.submission.duplicate", null, locale)
        );
    }

    private boolean tryClaimDurably(
        HttpSession session,
        String sessionSubmissionKey,
        String durableSubmissionKey
    ) {
        try {
            return this.tokenService.tryClaimKey(durableSubmissionKey);
        } catch (RuntimeException e) {
            SessionSubmissionGuard.release(session, sessionSubmissionKey);
            throw e;
        }
    }

    private String renderMembershipRenewalSuccess(
        Model model,
        Identity identity,
        boolean patronIsCasEmployee,
        boolean patronHasEmail,
        String membershipExpiryDate,
        String alephPatronBarcode,
        BigDecimal outstandingFinesAmount
    ) {
        model.addAttribute("apiToken", this.tokenService.createApiToken(identity.getId().toString()));
        model.addAttribute("autoLogoutOnLoad", true);
        model.addAttribute("isIdentityLoggedIn", false);
        model.addAttribute("patronIsCasEmployee", patronIsCasEmployee);
        model.addAttribute("patronHasEmail", patronHasEmail);
        model.addAttribute("membershipExpiryDate", membershipExpiryDate);
        model.addAttribute("alephBarcode", alephPatronBarcode);
        model.addAttribute("outstandingFinesAmount", outstandingFinesAmount);
        model.addAttribute("hasOutstandingFines", outstandingFinesAmount.compareTo(BigDecimal.ZERO) > 0);

        return "membership_renewal_success";
    }
}
