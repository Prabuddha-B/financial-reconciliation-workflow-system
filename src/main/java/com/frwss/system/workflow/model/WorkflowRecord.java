package com.frwss.system.workflow.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_records")
public class WorkflowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_record_id")
    private String sourceRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_state")
    private WorkflowState currentState;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
