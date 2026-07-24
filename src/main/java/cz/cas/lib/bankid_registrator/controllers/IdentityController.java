package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.RegistrationFeeConfig;
import cz.cas.lib.bankid_registrator.configurations.SessionTimerConfig;
import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentStatus;
import cz.cas.lib.bankid_registrator.entities.payment.PaymentType;
import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.model.payment.Payment;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.EmailService;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.LdapService;
import cz.cas.lib.bankid_registrator.services.PaymentService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import cz.cas.lib.bankid_registrator.services.VoucherService;
import cz.cas.lib.bankid_registrator.util.WebUtils;
import cz.cas.lib.bankid_registrator.util.StringUtils;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for identities i.e. users who have verified their identity via BankID
 */
@Controller
public class IdentityController extends ControllerAbstract
{
    private final TokenService tokenService;
    private final IdentityService identityService;
    private final AlephService alephService;
    private final EmailService emailService;
    private final LdapService ldapService;
    private final PaymentService paymentService;
    private final VoucherService voucherService;
    private final RegistrationFeeConfig registrationFeeConfig;

    public IdentityController(
        MessageSource messageSource,
        TokenService tokenService,
        IdentityService identityService,
        AlephService alephService,
        EmailService emailService,
        IdentityAuthService identityAuthService,
        LdapService ldapService,
        PaymentService paymentService,
        VoucherService voucherService,
        RegistrationFeeConfig registrationFeeConfig,
        SessionTimerConfig sessionTimerConfig
    ) {
        super(messageSource, identityAuthService, sessionTimerConfig);
        this.tokenService = tokenService;
        this.identityService = identityService;
        this.alephService = alephService;
        this.emailService = emailService;
        this.ldapService = ldapService;
        this.paymentService = paymentService;
        this.voucherService = voucherService;
        this.registrationFeeConfig = registrationFeeConfig;
    }

    /**
     * A page for requesting a link to reset the password of an identity
     * @param model
     * @param locale
     * @param session
     * @return
     */
    @RequestMapping(value="/identity/reset-password-request", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String ResetPasswordRequestView(Model model, Locale locale, HttpSession session) {
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordResetRequest.title", null, locale));

        return "identity_reset_password_request";
    }

    /**
     * Handle the form submission for requesting a link to reset the password of an identity
     * @param alephPatronBarcode
     * @param model
     * @param locale
     * @param session
     * @param request
     * @return
     */
    @RequestMapping(value="/identity/reset-password-request", method=RequestMethod.POST, produces=MediaType.TEXT_HTML_VALUE)
    public String ResetPasswordRequestFormSubmitted(
        @RequestParam("username") String alephPatronBarcode,
        Model model, Locale locale, HttpSession session, HttpServletRequest request
    ) {
        Optional<Identity> identitySearch = this.identityService.findByAlephBarcode(alephPatronBarcode);
        if (!identitySearch.isPresent()) {
            getLogger().error("Identity with Aleph barcode " + alephPatronBarcode + " not found");
            throw new HttpErrorException(HttpStatus.NOT_FOUND, this.messageSource.getMessage("error.token.identityNotFound", null, locale));
        }

        Identity identity = identitySearch.get();
        String patronId = identity.getAlephId();

        Map<String, Object> patronSearch = this.alephService.getAlephPatron(patronId, false);

        if (patronSearch.containsKey("error")) {
            getLogger().error("Error while searching for patron with id " + patronId + ": " + patronSearch.get("error").toString());
            throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR, this.messageSource.getMessage("error.token.patronNotFound", null, locale));
        }

        Patron patron = (Patron) patronSearch.get("patron");

        String token = this.tokenService.createIdentityToken(identity);
        String resetLink = WebUtils.getBaseUrl(request) + "/identity/reset-password?token=" + token;
        String emailTo = patron.getEmail();

        if (StringUtils.isEmpty(emailTo)) {
            getLogger().error("Patron with id " + patronId + " has no email address");
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.email.patronNoEmail", null, locale));
        }

        try {
            this.emailService.sendEmailIdentityPasswordReset(emailTo, resetLink, locale);
        } catch (Exception e) {
            getLogger().error("Failed to send email to " + emailTo + ": " + e.getMessage());
            throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR, this.messageSource.getMessage("error.email.identityPasswordReset.failedToSend", null, locale));
        }

        String obfuscatedEmail = this.emailService.getObfuscatedEmail(emailTo);

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordResetRequest.title", null, locale));
        model.addAttribute("emailTo", obfuscatedEmail);

