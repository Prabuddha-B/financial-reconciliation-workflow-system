package com.frwss.system.messaging.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification entity - System notifications for users
 *
 * Types:
 * - MESSAGE: New message received
 * - APPROVAL: Workflow approval needed
 * - REMINDER: System reminder
 * - SYSTEM: System notification
 * - TASK: Task assigned
 * - RECONCILIATION: Reconciliation complete
 * - WORKFLOW: Workflow state change
 */
@Entity
@Table(name = "notifications")
public class Notification {

    // ========== Fields ==========

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;           // Who receives this notification

    @Column(name = "sender_id")
    private Long senderId;              // Who sent it (optional - for messages)

    @Column(nullable = false, length = 255)
    private String title;               // Short title (e.g., "New Message")

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;             // Detailed message

    @Column(nullable = false, length = 50)
    private String type;                // MESSAGE, APPROVAL, REMINDER, etc.

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;     // Has user seen it?

    @Column(length = 500)
    private String link;                // URL to navigate to

    @Column(name = "entity_type", length = 50)
    private String entityType;          // WORKFLOW, RECONCILIATION, etc.

    @Column(name = "entity_id", length = 100)
    private String entityId;            // ID of the entity

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;  // Soft delete

    // ========== Lifecycle Callbacks ==========

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        if (this.isRead == null) this.isRead = false;
        if (this.isDeleted == null) this.isDeleted = false;
    }

    // ========== Getters and Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}