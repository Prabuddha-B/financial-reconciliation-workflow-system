package com.frwss.system.messaging.controller;

import com.frwss.system.messaging.model.Message;
import com.frwss.system.messaging.service.MessageService;
import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Message Controller View Unit Tests")
class MessageControllerViewTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @InjectMocks
    private MessageController messageController;

    @Test
    @DisplayName("Should not show a message to someone who is neither its sender nor its recipient")
    void viewMessage_byOutsider_redirectsToInboxAndDoesNotMarkRead() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("carol");
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user(3, "carol")));
        when(messageService.getMessage(10L)).thenReturn(message(10L, 1L, 2L));

        Model model = new ConcurrentModel();
        String view = messageController.viewMessage(10L, model);

        assertEquals("redirect:/messages/inbox", view);
        verify(messageService, never()).markAsRead(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Should show the message to its recipient and mark it as read")
    void viewMessage_byRecipient_showsMessageAndMarksRead() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user(2, "bob")));
        when(messageService.getMessage(10L)).thenReturn(message(10L, 1L, 2L));
        when(userRepository.findById(1)).thenReturn(Optional.of(user(1, "alice")));
        when(userRepository.findById(2)).thenReturn(Optional.of(user(2, "bob")));

        Model model = new ConcurrentModel();
        String view = messageController.viewMessage(10L, model);

        assertEquals("messages/view", view);
        verify(messageService).markAsRead(10L, 2L);
    }

    @Test
    @DisplayName("Should show the message to its sender without marking it as read")
    void viewMessage_bySender_showsMessageWithoutMarkingRead() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user(1, "alice")));
        when(messageService.getMessage(10L)).thenReturn(message(10L, 1L, 2L));
        when(userRepository.findById(1)).thenReturn(Optional.of(user(1, "alice")));
        when(userRepository.findById(2)).thenReturn(Optional.of(user(2, "bob")));

        Model model = new ConcurrentModel();
        String view = messageController.viewMessage(10L, model);

        assertEquals("messages/view", view);
        verify(messageService, never()).markAsRead(anyLong(), anyLong());
    }

    private Message message(long id, long senderId, long recipientId) {
        Message message = new Message();
        message.setId(id);
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setSubject("Hello");
        message.setContent("Body");
        message.setIsRead(false);
        return message;
    }

    private User user(int id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }
}
