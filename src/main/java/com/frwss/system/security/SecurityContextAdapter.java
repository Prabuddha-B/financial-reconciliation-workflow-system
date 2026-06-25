package com.frwss.system.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextAdapter {

    @Value("${security.enabled:false}")
    private boolean useRealSecurity;

    @Value("${security.dev.username:admin}")
    private String devUsername;

    @Value("${security.dev.role:REVIEWER}")
    private String devRole;


//    Returns the currently logged-in username
    public String getCurrentUsername() {
        if (useRealSecurity) {
            return getRealUsername();
        }
        return devUsername;
    }


//     Returns the current user's role (without the ROLE_ prefix)
    public String getCurrentUserRole() {
        if (useRealSecurity) {
            return getRealUserRole();
        }
        return devRole;
    }

//    Checks whether the current user has a specific role
    public boolean hasRole(String role) {

        if (!useRealSecurity)  return true;

        Authentication authentication =  SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) return false;

        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }


//     Checks multiple roles.
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDevelopmentMode() {
        return !useRealSecurity;
    }

    public boolean isSecurityEnabled() {
        return useRealSecurity;
    }


//     Gets username from Spring Security.
    private String getRealUsername() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return devUsername;
        }

        return authentication.getName();
    }

//    Gets the first role from Spring Security
    private String getRealUserRole() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getAuthorities().isEmpty()) {
            return devRole;
        }

        GrantedAuthority authority =
                authentication.getAuthorities().iterator().next();

        return authority.getAuthority().replace("ROLE_", "");
    }
}