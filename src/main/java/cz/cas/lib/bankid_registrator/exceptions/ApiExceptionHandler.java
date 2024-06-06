package cz.cas.lib.bankid_registrator.exceptions;

import cz.cas.lib.bankid_registrator.controllers.ApiController;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;

/**
 * ApiExceptionHandler is an exception handler for the API routes/controller.
 */
@ControllerAdvice(assignableTypes = {ApiController.class})
public class ApiExceptionHandler extends ExceptionHandlerAbstract
{
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", e.getMessage());
        getLogger().error("Exception: " + e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * Handles the case when a request is not processable due to the user not being currently processed.
     * @param e
     * @return
     */
    @ExceptionHandler(PatronNotProcessableException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotProcessableException(PatronNotProcessableException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", e.getMessage());
        getLogger().error("Exception: " + e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }
}