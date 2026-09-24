package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.exceptions.AmbiguousPatronMatchException;
import cz.cas.lib.bankid_registrator.services.*;
import cz.cas.lib.bankid_registrator.valueobjs.AccessTokenContainer;
import java.util.Locale;
import java.util.Optional;
import cz.cas.lib.bankid_registrator.model.app_settings.SupportEmail;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatronMatchingCallbackTest {
    @Spy MessageSource messageSource = translations();

    private static MessageSource translations() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
    @Mock MainService mainService;
    @Mock AlephServiceIface envAlephService;
    @Mock IdentityAuthService identityAuthService;
    @Mock AccessTokenContainer accessTokenContainer;
    @Mock IdentityService identityService;
    @Mock PaymentService paymentService;
    @Mock AppSettingsService appSettingsService;
    @InjectMocks MainController controller;

    @ParameterizedTest
    @CsvSource({"cs,false", "cs,true", "en,false", "en,true", "sk,false", "sk,true"})
    void ambiguousMatchLogsOutAndStopsBeforeIdentityOrPaymentChanges(String language, boolean hasSupportEmail) {
        Locale locale = Locale.forLanguageTag(language);
        MockHttpServletRequest request = new MockHttpServletRequest();
        ExtendedModelMap model = new ExtendedModelMap();
        when(identityAuthService.isLoggedin(request)).thenReturn(true);
        when(mainService.isTokenValid(null)).thenReturn(true);
        when(envAlephService.newPatron(null, null)).thenThrow(new AmbiguousPatronMatchException());
        SupportEmail supportEmail = new SupportEmail();
        supportEmail.setEmail("support@example.com");
        when(appSettingsService.getPrimarySupportEmail())
            .thenReturn(hasSupportEmail ? Optional.of(supportEmail) : Optional.empty());
        String key = hasSupportEmail ? "error.identity.ambiguousPatron.withEmail" : "error.identity.ambiguousPatron";
        Object[] arguments = hasSupportEmail ? new Object[] {"support@example.com"} : null;
        String message = messageSource.getMessage(key, arguments, locale);

        assertEquals("error", controller.CallbackEntry("code", null, null, null, null,
            model, locale, request.getSession(), request));
        assertEquals(message, model.getAttribute("errorHtml"));
        assertNull(model.getAttribute("errorSupportEmail"));
        assertFalse(message.contains("{0}"), "The email placeholder must be resolved");
        if (hasSupportEmail) {
            assertTrue(message.contains("href=\"mailto:support@example.com\""));
            assertTrue(message.contains(">support@example.com</a>"));
        } else {
            assertFalse(message.contains("mailto:"));
            assertFalse(message.contains("support@example.com"));
        }
        verify(identityAuthService).logout(request);
        verifyNoInteractions(identityService, paymentService);
    }
}
