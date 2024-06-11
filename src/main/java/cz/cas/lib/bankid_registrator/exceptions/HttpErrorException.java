package cz.cas.lib.bankid_registrator.exceptions;

import org.springframework.http.HttpStatus;

public class HttpErrorException extends RuntimeException
{
    private final HttpStatus status;
    private final String errorMessage;

    public HttpErrorException(HttpStatus status, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
