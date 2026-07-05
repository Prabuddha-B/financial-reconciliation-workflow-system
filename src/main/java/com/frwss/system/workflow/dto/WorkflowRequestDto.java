//-------------------------------- Transfers Workflow Request Data -------------------------------

package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;

public class WorkflowRequestDto {
    private String sourceType;
    private String sourceRecordId;
    private String createdBy;
    private String comments;
    private WorkflowState targetState;

//    Getters and Setters
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public WorkflowState getTargetState() {
        return targetState;
    }

    public void setTargetState(WorkflowState targetState) {
        this.targetState = targetState;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
