//--------------------------- Transfers Workflow Response Data -----------------------------------------

package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class WorkflowResponseDto {

    private Long workflowId;

    private String sourceType;

    private String sourceRecordId;

    private WorkflowState currentState;

    private String message;

    private String createdBy;

    private String comments;

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

    /**
     * The details split into display lines. Details written one fact per line are split on line
     * breaks. Older records were written as a single line with " | " between the facts, so those
     * are split on that instead. Empty parts are dropped. Not a getter on purpose, so it is not
     * added to the JSON the API returns.
     */
    public List<String> detailLines() {
        if (details == null || details.isBlank()) {
            return List.of();
        }
        String[] parts = details.contains("\n")
                ? details.split("\\r?\\n")
                : details.split(" \\| ");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
    }

    /** The first display line, used as the one-line summary in the approvals list. */
    public String detailsSummary() {
        List<String> lines = detailLines();
        return lines.isEmpty() ? "" : lines.get(0);
    }
}