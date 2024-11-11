package cz.cas.lib.bankid_registrator;

import cz.cas.lib.bankid_registrator.exceptions.GlobalExceptionHandler;
import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
import cz.cas.lib.bankid_registrator.exceptions.IdentityAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests for {@link GlobalExceptionHandler}.
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * ./mvnw -Dtest=GlobalExceptionHandlerTest test
 * }</pre> 
 */
public class GlobalExceptionHandlerTest
{
    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        this.exceptionHandler = new GlobalExceptionHandler(this.messageSource);
    }

    @Test
    public void testHandleException()
    {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String statusCode = String.valueOf(status.value());
        String statusReasonPhrase = status.getReasonPhrase();
        String statusReasonPhraseTranslationKey = "error." + statusCode + ".text";
        String exceptionMessage = "Test exception";

        Mockito
            .when(this.messageSource.getMessage(eq(statusReasonPhraseTranslationKey), any(), eq(statusReasonPhrase), eq(DEFAULT_LOCALE)))
            .thenReturn(statusReasonPhraseTranslationKey);

        Exception exception = new Exception(exceptionMessage);
        ModelAndView modelAndView = this.exceptionHandler.handleException(exception, DEFAULT_LOCALE);

        assertEquals("error", modelAndView.getViewName());
        assertEquals(status, modelAndView.getStatus());
        assertEquals(statusCode, modelAndView.getModel().get("errorCode"));
        assertEquals(statusReasonPhraseTranslationKey, modelAndView.getModel().get("errorTitle"));
        assertEquals(exceptionMessage, modelAndView.getModel().get("error"));
        assertEquals(DEFAULT_LOCALE.getLanguage(), modelAndView.getModel().get("lang"));
    }

    /**
     * Test that the {@link GlobalExceptionHandler#handleHttpErrorException(HttpErrorException, Locale)} method 
     * returns a {@link ModelAndView} with the correct values set when the message translation is missing.
     */
    @Test
    public void testHandleHttpErrorException_shouldHaveStatusReasonPhraseAsErrorTitle_whenMessageTranslationMissing()
    {
        HttpStatus status = HttpStatus.GONE;
        String statusCode = String.valueOf(status.value());
        String statusReasonPhrase = status.getReasonPhrase();
        String statusReasonPhraseTranslationKey = "error." + statusCode + ".text";

        // Mock the message source to return the status reason phrase because in this scenario the message translation 'error.410.text' is missing
        Mockito
            .when(this.messageSource.getMessage(eq(statusReasonPhraseTranslationKey), any(), eq(statusReasonPhrase), eq(DEFAULT_LOCALE)))
            .thenReturn(statusReasonPhrase);

        HttpErrorException exception = new HttpErrorException(status);
        ModelAndView modelAndView = this.exceptionHandler.handleHttpErrorException(exception, DEFAULT_LOCALE);

        assertEquals("error", modelAndView.getViewName());
        assertEquals(status, modelAndView.getStatus());
        assertEquals(statusCode, modelAndView.getModel().get("errorCode"));
        assertEquals(statusReasonPhrase, modelAndView.getModel().get("errorTitle"));
        assertEquals(null, modelAndView.getModel().get("error"));
        assertEquals(DEFAULT_LOCALE.getLanguage(), modelAndView.getModel().get("lang"));
    }

    /**
     * Test that the {@link GlobalExceptionHandler#handleHttpErrorException(HttpErrorException, Locale)} method
     * returns a {@link ModelAndView} with the correct values set when the message translation is present.
     */
    @Test
    public void testHandleHttpErrorException_shouldHaveTranslatedStatusReasonPhraseAsErrorTitle_whenStatusReasonPhraseTranslationPresent()
    {
        HttpStatus status = HttpStatus.GONE;
        String statusCode = String.valueOf(status.value());
        String statusReasonPhrase = status.getReasonPhrase();
        String statusReasonPhraseTranslationKey = "error." + statusCode + ".text";
        String statusReasonPhraseTranslated = "It's gone guys!";

        // Mock the message source to return the translated custom status reason phrase because in this scenario the translation for 'error.410.text' is present
        Mockito
            .when(this.messageSource.getMessage(eq(statusReasonPhraseTranslationKey), any(), eq(statusReasonPhrase), eq(DEFAULT_LOCALE)))
            .thenReturn(statusReasonPhraseTranslated);

        HttpErrorException exception = new HttpErrorException(status);
        ModelAndView modelAndView = this.exceptionHandler.handleHttpErrorException(exception, DEFAULT_LOCALE);

        assertEquals("error", modelAndView.getViewName());
        assertEquals(status, modelAndView.getStatus());
        assertEquals(statusCode, modelAndView.getModel().get("errorCode"));
        assertEquals(statusReasonPhraseTranslated, modelAndView.getModel().get("errorTitle"));
        assertEquals(null, modelAndView.getModel().get("error"));
        assertEquals(DEFAULT_LOCALE.getLanguage(), modelAndView.getModel().get("lang"));
    }

    /**
     * Test that the {@link GlobalExceptionHandler#handleHttpErrorException(HttpErrorException, Locale)} method
     * returns a {@link ModelAndView} with the correct values set when the exception message is present.
     */
    @Test
    public void testHandleHttpErrorException_shouldHaveExceptionMessageAsError_whenExceptionMessagePresent()
    {
        HttpStatus status = HttpStatus.GONE;
        String statusCode = String.valueOf(status.value());
        String statusReasonPhrase = status.getReasonPhrase();
        String statusReasonPhraseTranslationKey = "error." + statusCode + ".text";
        String statusReasonPhraseTranslated = "It's gone guys!";
        String exceptionMessage = "This is a custom message";

        // Mock the message source to return the translated custom status reason phrase because in this scenario the translation for 'error.410.text' is present
        Mockito
            .when(this.messageSource.getMessage(eq(statusReasonPhraseTranslationKey), any(), eq(statusReasonPhrase), eq(DEFAULT_LOCALE)))
            .thenReturn(statusReasonPhraseTranslated);

        HttpErrorException exception = new HttpErrorException(status, exceptionMessage);
        ModelAndView modelAndView = this.exceptionHandler.handleHttpErrorException(exception, DEFAULT_LOCALE);

        assertEquals("error", modelAndView.getViewName());
        assertEquals(status, modelAndView.getStatus());
        assertEquals(statusCode, modelAndView.getModel().get("errorCode"));
        assertEquals(statusReasonPhraseTranslated, modelAndView.getModel().get("errorTitle"));
        assertEquals(exceptionMessage, modelAndView.getModel().get("error"));
        assertEquals(DEFAULT_LOCALE.getLanguage(), modelAndView.getModel().get("lang"));
    }

    /**
     * Test that the {@link GlobalExceptionHandler#handleIdentityAuthException(IdentityAuthException, Locale)} method
     * returns a {@link ModelAndView} with the correct values set when the exception message is present.
     */
    @Test
    public void testHandleIdentityAuthException_shouldHaveExceptionMessageAsError_whenExceptionMessagePresent()
    {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String statusCode = String.valueOf(status.value());
        String statusReasonPhrase = status.getReasonPhrase();
        String statusReasonPhraseTranslationKey = "error." + statusCode + ".text";
        String statusReasonPhraseTranslated = "Service unavailable";
        String exceptionMessage = "Oh no! The service is unavailable at the moment! Try later.";

        // Mock the message source to return the translated custom status reason phrase because in this scenario the translation for 'error.503.text' is present
        Mockito
            .when(this.messageSource.getMessage(eq(statusReasonPhraseTranslationKey), any(), eq(statusReasonPhrase), eq(DEFAULT_LOCALE)))
            .thenReturn(statusReasonPhraseTranslated);

        IdentityAuthException exception = new IdentityAuthException(exceptionMessage);
        ModelAndView modelAndView = this.exceptionHandler.handleIdentityAuthException(exception, DEFAULT_LOCALE);

        assertEquals("error", modelAndView.getViewName());
        assertEquals(status, modelAndView.getStatus());
        assertEquals(statusCode, modelAndView.getModel().get("errorCode"));
        assertEquals(statusReasonPhraseTranslated, modelAndView.getModel().get("errorTitle"));
        assertEquals(exceptionMessage, modelAndView.getModel().get("error"));
        assertEquals(DEFAULT_LOCALE.getLanguage(), modelAndView.getModel().get("lang"));
    }
}
