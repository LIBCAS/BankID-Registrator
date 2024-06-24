package cz.cas.lib.bankid_registrator.controllers;

import cz.cas.lib.bankid_registrator.exceptions.HttpErrorException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainErrorController extends ControllerAbstract implements ErrorController
{
    @RequestMapping("/error")
    public void handleError(HttpServletRequest request) throws Exception {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            HttpStatus httpStatus = HttpStatus.resolve(statusCode);
            getLogger().info("HttpErrorException" + httpStatus.getReasonPhrase() + " " + httpStatus.value());

            throw new HttpErrorException(httpStatus, httpStatus.getReasonPhrase());
        } else if (exception != null) {
            Throwable throwable = (Throwable) exception;
            if (throwable instanceof Exception) {
                throw (Exception) throwable;
            } else {
                throw new Exception(throwable);
            }
        }
    }
}
