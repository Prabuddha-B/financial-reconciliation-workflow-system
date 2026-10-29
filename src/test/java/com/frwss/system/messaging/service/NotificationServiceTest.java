package com.frwss.system.messaging.service;

import com.frwss.system.messaging.model.Notification;
import com.frwss.system.messaging.model.NotificationType;
import com.frwss.system.messaging.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // createNotification()
    // =========================================================================

    @Test
    @DisplayName("Should build and save a notification with all supplied fields")
    void createNotification_validInput_savesNotificationWithAllFields() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(1L);
            return n;
        });

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Notification result = notificationService.createNotification(
                10L, 5L, "New Message", "You have mail", NotificationType.MESSAGE,
                "/messages/view/1", "Message", "1");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(10L, saved.getRecipientId());
        assertEquals(5L, saved.getSenderId());
        assertEquals("New Message", saved.getTitle());
        assertEquals("You have mail", saved.getMessage());
        assertEquals("MESSAGE", saved.getType());
        assertEquals("/messages/view/1", saved.getLink());
        assertEquals("Message", saved.getEntityType());
        assertEquals("1", saved.getEntityId());
        assertNotNull(saved.getCreatedAt());

        assertEquals(1L, result.getId());
    }

    // =========================================================================
    // getNotifications() / getUnreadNotifications() / getUnreadCount()
    // =========================================================================

    @Test
    @DisplayName("Should return the repository's full notification list for a user")
    void getNotifications_delegatesToRepository() {

        when(notificationRepository.findByRecipientId(10L)).thenReturn(List.of(new Notification()));

        List<Notification> result = notificationService.getNotifications(10L);

        assertEquals(1, result.size());
        verify(notificationRepository).findByRecipientId(10L);
    }

    @Test
    @DisplayName("Should return the repository's unread notification list for a user")
    void getUnreadNotifications_delegatesToRepository() {

        when(notificationRepository.findUnreadByRecipientId(10L)).thenReturn(List.of(new Notification()));

        List<Notification> result = notificationService.getUnreadNotifications(10L);

        assertEquals(1, result.size());
        verify(notificationRepository).findUnreadByRecipientId(10L);
    }

    @Test
    @DisplayName("Should return the repository's unread count for a user")
    void getUnreadCount_delegatesToRepository() {

        when(notificationRepository.countUnreadByRecipientId(10L)).thenReturn(4L);

        long result = notificationService.getUnreadCount(10L);

        assertEquals(4L, result);
    }

    // =========================================================================
    // markAsRead()
    // =========================================================================

    @Test
    @DisplayName("Should mark an unread notification as read when called by its recipient")
    void markAsRead_byRecipientUnread_marksReadAndSaves() {

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRecipientId(10L);
        notification.setIsRead(false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 10L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        assertTrue(captor.getValue().getIsRead());
        assertNotNull(captor.getValue().getReadAt());
    }

    @Test
    @DisplayName("Should not save again when the notification is already marked as read")
    void markAsRead_alreadyRead_doesNotSaveAgain() {

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRecipientId(10L);
        notification.setIsRead(true);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L, 10L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should refuse to mark a notification as read by anyone other than the recipient")
    void markAsRead_byNonRecipient_throwsRuntimeException() {

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRecipientId(10L);
        notification.setIsRead(false);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(1L, 999L));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when marking a non-existent notification as read")
    void markAsRead_notificationMissing_throwsRuntimeException() {

        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(999L, 10L));
    }

    // =========================================================================
    // markAllAsRead()
    // =========================================================================

    @Test
    @DisplayName("Should delegate markAllAsRead to the repository")
    void markAllAsRead_delegatesToRepository() {

        notificationService.markAllAsRead(10L);

        verify(notificationRepository).markAllAsRead(10L);
    }

    // =========================================================================
    // deleteNotification()
    // =========================================================================

    @Test
    @DisplayName("Should soft-delete a notification when called by its recipient")
    void deleteNotification_byRecipient_setsDeletedFlag() {

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRecipientId(10L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(1L, 10L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        assertTrue(captor.getValue().getIsDeleted());
    }

    @Test
    @DisplayName("Should refuse to delete a notification for anyone other than the recipient")
    void deleteNotification_byNonRecipient_throwsRuntimeException() {

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setRecipientId(10L);

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(RuntimeException.class, () -> notificationService.deleteNotification(1L, 999L));

        verify(notificationRepository, never()).save(any());
    }

    // =========================================================================
    // Business-specific notification helpers
    // =========================================================================

    @Test
    @DisplayName("Should create an APPROVAL notification for a pending workflow")
    void notifyWorkflowApproval_savesApprovalNotification() {

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyWorkflowApproval(55L, 10L, "2 items pending");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(10L, saved.getRecipientId());
        assertEquals(NotificationType.APPROVAL.name(), saved.getType());
        assertEquals("/workflow/status/55", saved.getLink());
        assertEquals("WORKFLOW", saved.getEntityType());
        assertEquals("55", saved.getEntityId());
        assertTrue(saved.getMessage().contains("55"));
        assertTrue(saved.getMessage().contains("2 items pending"));
    }

    @Test
    @DisplayName("Should create a RECONCILIATION notification summarizing the run counts")
    void notifyReconciliationComplete_savesReconciliationNotificationWithCounts() {

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyReconciliationComplete(10L, 5, 2, 1);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.RECONCILIATION.name(), saved.getType());
        assertEquals("/reconciliation", saved.getLink());
        assertTrue(saved.getMessage().contains("Exact: 5"));
        assertTrue(saved.getMessage().contains("Partial: 2"));
        assertTrue(saved.getMessage().contains("Orphans: 1"));
    }

    @Test
    @DisplayName("Should create an APPROVED notification referencing the record type and id")
    void notifyRecordApproved_savesApprovedNotification() {

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyRecordApproved(10L, "ACC-001", "AccountingRecord");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.APPROVED.name(), saved.getType());
        assertEquals("AccountingRecord", saved.getEntityType());
        assertEquals("ACC-001", saved.getEntityId());
    }

    @Test
    @DisplayName("Should create a REJECTED notification referencing the record type and id")
    void notifyRecordRejected_savesRejectedNotification() {

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyRecordRejected(10L, "ACC-002", "AccountingRecord");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.REJECTED.name(), saved.getType());
        assertEquals("AccountingRecord", saved.getEntityType());
        assertEquals("ACC-002", saved.getEntityId());
    }

    @Test
    @DisplayName("Should create a REMINDER notification with no link or entity reference")
    void notifySystemReminder_savesReminderNotification() {

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifySystemReminder(10L, "Renew your password", "It expires soon");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.REMINDER.name(), saved.getType());
        assertNull(saved.getLink());
        assertEquals("SYSTEM", saved.getEntityType());
        assertTrue(saved.getTitle().contains("Renew your password"));
    }

    @Test
    @DisplayName("Should create an UPLOAD notification referencing the filename and uploader")
    void notifyUploadComplete_savesUploadNotification() {

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyUploadComplete(10L, "payroll-july.csv", "admin");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(NotificationType.UPLOAD.name(), saved.getType());
        assertEquals("payroll-july.csv", saved.getEntityId());
        assertTrue(saved.getMessage().contains("payroll-july.csv"));
        assertTrue(saved.getMessage().contains("admin"));
    }
}
