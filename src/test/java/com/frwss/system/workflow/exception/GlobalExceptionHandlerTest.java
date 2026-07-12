package com.frwss.system.workflow.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Global Exception Handler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    // =========================================================================
    // handleInvalidWorkflowTransition()
    // =========================================================================

    @Test
    @DisplayName("Should return HTTP 400 for invalid workflow transition")
    void handleInvalidWorkflowTransition_returnsBadRequest() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        InvalidWorkflowTransitionException exception =
                new InvalidWorkflowTransitionException("Invalid workflow transition");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleInvalidWorkflowTransition(exception);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                "Invalid workflow transition",
                response.getBody().get("error")
        );
    }

    // =========================================================================
    // handleRuntimeException()
    // =========================================================================

    @Test
    @DisplayName("Should return HTTP 400 for runtime exception")
    void handleRuntimeException_returnsBadRequest() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        RuntimeException exception =
                new RuntimeException("Justification is required");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleRuntimeException(exception);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                "Justification is required",
                response.getBody().get("error")
        );
    }

    // =========================================================================
    // handleWorkflowNotFound()
    // =========================================================================

    @Test
    @DisplayName("Should return HTTP 404 for workflow not found")
    void handleWorkflowNotFound_returnsNotFound() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        WorkflowNotFoundException exception =
                new WorkflowNotFoundException("Workflow not found");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ResponseEntity<Map<String, String>> response =
                exceptionHandler.handleWorkflowNotFound(exception);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                "Workflow not found",
                response.getBody().get("error")
        );
    }
}