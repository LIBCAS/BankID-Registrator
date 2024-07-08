package cz.cas.lib.bankid_registrator.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

public class ServiceAbstract implements ServiceIface
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final MessageSource messageSource;

    protected ServiceAbstract(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Get logger
     * @return 
     */
    protected final Logger getLogger() {
        return this.logger;
    }

    /**
     * Log service init debug message
     */
    protected void init() {
        getLogger().debug("Initializing service " + getClass().getSimpleName() + " ...");
    }
}
