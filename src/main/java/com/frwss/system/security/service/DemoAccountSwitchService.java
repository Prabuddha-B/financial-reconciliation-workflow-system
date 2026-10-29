package com.frwss.system.security.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.User;
import com.frwss.system.security.SecurityContextAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class DemoAccountSwitchService {

    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    public DemoAccountSwitchService(
            CustomUserDetailsService userDetailsService,
            AuditService auditService,
            SecurityContextAdapter securityContextAdapter) {

        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

//    Switches the authenticated demo account
    public void switchAccount(User targetUser) {

        String previousUser =
                securityContextAdapter.getCurrentUsername();

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        targetUser.getUsername()
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        auditService.log(
                AuditActionType.ACCOUNT_SWITCHED,
                "User",
                targetUser.getUserId().toString(),
                previousUser,
                "Switched account to '"
                        + targetUser.getUsername()
                        + "'"
        );
    }
}