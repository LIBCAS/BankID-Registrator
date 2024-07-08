package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.util.WebUtils;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Abstract controller for default Thymeleaf-linked controllers
 */
public abstract class ControllerAbstract
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MessageSource messageSource;

    @NotEmpty
    @Value("${spring.application.name}")
    protected String appName;

    public ControllerAbstract(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model, Locale locale, HttpServletRequest request) {
        String currentUrl = WebUtils.getCurrentUrl(request, "lang");
        String currentUrlParamsConnector = currentUrl.contains("?") ? "&" : "?";

        model.addAttribute("lang", locale.getLanguage());
        model.addAttribute("appName", this.appName);
        model.addAttribute("currentUrl", currentUrl);
        model.addAttribute("currentUrlParamsConnector", currentUrlParamsConnector);
    }

    /**
     * Get logger
     * @return 
     */
    protected final Logger getLogger() {
        return this.logger;
    }

    /**
     * Log controller init debug message
     */
    protected void init() {
        getLogger().debug("initializing controller " + getClass().getSimpleName() + " ...");
    }

    /**
     * Get app's base URL
     */
    protected String getBaseUrl(HttpServletRequest request) {
        return WebUtils.getBaseUrl(request);
    }
}