// -------------------------------------------------------------------------------
//       Represents requests to access workflow records that do not exist
// -------------------------------------------------------------------------------
package com.frwss.system.workflow.exception;

public class WorkflowNotFoundException extends RuntimeException {

    /**
     * Creates a workflow not found exception with a descriptive error message.
     */
    public WorkflowNotFoundException(String message) {
        super(message);
    }
}
