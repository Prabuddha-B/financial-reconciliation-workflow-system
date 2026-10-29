package com.frwss.system.messaging.service;

import com.frwss.system.messaging.model.Message;
import com.frwss.system.messaging.model.NotificationType;
import com.frwss.system.messaging.repository.MessageRepository;
import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling messages between users
 *
 * Key Features:
 * 1. Send messages between users
 * 2. Track read/unread status
 * 3. Support for replies (threaded conversations)
 * 4. Soft delete (each user can delete from their view)
 * 5. Reference to other entities (reconciliation, workflow)
 */
@Service
public class MessageService {

    // ✅ FIXED: Using SLF4J Logger (org.slf4j.Logger)
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public MessageService(
            MessageRepository messageRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Send a message from one user to another
     *
     * @param senderId ID of the sender (Long)
     * @param recipientId ID of the recipient (Long)
     * @param subject Message subject
     * @param content Message body
     * @param referenceType Optional: RECONCILIATION, WORKFLOW, etc.
     * @param referenceId Optional: ID of referenced entity
     * @return The saved message
     */
    @Transactional
    public Message sendMessage(Long senderId, Long recipientId, String subject, String content,
                               String referenceType, String referenceId) {

        // ✅ FIXED: Correct log format with {} placeholders
        log.info("📨 Sending message from user {} to user {}", senderId, recipientId);

        // Validate sender exists
        User sender = userRepository.findById(senderId.intValue())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        // Validate recipient exists
        User recipient = userRepository.findById(recipientId.intValue())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        // Create message
        Message message = new Message();
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setSubject(subject);
        message.setContent(content);
        message.setReferenceType(referenceType);
        message.setReferenceId(referenceId);
        message.setSentAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        // Create notification for recipient
        notificationService.createNotification(
                recipientId,
                senderId,
                "📩 New Message from " + sender.getUsername(),
                "You have a new message: " + subject,
                NotificationType.MESSAGE,
                "/messages/view/" + savedMessage.getId(),
                "Message",
                savedMessage.getId().toString()
        );

        // ✅ FIXED: Correct log format
        log.info("✅ Message sent successfully. ID: {}", savedMessage.getId());

        return savedMessage;
    }

    /**
     * Get inbox messages for a user (all messages received)
     */
    @Transactional(readOnly = true)
    public List<Message> getInbox(Long userId) {
        return messageRepository.findInboxByUserId(userId);
    }

    /**
     * Get sent messages by a user
     */
    @Transactional(readOnly = true)
    public List<Message> getSentMessages(Long userId) {
        return messageRepository.findSentByUserId(userId);
    }

    /**
     * Get a single message by ID
     */
    @Transactional(readOnly = true)
    public Message getMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
    }

    /**
     * Mark a message as read (only recipient can do this)
     */
    @Transactional
    public void markAsRead(Long messageId, Long userId) {
        Message message = getMessage(messageId);

        // Verify the user is the recipient
        if (!message.getRecipientId().equals(userId)) {
            throw new RuntimeException("You are not authorized to read this message");
        }

        if (!message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
            log.info("✅ Message {} marked as read by user {}", messageId, userId);
        }
    }

    /**
     * Mark all messages as read for a user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        messageRepository.markAllAsRead(userId);
        log.info("✅ All messages marked as read by user {}", userId);
    }

    /**
     * Get unread count for a user (for badge display)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return messageRepository.countUnreadByUserId(userId);
    }

    /**
     * Get conversation between two users (threaded view)
     */
    @Transactional(readOnly = true)
    public List<Message> getConversation(Long user1, Long user2) {
        return messageRepository.findConversation(user1, user2);
    }

    /**
     * Delete a message (soft delete - only for the current user's view)
     */
    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = getMessage(messageId);

        if (message.getSenderId().equals(userId)) {
            message.setIsDeletedBySender(true);
        } else if (message.getRecipientId().equals(userId)) {
            message.setIsDeletedByRecipient(true);
        } else {
            throw new RuntimeException("You are not authorized to delete this message");
        }

        messageRepository.save(message);
        log.info("🗑️ Message {} deleted by user {}", messageId, userId);
    }

    /**
     * Reply to a message (creates a new message with parent reference)
     */
    @Transactional
    public Message replyToMessage(Long parentMessageId, Long senderId, String content) {
        Message parentMessage = getMessage(parentMessageId);

        return sendMessage(
                senderId,
                parentMessage.getSenderId(),
                "Re: " + parentMessage.getSubject(),
                content,
                parentMessage.getReferenceType(),
                parentMessage.getReferenceId()
        );
    }

    /**
     * Send a message with a reference to a reconciliation record
     * Useful for: "Please review this reconciliation record"
     */
    @Transactional
    public Message sendReconciliationMessage(Long senderId, Long recipientId, Long reconciliationId, String comment) {
        String subject = "Review needed for reconciliation record #" + reconciliationId;
        String content = "Please review reconciliation record " + reconciliationId + ".\n\n" +
                "Comment: " + comment + "\n\n" +
                "Go to: /reconciliation/manual-review";

        return sendMessage(
                senderId,
                recipientId,
                subject,
                content,
                "RECONCILIATION",
                reconciliationId.toString()
        );
    }

    /**
     * Send a message with a reference to a workflow approval
     * Useful for: "Please approve this workflow"
     */
    @Transactional
    public Message sendWorkflowMessage(Long senderId, Long recipientId, Long workflowId, String comment) {
        String subject = "Workflow approval needed #" + workflowId;
        String content = "Please review and approve workflow " + workflowId + ".\n\n" +
                "Comment: " + comment + "\n\n" +
                "Go to: /workflow/approvals";

        return sendMessage(
                senderId,
                recipientId,
                subject,
                content,
                "WORKFLOW",
                workflowId.toString()
        );
    }
}