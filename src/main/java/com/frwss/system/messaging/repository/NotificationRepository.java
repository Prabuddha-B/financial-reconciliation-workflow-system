package com.frwss.system.messaging.repository;

import com.frwss.system.messaging.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * Get all notifications for a user
     */
    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :userId 
        AND n.isDeleted = false 
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByRecipientId(@Param("userId") Long userId);

    /**
     * Get unread notifications for a user
     */
    @Query("""
        SELECT n FROM Notification n 
        WHERE n.recipientId = :userId 
        AND n.isRead = false 
        AND n.isDeleted = false
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findUnreadByRecipientId(@Param("userId") Long userId);

    /**
     * Count unread notifications for a user
     */
    @Query("""
        SELECT COUNT(n) FROM Notification n 
        WHERE n.recipientId = :userId 
        AND n.isRead = false 
        AND n.isDeleted = false
    """)
    long countUnreadByRecipientId(@Param("userId") Long userId);

    /**
     * Mark all notifications as read for a user
     */
    @Modifying
    @Query("""
        UPDATE Notification n 
        SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP 
        WHERE n.recipientId = :userId AND n.isRead = false
    """)
    void markAllAsRead(@Param("userId") Long userId);
}
