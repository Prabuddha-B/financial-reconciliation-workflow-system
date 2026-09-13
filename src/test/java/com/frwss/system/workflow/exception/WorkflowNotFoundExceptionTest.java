package com.frwss.system.workflow.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Workflow Not Found Exception Unit Tests")
class WorkflowNotFoundExceptionTest {
    @Test
    @DisplayName("Should preserve message and runtime exception inheritance")
    void workflowNotFoundException_message_preservesMessage() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        String message = "Workflow not found";

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        WorkflowNotFoundException exception = new WorkflowNotFoundException(message);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(message, exception.getMessage()); assertTrue(exception instanceof RuntimeException);
    }
}
