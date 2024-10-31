package cz.cas.lib.bankid_registrator.exceptions;

import cz.cas.lib.bankid_registrator.util.StringUtils;
import java.util.Locale;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * A global exception handler for the app. It handles exceptions by redirecting to an error page and passing the error message to the page.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ExceptionHandlerAbstract
{
    @NotEmpty
    @Value("${spring.application.name}")
    private String appName;

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleException(Exception e, Locale locale) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("error", e.getMessage());
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", this.messageSource.getMessage("error.500.text", null, locale));
        mav.addObject("errorCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        getLogger().error("Exception: " + e.getMessage(), e);
        mav.setViewName("error");
        return mav;
    }

    @ExceptionHandler(value = HttpErrorException.class)
    public ModelAndView handleHttpErrorException(HttpErrorException e, Locale locale) {
        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("error", "");
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", this.messageSource.getMessage("page.error.title", null, "Error", locale));

        HttpStatus status = e.getStatus();
        String statusCode = String.valueOf(status.value());
        if ((status.is4xxClientError() || status.is5xxServerError()) && StringUtils.isEmpty(e.getErrorMessage())) {
            mav.addObject("errorCode", this.messageSource.getMessage("error." + statusCode + ".code", null, statusCode, locale));
            mav.addObject("error", this.messageSource.getMessage("error." + statusCode + ".text", null, e.getErrorMessage(), locale));
        } else {
            mav.addObject("errorCode", statusCode);
            mav.addObject("error", e.getErrorMessage());
        }

        getLogger().error("Exception: " + e.getErrorMessage());
        mav.setViewName("error");
        mav.setStatus(status);

        return mav;
    }

    @ExceptionHandler(value = IdentityAuthException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ModelAndView handleIdentityAuthException(Exception e, Locale locale) {
        String exMsg = this.messageSource.getMessage(e.getMessage().isEmpty() ? "error.identity.auth" : e.getMessage(), null, locale);

        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("error", exMsg);
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", this.messageSource.getMessage("error.503.text", null, locale));
        mav.addObject("errorCode", HttpStatus.SERVICE_UNAVAILABLE.value());
        getLogger().error(exMsg, e);
        mav.setViewName("error");
        return mav;
    }
}
