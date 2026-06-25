//--------------------------- Transfers Workflow Response Data -----------------------------------------

package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;


public class WorkflowResponseDto {

    private Long workflowId;

    private String sourceType;

    private String sourceRecordId;

    private WorkflowState currentState;

    private String message;

    private String createdBy;

//    Getters and Setters

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceRecordId() {
        return sourceRecordId;
    }

    public void setSourceRecordId(String sourceRecordId) {
        this.sourceRecordId = sourceRecordId;
    }

    public WorkflowState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(WorkflowState currentState) {
        this.currentState = currentState;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}