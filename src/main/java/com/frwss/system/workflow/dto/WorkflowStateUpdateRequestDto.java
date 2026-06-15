// ------------------------ Store Workflow State Update Request --------------------------------------

package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;

public class WorkflowStateUpdateRequestDto {

    private WorkflowState newState;

    public WorkflowState getNewState() {
        return newState;
    }
    public void setNewState(WorkflowState newState) {
        this.newState = newState;
    }

}
