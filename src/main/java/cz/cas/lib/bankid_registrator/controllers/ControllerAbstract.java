package cz.cas.lib.bankid_registrator.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author iok
 */
public abstract class ControllerAbstract {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

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