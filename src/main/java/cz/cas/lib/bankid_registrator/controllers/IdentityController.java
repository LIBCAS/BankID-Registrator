package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
import cz.cas.lib.bankid_registrator.services.TokenService;
import java.util.Locale;
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

    public IdentityController(MessageSource messageSource, TokenService tokenService) {
        super(messageSource);
        this.tokenService = tokenService;
    }

    /**
     * 
     * @param token
     * @param model
     * @return 
     */
    @RequestMapping(value="/identity/password-reset", method=RequestMethod.GET, produces=MediaType.TEXT_HTML_VALUE)
    public String ResetPassword(@RequestParam("token") String token, Model model, Locale locale, HttpSession session) {
        boolean isTokenValid = this.tokenService.isIdentityTokenValid(token);

        if (!isTokenValid) {
            throw new HttpErrorException(HttpStatus.BAD_REQUEST, this.messageSource.getMessage("error.token.invalidOrMissing", null, locale));
        }

        model.addAttribute("pageTitle", this.messageSource.getMessage("page.identityPasswordReset.title", null, locale));
        model.addAttribute("token", token);

        return "identity_password_reset";
    }
}
