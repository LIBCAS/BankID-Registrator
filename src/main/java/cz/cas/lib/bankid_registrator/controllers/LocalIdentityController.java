package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.dto.PatronPasswordDTO;
import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
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
@RequestMapping("/test")
public class LocalIdentityController extends ControllerAbstract
{
    public LocalIdentityController(
        MessageSource messageSource
    ) {
        super(messageSource);
    }

    /**
     * A page for requesting a link to reset the password of an identity
     * @param model
     * @param locale
     * @param session
     * @return
     */
    @RequestMapping(value="/identity_reset_password_request", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String ResetPasswordRequestView(Model model, Locale locale, HttpSession session) {
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
    public String ResetPasswordView(Model model, Locale locale, HttpSession session) {
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
}