package cz.cas.lib.bankid_registrator.exceptions;

public class IdentityAuthException extends RuntimeException
{
    public IdentityAuthException(String message) {
        super(message);
    }

    public IdentityAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}