package com.frwss.system.messaging.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Message entity - Represents a message sent between users
 *
 * Features:
 * - Supports replies via parent_message_id
 * - Tracks read status
 * - Soft delete (users can delete from their view only)
 * - References to other entities (reconciliation, workflow)
 */
@Entity
@Table(name = "messages")
public class Message {

    // ========== Fields ==========

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;              // Who sent the message

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;            // Who received the message

    @Column(nullable = false, length = 255)
    private String subject;              // Message subject line

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;              // Message body (can be long)

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;      // Has recipient read it?

    @Column(name = "parent_message_id")
    private Long parentMessageId;        // For replies (threaded conversations)

    @Column(name = "reference_type", length = 50)
    private String referenceType;        // RECONCILIATION, WORKFLOW, etc.

    @Column(name = "reference_id", length = 100)
    private String referenceId;          // ID of the referenced record

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;        // When it was sent

    @Column(name = "read_at")
    private LocalDateTime readAt;        // When it was read

    @Column(name = "is_deleted_by_sender")
    private Boolean isDeletedBySender = false;   // Soft delete for sender

    @Column(name = "is_deleted_by_recipient")
    private Boolean isDeletedByRecipient = false; // Soft delete for recipient

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== Lifecycle Callbacks ==========

    /**
     * Automatically set timestamps when entity is created
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.sentAt = now;
        // Set default values
        if (this.isRead == null) this.isRead = false;
        if (this.isDeletedBySender == null) this.isDeletedBySender = false;
        if (this.isDeletedByRecipient == null) this.isDeletedByRecipient = false;
    }

    /**
     * Automatically update timestamp when entity is updated
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Getters and Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Long getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(Long parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public Boolean getIsDeletedBySender() {
        return isDeletedBySender;
    }

    public void setIsDeletedBySender(Boolean isDeletedBySender) {
        this.isDeletedBySender = isDeletedBySender;
    }

    public Boolean getIsDeletedByRecipient() {
        return isDeletedByRecipient;
    }

    public void setIsDeletedByRecipient(Boolean isDeletedByRecipient) {
        this.isDeletedByRecipient = isDeletedByRecipient;
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
}