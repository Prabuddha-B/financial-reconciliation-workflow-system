// ------------------------- Handles Invalid Workflow Transitions ------------------

package com.frwss.system.workflow.exception;

public class InvalidWorkflowTransitionException extends RuntimeException {

    public InvalidWorkflowTransitionException(String message) {
        super(message);
    }
}