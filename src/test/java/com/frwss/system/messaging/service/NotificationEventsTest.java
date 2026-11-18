package com.frwss.system.messaging.service;

import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.security.model.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Events Unit Tests")
class NotificationEventsTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationEvents notificationEvents;

    // =========================================================================
    // reconciliationFinished()
    // =========================================================================

    @Test
    @DisplayName("Should notify the user who ran the reconciliation, using their user id")
    void reconciliationFinished_knownUser_notifiesWithUserId() {

        when(userRepository.findByUsername("entry")).thenReturn(Optional.of(user(2, "entry", true)));

        notificationEvents.reconciliationFinished("entry", 49, 8, 7);

        verify(notificationService).notifyReconciliationComplete(2L, 49, 8, 7);
    }

    @Test
    @DisplayName("Should do nothing when the user cannot be found")
    void reconciliationFinished_unknownUser_doesNothing() {

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        notificationEvents.reconciliationFinished("ghost", 1, 1, 1);

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Should never let a failed notification break the caller")
    void reconciliationFinished_notificationFails_doesNotThrow() {

        when(userRepository.findByUsername("entry")).thenReturn(Optional.of(user(2, "entry", true)));
        doThrow(new RuntimeException("database down"))
                .when(notificationService).notifyReconciliationComplete(2L, 1, 2, 3);

        assertDoesNotThrow(() -> notificationEvents.reconciliationFinished("entry", 1, 2, 3));
    }

    // =========================================================================
    // workflowSubmitted()
    // =========================================================================

    @Test
    @DisplayName("Should notify enabled approvers only, never the person who submitted it")
    void workflowSubmitted_notifiesEnabledApproversExceptSubmitter() {

        User entry = user(2, "entry", true, "ROLE_ENTRY_STAFF");
        User approver = user(4, "approver", true, "ROLE_APPROVER");
        User sleepyApprover = user(7, "sleepy", false, "ROLE_APPROVER");      // disabled
        User submitterWhoIsAlsoApprover = user(6, "bob", true, "ROLE_APPROVER");

        when(userRepository.findAll()).thenReturn(List.of(entry, approver, sleepyApprover, submitterWhoIsAlsoApprover));

        notificationEvents.workflowSubmitted(50L, "bob", "Reconciliation record ACC-0051 needs approval.");

        verify(notificationService, times(1)).notifyWorkflowApproval(anyLong(), anyLong(), anyString());
        verify(notificationService).notifyWorkflowApproval(eq(50L), eq(4L), contains("Submitted by bob"));
    }

    @Test
    @DisplayName("Should do nothing when there is no workflow id")
    void workflowSubmitted_nullWorkflowId_doesNothing() {

        notificationEvents.workflowSubmitted(null, "entry", "details");

        verifyNoInteractions(userRepository, notificationService);
    }

    // =========================================================================
    // workflowDecided()
    // =========================================================================

    @Test
    @DisplayName("Should tell the submitter their record was approved")
    void workflowDecided_approved_notifiesRecordApproved() {

        when(userRepository.findByUsername("entry")).thenReturn(Optional.of(user(2, "entry", true)));

        notificationEvents.workflowDecided("entry", "1001", "RECONCILIATION", true);

        verify(notificationService).notifyRecordApproved(2L, "1001", "RECONCILIATION");
    }

    @Test
    @DisplayName("Should tell the submitter their record was rejected")
    void workflowDecided_rejected_notifiesRecordRejected() {

        when(userRepository.findByUsername("entry")).thenReturn(Optional.of(user(2, "entry", true)));

        notificationEvents.workflowDecided("entry", "1001", "RECONCILIATION", false);

        verify(notificationService).notifyRecordRejected(2L, "1001", "RECONCILIATION");
    }

    // =========================================================================
    // uploadFinished()
    // =========================================================================

    @Test
    @DisplayName("Should tell the uploader their file was saved")
    void uploadFinished_notifiesUploader() {

        when(userRepository.findByUsername("entry")).thenReturn(Optional.of(user(2, "entry", true)));

        notificationEvents.uploadFinished("entry", "PAYROLL data (20 records saved)");

        verify(notificationService).notifyUploadComplete(2L, "PAYROLL data (20 records saved)", "entry");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User user(int id, String username, boolean enabled, String... roleNames) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEnabled(enabled);
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setName(roleName);
            roles.add(role);
        }
        user.setRoles(roles);
        return user;
    }
}
