package cz.cas.lib.bankid_registrator.exceptions;

public class IdentityAuthException extends RuntimeException
{
    private final String message;

    public IdentityAuthException()
    {
        this.message = null;
    }

    public IdentityAuthException(String message) {
        this.message = message;
    }

    public IdentityAuthException(String message, Throwable cause) {
        super(cause);
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}