        return "identity_reset_password_request_success";
    }

    /**
     * A page for resetting the password of an identity
     * @param token
     * @param model
     * @return 
     */
    @RequestMapping(value="/identity/reset-password", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String ResetPasswordView(@RequestParam("token") String token, Model model, Locale locale, HttpSession session) {
        boolean isTokenValid = this.tokenService.isIdentityTokenValid(token);

        if (!isTokenValid) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.invalidOrMissing", null, locale));
        }

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordResetting.title", null, locale));
        model.addAttribute("token", token);
        model.addAttribute("passwordDTO", new PatronPasswordDTO());

        return "identity_reset_password";
    }

    /**
     * Handle the form submission for resetting the password of an identity
     * @param token
     * @param newPassword
     * @param repeatNewPassword
     * @param model
     * @param locale
     * @return
     */
    @RequestMapping(value="/identity/reset-password", method=RequestMethod.POST, produces=MediaType.TEXT_HTML_VALUE)
    public String passwordResetFormSubmitted(
        @RequestParam("token") String token,
        @Valid @ModelAttribute("passwordDTO") PatronPasswordDTO passwordDTO,
        BindingResult bindingResult,
        Model model, Locale locale
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordResetting.title", null, locale));
            return "identity_reset_password";
        }

        boolean isTokenValid = this.tokenService.isIdentityTokenValid(token);

        if (!isTokenValid) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.invalidOrMissing", null, locale));
        }

        Map<String, Object> pswUpdate = this.updatePatronPassword(token, passwordDTO.getNewPassword());

        if (pswUpdate.containsKey("error")) {
            throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR, this.messageSource.getMessage(pswUpdate.get("error").toString(), null, locale));
        }

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordResetting.title", null, locale));

        return "identity_reset_password_success";
    }

    /**
     * Handle the form submission for setting the password of an identity
     * @param token
     * @param newPassword
     * @param repeatNewPassword
     * @param model
     * @param locale
     * @param request
     * @return
     */
    @RequestMapping(value="/identity/set-password", method=RequestMethod.POST, produces=MediaType.TEXT_HTML_VALUE)
    public String passwordSetFormSubmitted(
        @RequestParam("token") String token,
        @RequestParam(value = "voucherCode", required = false) String voucherCode,
        @Valid @ModelAttribute("passwordDTO") PatronPasswordDTO passwordDTO,
        BindingResult bindingResult,
        Model model, 
        Locale locale, 
        HttpServletRequest request
    ) {
        if (!this.identityAuthService.isLoggedin(request)) {
            return "error_session_expired";
        }

        boolean isTokenValid = this.tokenService.isIdentityTokenValid(token);
        boolean patronIsCasEmployee = false;

        if (!isTokenValid) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.invalidOrMissing", null, locale));
        }

        String patronAlephId = null;

        String patronPassword = passwordDTO.getNewPassword();
        Map<String, Object> pswUpdate = this.updatePatronPassword(token, patronPassword);

        if (pswUpdate.containsKey("error")) {
            throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR, this.messageSource.getMessage("error.identityPassword.failed", null, locale));
        }

        request.getSession().setAttribute("patronPassword", patronPassword);

        Long identityId = null;
        Identity identity = null;
        String patronAlephBarcode = null;

        try {
            identityId = Long.parseLong(this.tokenService.extractIdentityIdFromToken(token));
            identity = this.identityService.findById(identityId).get();

            // Store identity ID in session for payment flow
            request.getSession().setAttribute("identity", identityId);
            patronAlephId = identity.getAlephId();
            patronAlephBarcode = identity.getAlephBarcode();
            Patron patron = (Patron) this.alephService.getAlephPatron(patronAlephId, true).get("patron");

            model.addAttribute("alephId", patronAlephId);
            model.addAttribute("alephBarcode", patronAlephBarcode);
            model.addAttribute("apiToken", this.tokenService.createApiToken(identityId.toString()));

            patronIsCasEmployee = patron.isCasEmployee;
        } catch (Exception e) {
            getLogger().error("Failed to get identity or patron from a password-setting form: " + e.getMessage());
            throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR, this.messageSource.getMessage("error.identityPassword.failed", null, locale));
        }

        // Check if user is an employee
        if (patronIsCasEmployee) {
            // Employees don't need to pay, show success page with loader
            Boolean patronLdapSynced = this.ldapService.accountExistsByLogin(patronAlephId, patronPassword);

            model.addAttribute("isIdentityLoggedIn", false);
            model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordSetting.success", null, locale));
            model.addAttribute("patronIsCasEmployee", patronIsCasEmployee);
            model.addAttribute("patronLdapSynced", patronLdapSynced);

            return "identity_set_password_success";
        } else {
            // Non-employees need to pay
            BigDecimal discountAmount = BigDecimal.ZERO;
            String appliedVoucherCode = null;

            // Validate and apply voucher if provided
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                BigDecimal feeAmount = registrationFeeConfig.getDefaultAmount();
                Map<String, Object> voucherResult = voucherService.validateVoucher(voucherCode.trim(), identity, feeAmount);

                if (Boolean.TRUE.equals(voucherResult.get("valid"))) {
                    discountAmount = (BigDecimal) voucherResult.get("discountAmount");
                    appliedVoucherCode = voucherCode.trim().toUpperCase();
                    getLogger().info("Voucher '{}' applied for registration. Identity: {}, Discount: {}",
                        appliedVoucherCode, identity.getId(), discountAmount);
                } else {
                    getLogger().warn("Invalid voucher '{}' for registration. Identity: {}, Error: {}",
                        voucherCode, identity.getId(), voucherResult.get("error"));
                    // Proceed without voucher — don't block registration
                }
            }

            // Determine if the fee is fully covered by voucher (no Aleph fee needed)
            boolean feeFullyCovered = appliedVoucherCode != null
                && discountAmount.compareTo(registrationFeeConfig.getDefaultAmount()) >= 0
                && paymentService.getPatronTotalDueCash(identity.getAlephId()).compareTo(BigDecimal.ZERO) == 0;

            // Create the Aleph registration fee only when the patron actually needs to pay
            if (!feeFullyCovered) {
                synchronized (this) {
                    Map<String, Object> feeCreation = this.alephService.createRegistrationFee(
                        (Patron) this.alephService.getAlephPatron(patronAlephId, false).get("patron"));
                    if (feeCreation.containsKey("error")) {
                        getLogger().error("Error creating registration fee in Aleph: {}", feeCreation.get("error"));
                        throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                            this.messageSource.getMessage("error.identityPassword.failed", null, locale));
                    }
                }
            }

            Payment payment = paymentService.createPayment(identity, PaymentType.REGISTRATION, appliedVoucherCode, discountAmount);
            getLogger().info("Created payment for new registration. Identity: {}, Barcode: {}, Discount: {}",
                identity.getId(), identity.getAlephBarcode(), discountAmount);

            // If voucher fully covers the fee, create pending usage and confirm immediately
            if (payment.getStatus() == PaymentStatus.VOUCHER_COVERED && appliedVoucherCode != null) {
                voucherService.createPendingUsage(
                    voucherService.findByCode(appliedVoucherCode).orElse(null),
                    identity, discountAmount);
                voucherService.confirmUsage(
                    voucherService.findByCode(appliedVoucherCode).orElse(null),
                    identity);

                getLogger().info("Fee fully covered by voucher. Redirecting to success. Identity: {}", identity.getId());
                return "redirect:/payment/callback?refId=" + identity.getAlephBarcode() + "&status=success";
            }

            // If partial discount or no voucher, create pending usage and redirect to payment
            if (appliedVoucherCode != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                voucherService.createPendingUsage(
                    voucherService.findByCode(appliedVoucherCode).orElse(null),
                    identity, discountAmount);
            }

            return "redirect:/payment/initiate";
        }
    }

    /**
     * Update the password of identity's patron in Aleph
     * @param token
     * @param password
     * @return
     */
    public Map<String, Object> updatePatronPassword(String token, String password) {
        Map<String, Object> result = new HashMap<>();

        Long identityId = Long.parseLong(this.tokenService.extractIdentityIdFromToken(token));
        Optional<Identity> identity = this.identityService.findById(identityId);

        if (!identity.isPresent()) {
            getLogger().error("Identity with id " + identityId + " not found");
            result.put("error", "error.token.identityNotFound");
            return result;
        }

        String patronId = identity.get().getAlephId();

        Map<String, Object> pswReset = this.alephService.updatePatronPassword(patronId, password);

        if (pswReset.get("error") != null) {
            getLogger().error("Failed to update password for patron " + patronId + ": " + pswReset.get("error"));
            result.put("error", "error.identityPassword.failed");
            return result;
        }

        // Mark that the identity has set their password
        identity.get().setPasswordSet(true);
        this.identityService.save(identity.get());

        // TODO: Blacklist the successfully used token here

        result.put("success", true);

        return result;
    }
}
