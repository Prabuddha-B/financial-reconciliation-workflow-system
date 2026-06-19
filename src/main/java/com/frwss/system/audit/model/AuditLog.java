package com.frwss.system.audit.model;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private AuditActionType actionType;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "previous_hash", nullable = false)
    private String previousHash;

    @Column(name = "current_hash", nullable = false, length =  64)
    private String currentHash;

    public Long getId(){
        return id;
    }

    public AuditActionType getActionType(){
        return actionType;
    }

    public void setActionType(AuditActionType actionType){
        this.actionType = actionType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }
}
