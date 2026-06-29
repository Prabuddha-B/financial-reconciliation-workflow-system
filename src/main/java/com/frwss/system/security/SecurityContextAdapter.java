package com.frwss.system.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * This adapter class provides current user information
 * It supports both development mode (hardcoded values) and production mode (real security).
 * The mode is controlled via application.properties
 */

@Component
public class SecurityContextAdapter {

    // Configurable via application.properties: security.enabled=true/false
    @Value("${security.enabled:false}")
    private boolean useRealSecurity;

    // Hardcoded fallback values for development
    @Value("${security.dev.username:admin}")
    private String devUsername;

    @Value("${security.dev.role:REVIEWER}")
    private String devRole;

    // Returns the currently logged-in username
    public String getCurrentUsername(){
        if(useRealSecurity){
            // when security is enabled, get user from Spring Security context
            return getRealUsername();
        }
        // Development mode - return hardcoded user
        return devUsername;
    }

    // Returns the current user's role (without the ROLE_ prefix)
    public String getCurrentUserRole(){
        if(useRealSecurity){
            // when security is enabled, get role from Spring Security context
            return getRealUserRole();
        }
        // Development mode - return hardcoded role
        return devRole;
    }

    // Checks whether the current user has a specific role
    public boolean hasRole(String role){

        // If security is not enabled, always return true for development
        if (!useRealSecurity)  return true;

        // Real security logic
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) return false;
            return authentication.getAuthorities()
                    .stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
        } catch (Exception e){
            // if something goes wrong, default to false for security
            return false;
        }
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

    // these methods will be replaced when the security module is integrated

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