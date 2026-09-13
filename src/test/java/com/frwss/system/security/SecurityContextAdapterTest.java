package com.frwss.system.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Context Adapter Unit Tests")
class SecurityContextAdapterTest {

    @Mock
    private Authentication authentication;

    private SecurityContextAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SecurityContextAdapter();
        ReflectionTestUtils.setField(adapter, "useRealSecurity", true);
        ReflectionTestUtils.setField(adapter, "devUsername", "admin");
        ReflectionTestUtils.setField(adapter, "devRole", "REVIEWER");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // getCurrentUsername()
    // =========================================================================

    @Test
    @DisplayName("Should return the authenticated username")
    void getCurrentUsername_authenticatedUser_returnsUsername() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("prabuddha");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = adapter.getCurrentUsername();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "prabuddha",
                result
        );
    }

    @Test
    @DisplayName("Should return the development username when authentication is null")
    void getCurrentUsername_nullAuthentication_returnsDevelopmentUsername() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = adapter.getCurrentUsername();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "admin",
                result
        );
    }

    @Test
    @DisplayName("Should return the development username for an anonymous user")
    void getCurrentUsername_anonymousUser_returnsDevelopmentUsername() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(authentication.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = adapter.getCurrentUsername();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "admin",
                result
        );
    }

    @Test
    @DisplayName("Should preserve a null authenticated principal name")
    void getCurrentUsername_nullPrincipal_returnsNull() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = adapter.getCurrentUsername();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                null,
                result
        );
    }

    // =========================================================================
    // getCurrentUserRole()
    // =========================================================================


    @Test
    @DisplayName("Should return the development role when there are no authorities")
    void getCurrentUserRole_noRoles_returnsDevelopmentRole() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(authentication.getAuthorities()).thenReturn(Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = adapter.getCurrentUserRole();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "REVIEWER",
                result
        );
    }

    @Test
    @DisplayName("Should return the development role when authentication is null")
    void getCurrentUserRole_nullAuthentication_returnsDevelopmentRole() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = adapter.getCurrentUserRole();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "REVIEWER",
                result
        );
    }

    // =========================================================================
    // hasRole()
    // =========================================================================

    @Test
    @DisplayName("Should return true for a matching role")
    void hasRole_matchingRole_returnsTrue() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        doReturn(
                List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ).when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasRole("ADMIN");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("Should return false for an invalid role")
    void hasRole_invalidRole_returnsFalse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        doReturn(
                List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ).when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasRole("AUDITOR");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Should return false when authentication is null")
    void hasRole_nullAuthentication_returnsFalse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasRole("ADMIN");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Should return false when an invalid authority causes a security error")
    void hasRole_invalidAuthority_returnsFalse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        doReturn(
                Collections.<GrantedAuthority>singletonList(null)
        ).when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasRole("ADMIN");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Should return true for every role in development mode")
    void hasRole_developmentMode_returnsTrue() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReflectionTestUtils.setField(adapter, "useRealSecurity", false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasRole("INVALID");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                result
        );
    }

    // =========================================================================
    // hasAnyRole()
    // =========================================================================

    @Test
    @DisplayName("Should return true when any supplied role matches")
    void hasAnyRole_multipleRoles_oneRoleMatches_returnsTrue() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        doReturn(
                List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_AUDITOR"))
        ).when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasAnyRole("ADMIN", "AUDITOR");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                result
        );
    }

    @Test
    @DisplayName("Should return false when no supplied role matches")
    void hasAnyRole_invalidRoles_returnsFalse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        doReturn(
                List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_USER"))
        ).when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasAnyRole("ADMIN", "AUDITOR");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(
                result
        );
    }

    @Test
    @DisplayName("Should return false for no supplied roles")
    void hasAnyRole_noRoles_returnsFalse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.hasAnyRole();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(
                result
        );
    }

    // =========================================================================
    // getRealUsername()
    // =========================================================================

    @Test
    @DisplayName("Should return the real authenticated username")
    void getRealUsername_authenticatedUser_returnsUsername() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("auditor");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = ReflectionTestUtils.invokeMethod(adapter, "getRealUsername");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "auditor",
                result
        );
    }

    // =========================================================================
    // getRealUserRole()
    // =========================================================================

    @Test
    @DisplayName("Should return the real role without the ROLE prefix")
    void getRealUserRole_prefixedRole_returnsRoleWithoutPrefix() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        doReturn(
                List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_REVIEWER"))
        ).when(authentication).getAuthorities();
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = ReflectionTestUtils.invokeMethod(adapter, "getRealUserRole");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "REVIEWER",
                result
        );
    }

    // =========================================================================
    // isDevelopmentMode() and isSecurityEnabled()
    // =========================================================================

    @Test
    @DisplayName("Should report development mode when security is disabled")
    void isDevelopmentMode_securityDisabled_returnsTrue() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReflectionTestUtils.setField(adapter, "useRealSecurity", false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.isDevelopmentMode();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                result
        );
        assertFalse(
                adapter.isSecurityEnabled()
        );
    }

    @Test
    @DisplayName("Should report enabled security when security is enabled")
    void isSecurityEnabled_securityEnabled_returnsTrue() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReflectionTestUtils.setField(adapter, "useRealSecurity", true);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = adapter.isSecurityEnabled();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                result
        );
        assertFalse(
                adapter.isDevelopmentMode()
        );
    }
}
