package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
import cz.cas.lib.bankid_registrator.model.identity.Identity;
import cz.cas.lib.bankid_registrator.model.patron.Patron;
import cz.cas.lib.bankid_registrator.services.AlephService;
import cz.cas.lib.bankid_registrator.services.EmailService;
import cz.cas.lib.bankid_registrator.services.IdentityService;
import cz.cas.lib.bankid_registrator.services.TokenService;
import cz.cas.lib.bankid_registrator.util.WebUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    public IdentityController(
        MessageSource messageSource, 
        TokenService tokenService,
        IdentityService identityService,
        AlephService alephService,
        EmailService emailService
    ) {
        super(messageSource);
        this.tokenService = tokenService;
        this.identityService = identityService;
        this.alephService = alephService;
        this.emailService = emailService;
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

        Map<String, Object> patronSearch = this.alephService.getAlephPatron(patronId);

        if (patronSearch.containsKey("error")) {
            getLogger().error("Error while searching for patron with id " + patronId + ": " + patronSearch.get("error").toString());
            throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR, this.messageSource.getMessage("error.token.patronNotFound", null, locale));
        }

        Patron patron = (Patron) patronSearch.get("patron");

        String token = this.tokenService.createIdentityToken(identity);
        String resetLink = WebUtils.getBaseUrl(request) + "/identity/reset-password?token=" + token;
        String emailTo = patron.getEmail();

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
        @RequestParam("newPassword") String newPassword,
        @RequestParam("repeatNewPassword") String repeatNewPassword,
        Model model, Locale locale
    ) {
        boolean isTokenValid = this.tokenService.isIdentityTokenValid(token);

        if (!isTokenValid) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.invalidOrMissing", null, locale));
        }

        if (!newPassword.equals(repeatNewPassword)) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.passwordsNotMatching", null, locale));
        }

        Map<String, Object> pswUpdate = this.updatePatronPassword(token, newPassword);

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
     * @return
     */
    @RequestMapping(value="/identity/set-password", method=RequestMethod.POST, produces=MediaType.TEXT_HTML_VALUE)
    public String passwordSetFormSubmitted(
        @RequestParam("token") String token,
        @RequestParam("newPassword") String newPassword,
        @RequestParam("repeatNewPassword") String repeatNewPassword,
        Model model, Locale locale
    ) {
        boolean isTokenValid = this.tokenService.isIdentityTokenValid(token);

        if (!isTokenValid) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.invalidOrMissing", null, locale));
        }

        if (!newPassword.equals(repeatNewPassword)) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.passwordsNotMatching", null, locale));
        }

        Map<String, Object> pswUpdate = this.updatePatronPassword(token, newPassword);

        if (pswUpdate.containsKey("error")) {
            throw new HttpErrorException(HttpStatus.INTERNAL_SERVER_ERROR, this.messageSource.getMessage(pswUpdate.get("error").toString(), null, locale));
        }

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordSetting.title", null, locale));

        return "identity_set_password_success";
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

        // TODO: Blacklist the successfully used token here

        result.put("success", true);

        return result;
    }
}
