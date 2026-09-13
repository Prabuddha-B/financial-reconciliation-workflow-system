package com.frwss.system.model;

import com.frwss.system.security.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Unit Tests")
class UserTest {
    @Test
    @DisplayName("Should use enabled and empty roles as defaults")
    void user_defaults_enabledAndEmptyRoles() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        User user = new User();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        boolean enabled = user.isEnabled();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertTrue(enabled); assertNotNull(user.getRoles()); assertTrue(user.getRoles().isEmpty());
    }

    @Test
    @DisplayName("Should store and return all user fields")
    void user_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        User user = new User(); LocalDateTime created = LocalDateTime.now(); HashSet<Role> roles = new HashSet<>();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        user.setUserId(1); user.setUsername("admin"); user.setRole("ADMIN"); user.setPassword("secret");
        user.setEnabled(false); user.setCreatedAt(created); user.setRoles(roles);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(1, user.getUserId()); assertEquals("admin", user.getUsername()); assertEquals("ADMIN", user.getRole());
        assertEquals("secret", user.getPassword()); assertFalse(user.isEnabled()); assertEquals(created, user.getCreatedAt());
        assertSame(roles, user.getRoles());
    }
}
