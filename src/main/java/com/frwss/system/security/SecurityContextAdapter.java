package com.frwss.system.security;

import org.springframework.beans.factory.annotation.Value;
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

    public String getCurrentUsername(){
        if(useRealSecurity){
            // when security is enabled, get user from Spring Security context
            return getRealUsername();
        }
        // Development mode - return hardcoded user
        return devUsername;
    }

    public String getCurrentUserRole(){
        if(useRealSecurity){
            // when security is enabled, get role from Spring Security context
            return getRealUserRole();
        }
        // Development mode - return hardcoded role
        return devRole;
    }

    public boolean hasRole(String role){
        // If security is not enabled, always return true for development
        if(!useRealSecurity){
            return true;
        }

        // Real security logic
        try{
            // this will be replaced with actual Spring Security role check
            // return SecurityContextHolder.getContext().getAuthentication()
            //      .getAuthorities().stream()
            //      .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));

            // Placeholder - will be implemented when security module is integrated

            return getCurrentUserRole().equals(role) || getCurrentUserRole().equals("ADMIN");
        } catch(Exception e){
            // if something goes wrong, default to false for security
            return false;
        }
    }

    public boolean hasAnyRole(String... roles){
        for (String role: roles){
            if(hasRole(role)){
                return true;
            }
        }
        return false;
    }

    public boolean isDevelopmentMode(){
        return !useRealSecurity;
    }

    public boolean isSecurityEnabled(){
        return useRealSecurity;
    }

    // these methods will be replaced when the security module is integrated

    private String getRealUsername(){
        // TODO: Replace with actual Spring Security implementation
        // return SecurityContextHolder.getContext().getAuthentication().getName();

        // For now, fall back to dev username when security is enabled but not integrated
        // this prevents NPE during gradual integration
        return devUsername;
    }

    private String getRealUserRole(){
        // TODO: Replace with actual Spring Security implementation
        // return SecurityContextHolder.getContext().getAuthentication()
        //         .getAuthorities().iterator().next().getAuthority();

        // For now, fall back to dev role when security is enabled but not integrated

        return devRole;
    }
}
