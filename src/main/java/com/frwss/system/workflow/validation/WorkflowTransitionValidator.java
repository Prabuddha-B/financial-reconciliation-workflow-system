// ---------------------------------- Handles Workflow Transition Validation Logic -----------------------------------

package com.frwss.system.workflow.validation;

import com.frwss.system.workflow.model.WorkflowState;

public class WorkflowTransitionValidator {

//    Validates Workflow State
    public static boolean isValidTransition(WorkflowState currentState, WorkflowState newState){
        if(currentState == WorkflowState.ENTERED && newState == WorkflowState.REVIEWED) return  true;
        if(currentState == WorkflowState.REVIEWED && newState == WorkflowState.VERIFIED) return  true;
        if(currentState == WorkflowState.VERIFIED && newState == WorkflowState.REJECTED) return  true;
        return false;
    }
}
