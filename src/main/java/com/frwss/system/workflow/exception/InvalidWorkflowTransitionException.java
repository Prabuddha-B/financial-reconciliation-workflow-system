// ---------------------------------------------------------------------------------------
//          Represents attempts to perform transitions not allowed by workflow rules
// ----------------------------------------------------------------------------------------
package com.frwss.system.workflow.exception;

public class InvalidWorkflowTransitionException extends RuntimeException {

    /**
     * Creates an invalid workflow transition exception with a descriptive error message.
     */
    public InvalidWorkflowTransitionException(String message) {
        super(message);
    }
}