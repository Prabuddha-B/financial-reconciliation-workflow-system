// -------------------------------------------------------------------------------
//       Converts application exceptions into consistent HTTP error responses
// -------------------------------------------------------------------------------

package com.frwss.system.workflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Returns a bad request response when an invalid workflow transition is attempted.
     */
    @ExceptionHandler(InvalidWorkflowTransitionException.class)
    public ResponseEntity<Map<String, String>> handleInvalidWorkflowTransition(InvalidWorkflowTransitionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles runtime exceptions and returns their messages as bad request responses.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    /**
     * Returns a not found response when the requested workflow does not exist.
     */
    @ExceptionHandler(WorkflowNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleWorkflowNotFound(WorkflowNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}