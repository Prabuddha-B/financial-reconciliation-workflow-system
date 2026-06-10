package com.frwss.system.workflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "workflow_transition_logs")
public class WorkflowTransitionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "from_state")
    private String fromState;

    @Column(name = "to_state")
    private String toState;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "justification")
    private String justification;

    @Column(name = "transition_time")
    private LocalDateTime transitionTime;

}
