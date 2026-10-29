package com.frwss.system.messaging.service;

import com.frwss.system.messaging.model.Notification;
import com.frwss.system.messaging.model.NotificationType;
import com.frwss.system.messaging.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling notifications
 *
 * Key Features:
 * 1. Create notifications for various events
 * 2. Track read/unread status
 * 3. Link to relevant pages (via link field)
 * 4. Soft delete
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create a new notification
     *
     * @param recipientId Who receives this notification
     * @param senderId Who sent it (optional)
     * @param title Short title
     * @param message Detailed message
     * @param type Type of notification (MESSAGE, APPROVAL, etc.)
     * @param link URL to navigate to (optional)
     * @param entityType Type of entity referenced (optional)
     * @param entityId ID of entity referenced (optional)
     */
    @Transactional
    public Notification createNotification(
            Long recipientId,
            Long senderId,
            String title,
            String message,
            NotificationType type,
            String link,
            String entityType,
            String entityId) {

        log.info("Creating notification for user {}: {}", recipientId, title);

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setSenderId(senderId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type.name());
        notification.setLink(link);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setCreatedAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);
        log.info("✅ Notification created. ID: {}", savedNotification.getId());

        return savedNotification;
    }

    /**
     * Get all notifications for a user
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByRecipientId(userId);
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByRecipientId(userId);
    }

    /**
     * Get unread count for a user (for badge display)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByRecipientId(userId);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new RuntimeException("You are not authorized to read this notification");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification {} marked as read by user {}", notificationId, userId);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        log.info("All notifications marked as read by user {}", userId);
    }

    /**
     * Delete a notification (soft delete)
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getRecipientId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this notification");
        }

        notification.setIsDeleted(true);
        notificationRepository.save(notification);
        log.info("🗑️ Notification {} deleted by user {}", notificationId, userId);
    }

    // ========== Business-Specific Notification Methods ==========

    /**
     * Notify that a workflow approval is needed
     */
    @Transactional
    public void notifyWorkflowApproval(Long workflowId, Long approverId, String details) {
        createNotification(
                approverId,
                null,
                "Workflow Approval Needed",
                "Workflow #" + workflowId + " requires your approval. " + details,
                NotificationType.APPROVAL,
                "/workflow/status/" + workflowId,
                "WORKFLOW",
                workflowId.toString()
        );
    }

    /**
     * Notify that reconciliation is complete
     */
    @Transactional
    public void notifyReconciliationComplete(Long userId, int exactCount, int partialCount, int orphanCount) {
        createNotification(
                userId,
                null,
                "Reconciliation Complete",
                String.format("Reconciliation completed! Exact: %d, Partial: %d, Orphans: %d",
                        exactCount, partialCount, orphanCount),
                NotificationType.RECONCILIATION,
                "/reconciliation",
                "RECONCILIATION",
                null
        );
    }

    /**
     * Notify that a record has been approved
     */
    @Transactional
    public void notifyRecordApproved(Long userId, String recordId, String recordType) {
        createNotification(
                userId,
                null,
                "Record Approved",
                recordType + " record " + recordId + " has been approved.",
                NotificationType.APPROVED,
                "/reconciliation",
                recordType,
                recordId
        );
    }

    /**
     * Notify that a record has been rejected
     */
    @Transactional
    public void notifyRecordRejected(Long userId, String recordId, String recordType) {
        createNotification(
                userId,
                null,
                "❌ Record Rejected",
                recordType + " record " + recordId + " has been rejected.",
                NotificationType.REJECTED,
                "/reconciliation",
                recordType,
                recordId
        );
    }

    /**
     * Send a system reminder notification
     */
    @Transactional
    public void notifySystemReminder(Long userId, String title, String message) {
        createNotification(
                userId,
                null,
                "🔔 " + title,
                message,
                NotificationType.REMINDER,
                null,
                "SYSTEM",
                null
        );
    }

    /**
     * Notify about a new file upload
     */
    @Transactional
    public void notifyUploadComplete(Long userId, String filename, String uploader) {
        createNotification(
                userId,
                null,
                "File Upload Complete",
                "File '" + filename + "' has been uploaded by " + uploader + ".",
                NotificationType.UPLOAD,
                "/ingestion/dashboard",
                "UPLOAD",
                filename
        );
    }
}