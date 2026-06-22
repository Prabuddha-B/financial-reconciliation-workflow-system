// ---------------------------------- Handles Workflow Transition Validation Logic -----------------------------------

package com.frwss.system.workflow.validation;

import com.frwss.system.workflow.model.WorkflowState;

public class WorkflowTransitionValidator {

    // Validates Workflow State Transitions
    public static boolean isValidTransition(
            WorkflowState currentState,
            WorkflowState newState
    ) {

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