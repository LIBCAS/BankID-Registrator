package cz.cas.lib.bankid_registrator.exceptions;

/**
 * Exception thrown when a patron doesnt exist in the DB
 */
public class PatronNotFoundException extends RuntimeException
{
    private static final String DEFAULT_MESSAGE = "Patron not found.";

    public PatronNotFoundException(String message)
    {
        super(message);
    }

    public PatronNotFoundException()
    {
        super(DEFAULT_MESSAGE);
    }
}