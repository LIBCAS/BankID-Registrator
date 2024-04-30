package cz.cas.lib.bankid_registrator.exceptions;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("error", e.getMessage());
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
        return ResponseEntity.badRequest().body(result);
    }
}