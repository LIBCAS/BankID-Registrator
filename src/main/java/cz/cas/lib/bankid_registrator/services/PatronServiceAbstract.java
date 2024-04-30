package cz.cas.lib.bankid_registrator.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatronServiceAbstract {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @return 
     */
    protected final Logger getLogger() {
        return this.logger;
    }

    protected void init() {
        getLogger().debug("Initializing service" + getClass().getSimpleName() + "...");
    }
}
