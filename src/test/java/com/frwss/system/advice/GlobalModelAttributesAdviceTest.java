package com.frwss.system.advice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Global Model Attributes Advice Unit Tests")
class GlobalModelAttributesAdviceTest {

    private final GlobalModelAttributesAdvice advice =
            new GlobalModelAttributesAdvice();

    // =========================================================================
    // addUserAttributes()
    // =========================================================================

    @Test
    @DisplayName("Should add current user and role for authenticated user")
    void addUserAttributes_authenticatedUser_addsAttributes() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(
                        "admin",
                        "password",
                        "ROLE_ADMIN"
                );

        authentication.setAuthenticated(true);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        advice.addUserAttributes(model, authentication);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "admin",
                model.getAttribute("currentUser")
        );

        assertEquals(
                "ADMIN",
                model.getAttribute("userRole")
        );
    }

    @Test
    @DisplayName("Should not add attributes when authentication is null")
    void addUserAttributes_nullAuthentication_doesNothing() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        advice.addUserAttributes(model, null);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNull(model.getAttribute("currentUser"));
        assertNull(model.getAttribute("userRole"));
    }

    @Test
    @DisplayName("Should remove ROLE prefix before adding user role")
    void addUserAttributes_removesRolePrefix() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        TestingAuthenticationToken authentication =
                new TestingAuthenticationToken(
                        "auditor",
                        "password",
                        "ROLE_AUDITOR"
                );

        authentication.setAuthenticated(true);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        advice.addUserAttributes(model, authentication);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "AUDITOR",
                model.getAttribute("userRole")
        );
    }
}