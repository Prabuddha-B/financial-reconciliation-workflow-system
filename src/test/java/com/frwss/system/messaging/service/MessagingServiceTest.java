package com.frwss.system.messaging.service;

import com.frwss.system.messaging.model.Message;
import com.frwss.system.messaging.model.NotificationType;
import com.frwss.system.messaging.repository.MessageRepository;
import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
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
@DisplayName("Message Service Unit Tests")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // sendMessage()
    // =========================================================================

    @Test
    @DisplayName("Should save the message and notify the recipient when sender and recipient both exist")
    void sendMessage_validSenderAndRecipient_savesMessageAndNotifiesRecipient() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        User sender = user(1, "alice");
        User recipient = user(2, "bob");

        when(userRepository.findById(1)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(100L);
            return m;
        });

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Message result = messageService.sendMessage(
                1L, 2L, "Hello", "Just checking in", "RECONCILIATION", "ACC-001");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        Message saved = captor.getValue();
        assertEquals(1L, saved.getSenderId());
        assertEquals(2L, saved.getRecipientId());
        assertEquals("Hello", saved.getSubject());
        assertEquals("Just checking in", saved.getContent());
        assertEquals("RECONCILIATION", saved.getReferenceType());
        assertEquals("ACC-001", saved.getReferenceId());
        assertNotNull(saved.getSentAt());

        assertEquals(100L, result.getId());

        verify(notificationService).createNotification(
                eq(2L),
                eq(1L),
                eq("📩 New Message from alice"),
                eq("You have a new message: Hello"),
                eq(NotificationType.MESSAGE),
                eq("/messages/view/100"),
                eq("Message"),
                eq("100")
        );
    }

    @Test
    @DisplayName("Should throw and never save when the sender does not exist")
    void sendMessage_senderNotFound_throwsRuntimeExceptionAndSkipsSave() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        assertThrows(RuntimeException.class, () ->
                messageService.sendMessage(1L, 2L, "Hello", "Body", null, null));

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should throw and never save when the recipient does not exist")
    void sendMessage_recipientNotFound_throwsRuntimeExceptionAndSkipsSave() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        User sender = user(1, "alice");

        when(userRepository.findById(1)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        assertThrows(RuntimeException.class, () ->
                messageService.sendMessage(1L, 2L, "Hello", "Body", null, null));

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    // =========================================================================
    // getInbox() / getSentMessages() / getConversation()
    // =========================================================================

    @Test
    @DisplayName("Should return the repository's inbox result for a user")
    void getInbox_delegatesToRepository() {

        Message message = new Message();
        when(messageRepository.findInboxByUserId(5L)).thenReturn(List.of(message));

        List<Message> result = messageService.getInbox(5L);

        assertEquals(1, result.size());
        verify(messageRepository).findInboxByUserId(5L);
    }

    @Test
    @DisplayName("Should return the repository's sent-messages result for a user")
    void getSentMessages_delegatesToRepository() {

        Message message = new Message();
        when(messageRepository.findSentByUserId(5L)).thenReturn(List.of(message));

        List<Message> result = messageService.getSentMessages(5L);

        assertEquals(1, result.size());
        verify(messageRepository).findSentByUserId(5L);
    }

    @Test
    @DisplayName("Should return the repository's threaded conversation between two users")
    void getConversation_delegatesToRepository() {

        when(messageRepository.findConversation(1L, 2L)).thenReturn(List.of(new Message()));

        List<Message> result = messageService.getConversation(1L, 2L);

        assertEquals(1, result.size());
        verify(messageRepository).findConversation(1L, 2L);
    }

    // =========================================================================
    // getMessage()
    // =========================================================================

    @Test
    @DisplayName("Should return the message when it exists")
    void getMessage_exists_returnsMessage() {

        Message message = new Message();
        message.setId(10L);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        Message result = messageService.getMessage(10L);

        assertEquals(10L, result.getId());
    }

    @Test
    @DisplayName("Should throw when the message does not exist")
    void getMessage_missing_throwsRuntimeException() {

        when(messageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> messageService.getMessage(999L));
    }

    // =========================================================================
    // markAsRead()
    // =========================================================================

    @Test
    @DisplayName("Should mark an unread message as read when called by the recipient")
    void markAsRead_byRecipientUnread_marksReadAndSaves() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Message message = new Message();
        message.setId(10L);
        message.setRecipientId(2L);
        message.setIsRead(false);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        messageService.markAsRead(10L, 2L);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        assertTrue(captor.getValue().getIsRead());
        assertNotNull(captor.getValue().getReadAt());
    }

    @Test
    @DisplayName("Should not save again when the message is already marked as read")
    void markAsRead_alreadyRead_doesNotSaveAgain() {

        Message message = new Message();
        message.setId(10L);
        message.setRecipientId(2L);
        message.setIsRead(true);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        messageService.markAsRead(10L, 2L);

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should refuse to mark a message as read by anyone other than the recipient")
    void markAsRead_byNonRecipient_throwsRuntimeException() {

        Message message = new Message();
        message.setId(10L);
        message.setRecipientId(2L);
        message.setIsRead(false);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThrows(RuntimeException.class, () -> messageService.markAsRead(10L, 999L));

        verify(messageRepository, never()).save(any());
    }

    // =========================================================================
    // markAllAsRead() / getUnreadCount()
    // =========================================================================

    @Test
    @DisplayName("Should delegate markAllAsRead to the repository")
    void markAllAsRead_delegatesToRepository() {

        messageService.markAllAsRead(7L);

        verify(messageRepository).markAllAsRead(7L);
    }

    @Test
    @DisplayName("Should return the repository's unread count for a user")
    void getUnreadCount_delegatesToRepository() {

        when(messageRepository.countUnreadByUserId(7L)).thenReturn(3L);

        long result = messageService.getUnreadCount(7L);

        assertEquals(3L, result);
    }

    // =========================================================================
    // deleteMessage()
    // =========================================================================

    @Test
    @DisplayName("Should soft-delete the message from the sender's view when called by the sender")
    void deleteMessage_bySender_setsDeletedBySenderFlag() {

        Message message = new Message();
        message.setId(10L);
        message.setSenderId(1L);
        message.setRecipientId(2L);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        messageService.deleteMessage(10L, 1L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        assertTrue(captor.getValue().getIsDeletedBySender());
        assertFalse(captor.getValue().getIsDeletedByRecipient());
    }

    @Test
    @DisplayName("Should soft-delete the message from the recipient's view when called by the recipient")
    void deleteMessage_byRecipient_setsDeletedByRecipientFlag() {

        Message message = new Message();
        message.setId(10L);
        message.setSenderId(1L);
        message.setRecipientId(2L);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        messageService.deleteMessage(10L, 2L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());

        assertTrue(captor.getValue().getIsDeletedByRecipient());
    }

    @Test
    @DisplayName("Should refuse to delete a message for a user who is neither sender nor recipient")
    void deleteMessage_byUnrelatedUser_throwsRuntimeException() {

        Message message = new Message();
        message.setId(10L);
        message.setSenderId(1L);
        message.setRecipientId(2L);

        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThrows(RuntimeException.class, () -> messageService.deleteMessage(10L, 999L));

        verify(messageRepository, never()).save(any());
    }

    // =========================================================================
    // replyToMessage()
    // =========================================================================

    @Test
    @DisplayName("Should reply to the original sender with a prefixed subject and inherited reference")
    void replyToMessage_validParent_sendsReplyToOriginalSender() {

        // ---------------------------------------------------------------------
        // Arrange: original message from user 2 (alice) to user 1 (bob);
        // user 1 now replies, so the reply must go back to user 2.
        // ---------------------------------------------------------------------

        Message parent = new Message();
        parent.setId(5L);
        parent.setSenderId(2L);
        parent.setRecipientId(1L);
        parent.setSubject("Original subject");
        parent.setReferenceType("WORKFLOW");
        parent.setReferenceId("WF-001");

        User replier = user(1, "bob");
        User originalSender = user(2, "alice");

        when(messageRepository.findById(5L)).thenReturn(Optional.of(parent));
        when(userRepository.findById(1)).thenReturn(Optional.of(replier));
        when(userRepository.findById(2)).thenReturn(Optional.of(originalSender));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(200L);
            return m;
        });

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Message reply = messageService.replyToMessage(5L, 1L, "Sounds good");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1L, reply.getSenderId());
        assertEquals(2L, reply.getRecipientId());
        assertEquals("Re: Original subject", reply.getSubject());
        assertEquals("Sounds good", reply.getContent());
        assertEquals("WORKFLOW", reply.getReferenceType());
        assertEquals("WF-001", reply.getReferenceId());
    }

    // =========================================================================
    // sendReconciliationMessage() / sendWorkflowMessage()
    // =========================================================================

    @Test
    @DisplayName("Should build a reconciliation-review message with the RECONCILIATION reference type")
    void sendReconciliationMessage_buildsExpectedSubjectAndReference() {

        User sender = user(1, "alice");
        User recipient = user(2, "bob");

        when(userRepository.findById(1)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(300L);
            return m;
        });

        Message result = messageService.sendReconciliationMessage(1L, 2L, 42L, "Please double-check this");

        assertEquals("Review needed for reconciliation record #42", result.getSubject());
        assertEquals("RECONCILIATION", result.getReferenceType());
        assertEquals("42", result.getReferenceId());
        assertTrue(result.getContent().contains("Please double-check this"));
    }

    @Test
    @DisplayName("Should build a workflow-approval message with the WORKFLOW reference type")
    void sendWorkflowMessage_buildsExpectedSubjectAndReference() {

        User sender = user(1, "alice");
        User recipient = user(2, "bob");

        when(userRepository.findById(1)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2)).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setId(301L);
            return m;
        });

        Message result = messageService.sendWorkflowMessage(1L, 2L, 77L, "Needs your sign-off");

        assertEquals("Workflow approval needed #77", result.getSubject());
        assertEquals("WORKFLOW", result.getReferenceType());
        assertEquals("77", result.getReferenceId());
        assertTrue(result.getContent().contains("Needs your sign-off"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User user(int id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }
}