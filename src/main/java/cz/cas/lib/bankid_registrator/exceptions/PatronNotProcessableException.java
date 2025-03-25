package cz.cas.lib.bankid_registrator.exceptions;

/**
 * Exception thrown when a patron is not currently being processed.
 */
public class PatronNotProcessableException extends RuntimeException
{
    private static final String DEFAULT_MESSAGE = "Patron is not currently being processed.";

    public PatronNotProcessableException(String message)
    {
        super(message);
    }

    public PatronNotProcessableException()
    {
        super(DEFAULT_MESSAGE);
    }
}