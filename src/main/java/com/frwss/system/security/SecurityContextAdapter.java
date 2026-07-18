package com.frwss.system.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextAdapter {

    @Value("${security.enabled:true}")
    private boolean useRealSecurity;

    @Value("${security.dev.username:admin}")
    private String devUsername;

    @Value("${security.dev.role:REVIEWER}")
    private String devRole;

    public String getCurrentUsername() {
        if (useRealSecurity) {
            return getRealUsername();
        }
        return devUsername;
    }

    public String getCurrentUserRole() {
        if (useRealSecurity) {
            return getRealUserRole();
        }
        return devRole;
    }

    public boolean hasRole(String role) {
        if (!useRealSecurity) {
            return true;
        }
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) return false;
            return authentication.getAuthorities()
                    .stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
        } catch (Exception e) {
            return false;
        }
    }

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

    private String getRealUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return devUsername;
        }
        return authentication.getName();
    }

    private String getRealUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().isEmpty()) {
            return devRole;
        }
        GrantedAuthority authority = authentication.getAuthorities().iterator().next();
        return authority.getAuthority().replace("ROLE_", "");
    }
}