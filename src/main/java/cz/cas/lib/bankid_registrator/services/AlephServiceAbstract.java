package cz.cas.lib.bankid_registrator.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AlephServiceAbstract
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public Logger getLogger() {
        return this.logger;
    }

    public void init() {
        getLogger().debug("Initializing service: " + getClass().getSimpleName());
    }
}