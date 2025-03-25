package cz.cas.lib.bankid_registrator.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

@Component
public class LocaleInterceptor extends ConfigurationAbstract implements HandlerInterceptor
{
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private LocaleResolver localeResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        Locale locale = localeResolver.resolveLocale(request);

        String localizedUri = getLocalizedUri(uri, locale);

        if (!uri.equals(localizedUri)) {
            response.sendRedirect(localizedUri);
            return false;
        }

        return true;
    }

    private String getLocalizedUri(String uri, Locale locale) {
        try {
            String localizedUriPlaceholder = uri.replace("/", ".");
            String localizedUri = this.messageSource.getMessage("route" + localizedUriPlaceholder, null, locale);
            getLogger().info("Localized URI found for " + uri + " in locale " + locale.getLanguage() + ": " + localizedUri);
            return localizedUri;
        } catch (Exception e) {
            getLogger().info("No localized URI found for " + uri + " in locale " + locale.getLanguage());
            return uri;
        }
    }
}