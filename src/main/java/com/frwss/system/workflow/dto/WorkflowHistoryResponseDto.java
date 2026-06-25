// ------------------------- Stores Workflow History Response ------------------

package com.frwss.system.workflow.dto;

import java.time.LocalDateTime;

public class WorkflowHistoryResponseDto {
    private String fromState;
    private String toState;
    private String performedBy;
    private String justification;
    private LocalDateTime transitionTime;

//    Getters and Setters
    public String getFromState() {
        return fromState;
    }
    public void setFromState(String fromState) {
        this.fromState = fromState;
    }
    public String getToState() {
        return toState;
    }
    public void setToState(String toState) {
        this.toState = toState;
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
    public LocalDateTime getTransitionTime() {
        return transitionTime;
    }
    public void setTransitionTime(LocalDateTime transitionTime) {
        this.transitionTime = transitionTime;
    }


}
