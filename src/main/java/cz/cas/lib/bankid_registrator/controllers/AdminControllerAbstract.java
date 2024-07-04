package cz.cas.lib.bankid_registrator.controllers;

import javax.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Abstract controller for admin pages
 */
public abstract class AdminControllerAbstract {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @NotEmpty
    @Value("${spring.application.name}")
    protected String appName;

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        model.addAttribute("appName", this.appName);
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
}