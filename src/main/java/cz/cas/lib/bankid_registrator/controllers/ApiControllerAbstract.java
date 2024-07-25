package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.configurations.ApiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * Abstract controller for API controllers
 */
public abstract class ApiControllerAbstract
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MessageSource messageSource;
    protected final ApiConfig apiConfig;

    public ApiControllerAbstract(MessageSource messageSource, ApiConfig apiConfig) {
        this.messageSource = messageSource;
        this.apiConfig = apiConfig;
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