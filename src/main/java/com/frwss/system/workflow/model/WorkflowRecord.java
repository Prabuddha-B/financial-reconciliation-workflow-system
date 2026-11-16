//-------------------------------------------------------------------------------------------------------
//                      JPA Entity Representing a  Workflow Record Information
//-------------------------------------------------------------------------------------------------------

package com.frwss.system.workflow.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_records")
public class WorkflowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    // Unique identifier of the workflow - PRIMARY KEY
    @Column(name = "workflow_id")
    private Long workflowId;

    // Identifies the originating module or record type
    @Column(name = "source_type")
    private String sourceType;

    // Identifies the original source record
    @Column(name = "source_record_id")
    private String sourceRecordId;

    // Stores the current workflow state
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state")
    private WorkflowState currentState;

    // Identifies who created the workflow
    @Column(name = "created_by")
    private String createdBy;

    // Workflow creation timestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Last update timestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Submitter's comments
    @Column(name = "comments", length = 2000)
    private String comments;

    // Additional information supplied by the source module
    @Column(name = "details", length = 2000)
    private String details;

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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
