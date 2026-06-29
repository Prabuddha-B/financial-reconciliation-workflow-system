// ------------------------ Store Workflow State Update Request --------------------------------------

package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;

public class WorkflowStateUpdateRequestDto {

    private WorkflowState newState;
    private String performedBy;
    private String justification;

//    Getters and Setters
    public WorkflowState getNewState() {
        return newState;
    }
    public void setNewState(WorkflowState newState) {
        this.newState = newState;
    }
    public String getPerformedBy() {
        return performedBy;
    }
    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
    public String getJustification() {
        return justification;
    }
    public void setJustification(String justification) {
        this.justification = justification;
    }
}
