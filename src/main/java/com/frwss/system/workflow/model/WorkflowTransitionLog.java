// --------------------------------------------------------------------------------------------------------------
//                                  Stores Workflow State Transition History
// --------------------------------------------------------------------------------------------------------------
package com.frwss.system.workflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "workflow_transition_logs")
public class WorkflowTransitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Unique identifier for each transition log --- PRIMARY KEY
    @Column(name = "log_id")
    private Long logId;

    // Identifies the associated workflow
    @Column(name = "workflow_id")
    private Long workflowId;

    // Previous workflow state
    @Column(name = "from_state")
    private String fromState;

    // New workflow state
    @Column(name = "to_state")
    private String toState;

    // User who performed the transition
    @Column(name = "performed_by")
    private String performedBy;

    // Reason supplied for the transition
    @Column(name = "justification")
    private String justification;

    // Date and time of the transition
    @Column(name = "transition_time")
    private LocalDateTime transitionTime;

//    Getters and Setters
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

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
