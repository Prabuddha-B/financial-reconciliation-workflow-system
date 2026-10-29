package com.frwss.system.messaging.repository;

import com.frwss.system.messaging.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity
 * Handles database operations for messages
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    /**
     * Get inbox messages for a user (excluding deleted)
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE m.recipientId = :userId 
        AND m.isDeletedByRecipient = false 
        ORDER BY m.sentAt DESC
    """)
    List<Message> findInboxByUserId(@Param("userId") Long userId);

    /**
     * Get set messages by a user (excluding deleted)
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE m.senderId = :userId 
        AND m.isDeletedBySender = false 
        ORDER BY m.sentAt DESC
    """)
    List<Message> findSentByUserId(@Param("userId") Long userId);

    /**
     * Count unread messages for a user
     */
    @Query("""
        SELECT COUNT(m) FROM Message m 
        WHERE m.recipientId = :userId 
        AND m.isRead = false 
        AND m.isDeletedByRecipient = false
    """)
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Get conversation between two users (threaded view)
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE (m.senderId = :user1 AND m.recipientId = :user2 
               OR m.senderId = :user2 AND m.recipientId = :user1)
        AND m.isDeletedBySender = false 
        AND m.isDeletedByRecipient = false
        ORDER BY m.sentAt ASC
    """)
    List<Message> findConversation(@Param("user1") Long user1, @Param("user2") Long user2);

    /**
     * Get messages by reference (e.g., all messages about a reconciliation)
     */
    @Query("""
        SELECT m FROM Message m 
        WHERE m.referenceType = :referenceType 
        AND m.referenceId = :referenceId
        ORDER BY m.sentAt DESC
    """)
    List<Message> findByReference(
            @Param("referenceType") String referenceType,
            @Param("referenceId") String referenceId
    );

    /**
     * Mark all messages as read for a user
     */
    @Modifying
    @Query("""
        UPDATE Message m 
        SET m.isRead = true, m.readAt = CURRENT_TIMESTAMP 
        WHERE m.recipientId = :userId AND m.isRead = false
    """)
    void markAllAsRead(@Param("userId") Long userId);


}
