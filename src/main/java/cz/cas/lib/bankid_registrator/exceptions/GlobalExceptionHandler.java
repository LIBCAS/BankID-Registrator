package cz.cas.lib.bankid_registrator.exceptions;

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
        mav.addObject("errorCode", this.messageSource.getMessage("error.500.code", null, locale));
        getLogger().error("Exception: " + e.getMessage());
        mav.setViewName("error");
        return mav;
    }    
}
