package cz.cas.lib.bankid_registrator.exceptions;

import org.springframework.http.HttpStatus;

public class HttpErrorException extends RuntimeException
{
    private final HttpStatus status;
    private final String message;

    public HttpErrorException(HttpStatus status)
    {
        this.status = status;
        this.message = null;
    }

    public HttpErrorException(HttpStatus status, String message)
    {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus()
    {
        return this.status;
    }

    public String getMessage()
    {
        return this.message;
    }
}
