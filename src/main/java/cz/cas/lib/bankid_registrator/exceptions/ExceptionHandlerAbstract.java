package cz.cas.lib.bankid_registrator.exceptions;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandlerAbstract
{
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Logger getLogger() {
        return this.logger;
    }
}