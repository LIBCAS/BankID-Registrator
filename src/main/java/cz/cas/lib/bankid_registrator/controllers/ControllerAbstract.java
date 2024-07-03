package cz.cas.lib.bankid_registrator.controllers;

import javax.validation.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;

/**
 *
 * @author iok
 */
public abstract class ControllerAbstract {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MessageSource messageSource;

    @NotEmpty
    @Value("${spring.application.name}")
    protected String appName;

    public ControllerAbstract(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * 
     * @return 
     */
    protected final Logger getLogger() {
        return this.logger;
    }

    protected void init() {
        getLogger().debug("initializing controller " + getClass().getSimpleName() + " ...");
    }
}