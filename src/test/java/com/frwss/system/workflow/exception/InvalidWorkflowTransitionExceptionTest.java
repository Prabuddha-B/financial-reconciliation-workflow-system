package com.frwss.system.workflow.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Invalid Workflow Transition Exception Unit Tests")
class InvalidWorkflowTransitionExceptionTest {
    @Test
    @DisplayName("Should preserve message and runtime exception inheritance")
    void invalidWorkflowTransitionException_message_preservesMessage() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        String message = "Invalid transition";

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        InvalidWorkflowTransitionException exception = new InvalidWorkflowTransitionException(message);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(message, exception.getMessage()); assertTrue(exception instanceof RuntimeException);
    }
}
