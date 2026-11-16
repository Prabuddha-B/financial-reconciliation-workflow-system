// -------------------------------------------------------------------------------------------------------------------
//             Validates whether a requested workflow state transition follows the defined workflow rules
// -------------------------------------------------------------------------------------------------------------------

package com.frwss.system.workflow.validation;

import com.frwss.system.workflow.model.WorkflowState;

public class WorkflowTransitionValidator {

    /**
     * Checks whether the transition from the current stat to the requested new state is permitted.
     */
    public static boolean isValidTransition(WorkflowState currentState, WorkflowState newState) {

        // ENTERED -> REVIEWED
        if (currentState == WorkflowState.ENTERED && newState == WorkflowState.REVIEWED) {
            return true;
        }

        // REVIEWED -> VERIFIED
        if (currentState == WorkflowState.REVIEWED && newState == WorkflowState.VERIFIED) {
            return true;
        }

        // REVIEWED -> REJECTED
        if (currentState == WorkflowState.REVIEWED && newState == WorkflowState.REJECTED) {
            return true;
        }

        return false;
    }
}