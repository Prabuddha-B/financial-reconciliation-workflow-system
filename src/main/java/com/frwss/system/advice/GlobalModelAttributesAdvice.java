// ------ Provides global user information to all Thymeleaf views for shared UI components -----

package com.frwss.system.advice;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAttributesAdvice {

    @ModelAttribute
    public void addUserAttributes(Model model, Authentication authentication) {

        if (authentication != null && authentication.isAuthenticated()) {

            model.addAttribute("currentUser", authentication.getName());

            String role = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("");

            role = role.replace("ROLE_", "");

            model.addAttribute("userRole", role);
        }
    }
}