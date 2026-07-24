package cz.cas.lib.bankid_registrator.exceptions;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

    public GlobalExceptionHandler(MessageSource messageSource)
    {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleException(Exception e, Locale locale)
    {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String statusCode = String.valueOf(status.value());
        String statusMessage = this.messageSource.getMessage("error." + statusCode + ".text", null, status.getReasonPhrase(), locale);
        String detailMessage = e.getMessage();

        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", statusCode + " " + statusMessage);
        mav.addObject("errorTitle", statusMessage);
        mav.addObject("errorCode", statusCode);
        if (detailMessage != null) {
            mav.addObject("error", detailMessage);
        }
        mav.setViewName("error");
        mav.setStatus(status);

        this.getLogger().error("Exception: " + statusMessage, e);

        return mav;
    }

    @ExceptionHandler(value = HttpMediaTypeNotAcceptableException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ModelAndView handleMediaTypeNotAcceptableException(
        HttpMediaTypeNotAcceptableException e,
        Locale locale,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_ACCEPTABLE;
        String statusCode = String.valueOf(status.value());
        String statusMessage = this.messageSource.getMessage("error." + statusCode + ".text", null, status.getReasonPhrase(), locale);

        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", statusCode + " " + statusMessage);
        mav.addObject("errorTitle", statusMessage);
        mav.addObject("errorCode", statusCode);
        mav.addObject("error", e.getMessage());
        mav.setViewName("error");
        mav.setStatus(status);

        this.getLogger().warn(
            "Unacceptable media type requested: method={}, uri={}, accept={}, producibleMediaTypes={}",
            request.getMethod(),
            request.getRequestURI(),
            request.getHeader("Accept"),
            e.getSupportedMediaTypes()
        );

        return mav;
    }

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ModelAndView handleMethodNotSupportedException(
        HttpRequestMethodNotSupportedException e,
        Locale locale,
        HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        String statusCode = String.valueOf(status.value());
        String statusMessage = this.messageSource.getMessage("error." + statusCode + ".text", null, status.getReasonPhrase(), locale);

        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", statusCode + " " + statusMessage);
        mav.addObject("errorTitle", statusMessage);
        mav.addObject("errorCode", statusCode);
        mav.addObject("error", e.getMessage());
        mav.setViewName("error");
        mav.setStatus(status);

        this.getLogger().warn(
            "Unsupported request method: method={}, uri={}, supportedMethods={}",
            request.getMethod(),
            request.getRequestURI(),
            e.getSupportedHttpMethods()
        );

        return mav;
    }

    @ExceptionHandler(value = HttpErrorException.class)
    public ModelAndView handleHttpErrorException(HttpErrorException e, Locale locale)
    {
        HttpStatus status = e.getStatus();
        String statusCode = String.valueOf(status.value());
        String statusMessage = this.messageSource.getMessage("error." + statusCode + ".text", null, status.getReasonPhrase(), locale);
        String detailMessage = e.getMessage();

        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", statusCode + " " + statusMessage);
        mav.addObject("errorTitle", statusMessage);
        mav.addObject("errorCode", statusCode);
        if (detailMessage != null) {
            mav.addObject("error", detailMessage);
        }
        mav.setViewName("error");
        mav.setStatus(status);

        this.getLogger().error("Exception: " + (detailMessage != null ? detailMessage : statusMessage), e);

        return mav;
    }

    @ExceptionHandler(value = IdentityAuthException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ModelAndView handleIdentityAuthException(IdentityAuthException e, Locale locale)
    {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String statusCode = String.valueOf(status.value());
        String statusMessage = this.messageSource.getMessage("error." + statusCode + ".text", null, status.getReasonPhrase(), locale);
        String detailMessage = e.getMessage() != null ? e.getMessage() : this.messageSource.getMessage("error.identity.auth", null, null, locale);

        ModelAndView mav = new ModelAndView();
        mav.addObject("lang", locale.getLanguage());
        mav.addObject("appName", this.appName);
        mav.addObject("pageTitle", statusCode + " " + statusMessage);
        mav.addObject("errorTitle", statusMessage);
        mav.addObject("errorCode", statusCode);
        if (detailMessage != null) {
            mav.addObject("error", detailMessage);
        }
        mav.setViewName("error");
        mav.setStatus(status);

        this.getLogger().error("Exception: " + (detailMessage != null ? detailMessage : statusMessage), e.getCause() != null ? e.getCause() : e);

        return mav;
    }
}
