package com.frwss.system.messaging.controller;

import com.frwss.system.messaging.dto.MessageDto;
import com.frwss.system.messaging.service.MessageService;
import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Message Controller Compose Unit Tests")
class MessageControllerComposeTest {

    @Mock
    private MessageService messageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @InjectMocks
    private MessageController messageController;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    @Test
    @DisplayName("Should list every user except the current user as possible recipients")
    void compose_excludesCurrentUserFromRecipients() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        User alice = user(1, "alice");
        User bob = user(2, "bob");
        User carol = user(3, "carol");

        when(securityContextAdapter.getCurrentUsername()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        // must be a mutable list: the controller removes the current user from it
        when(userRepository.findAll()).thenReturn(new ArrayList<>(List.of(alice, bob, carol)));

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view = messageController.compose(model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("messages/compose", view);

        @SuppressWarnings("unchecked")
        List<User> recipients = (List<User>) model.getAttribute("users");

        assertEquals(2, recipients.size());
        assertFalse(recipients.contains(alice), "The current user must not be a recipient");
        assertTrue(recipients.contains(bob));
        assertTrue(recipients.contains(carol));
        assertInstanceOf(MessageDto.class, model.getAttribute("message"));
    }

    private User user(int id, String username) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        return user;
    }
}