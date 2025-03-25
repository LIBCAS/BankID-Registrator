package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
import cz.cas.lib.bankid_registrator.services.IdentityAuthService;
import java.util.Locale;
import javax.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Local Controller for identities i.e. users who have verified their identity via BankID
 */
@Controller
@Profile("local")
@RequestMapping("/local")
public class LocalIdentityController extends ControllerAbstract
{
    public LocalIdentityController(
        MessageSource messageSource, 
        IdentityAuthService identityAuthService
    ) {
        super(messageSource, identityAuthService);
    }

    /**
     * A page for requesting a link to reset the password of an identity
     * @param model
     * @param locale
     * @param session
     * @return
     */
    @RequestMapping(value="/identity_reset_password_request", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testResetPasswordRequestView(Model model, Locale locale, HttpSession session) {
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordResetRequest.title", null, locale));

        return "identity_reset_password_request";
    }

    /**
     * A page for resetting the password of an identity
     * @param token
     * @param model
     * @return 
     */
    @RequestMapping(value="/identity_reset_password", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testResetPasswordView(Model model, Locale locale, HttpSession session) {
        String token = "1234567890";
        boolean isTokenValid = true;

        if (!isTokenValid) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.invalidOrMissing", null, locale));
        }

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordResetting.title", null, locale));
        model.addAttribute("token", token);
        model.addAttribute("passwordDTO", new PatronPasswordDTO());

        return "identity_reset_password";
    }

    /**
     * A page displayed after the password for an identity (normal user) has been successfully set during the new registration process
     */
    @RequestMapping(value="/identity_set_password_success/normal", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testNewRegistrationPasswordSetSuccessView_normal(Model model, Locale locale) {
        model.addAttribute("alephBarcode", "123456789");
        model.addAttribute("isIdentityLoggedIn", false);
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordSetting.title", null, locale));
        model.addAttribute("patronIsCasEmployee", false);
        model.addAttribute("patronLdapSynced", true);

        return "identity_set_password_success";
    }

    /**
     * A page displayed after the password for an identity (employee) has been successfully set during the new registration process
     */
    @RequestMapping(value="/identity_set_password_success/employee", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String testNewRegistrationPasswordSetSuccessView_employee(Model model, Locale locale) {
        model.addAttribute("alephBarcode", "123456789");
        model.addAttribute("isIdentityLoggedIn", false);
        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordSetting.title", null, locale));
        model.addAttribute("patronIsCasEmployee", true);
        model.addAttribute("patronLdapSynced", true);

        return "identity_set_password_success";
    }
}