package cz.cas.lib.bankid_registrator.exceptions;

public class AmbiguousPatronMatchException extends RuntimeException {
    public AmbiguousPatronMatchException() {
        super("Multiple Aleph patrons match the verified name and birth date");
    }
}
