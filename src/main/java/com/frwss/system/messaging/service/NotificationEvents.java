package com.frwss.system.messaging.service;

import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Sends the business notifications (reconciliation finished, workflow needs approval, record
 * approved or rejected, upload finished) when those things happen in the other modules.
 *
 * Other modules call this class with a username, because that is what they know; it looks up the
 * user id and calls the matching helper in {@link NotificationService}.
 *
 * <b>A notification must never break the real work.</b> Every method catches its own errors and only
 * logs them. The methods run with {@code NOT_SUPPORTED}: they pause the caller's database
 * transaction, so a failed notification insert cannot mark the caller's transaction as
 * rollback-only. The trade-off is that a notification is saved even if the caller's transaction
 * rolls back later, which is acceptable for an alert.
 */
@Component
public class NotificationEvents {

    private static final Logger log = LoggerFactory.getLogger(NotificationEvents.class);

    /** Role name as stored by the security seeder. */
    private static final String APPROVER_ROLE = "ROLE_APPROVER";

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationEvents(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /** Tells the user who ran a reconciliation how things stand afterwards. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void reconciliationFinished(String username, int exactCount, int partialCount, int orphanCount) {
        try {
            findUserId(username).ifPresent(userId ->
                    notificationService.notifyReconciliationComplete(userId, exactCount, partialCount, orphanCount));
        } catch (Exception e) {
            log.warn("Could not send the reconciliation notification: {}", e.getMessage());
        }
    }

    /** Tells every enabled approver (except the person who submitted it) that a workflow needs them. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void workflowSubmitted(Long workflowId, String submittedBy, String details) {
        try {
            if (workflowId == null) {
                return;
            }
            for (User user : userRepository.findAll()) {
                boolean isApprover = user.isEnabled() && user.getRoles() != null
                        && user.getRoles().stream().anyMatch(role -> APPROVER_ROLE.equals(role.getName()));
                if (isApprover && !user.getUsername().equals(submittedBy)) {
                    notificationService.notifyWorkflowApproval(
                            workflowId, user.getUserId().longValue(),
                            "Submitted by " + submittedBy + ". " + (details == null ? "" : details));
                }
            }
        } catch (Exception e) {
            log.warn("Could not send the workflow approval notification: {}", e.getMessage());
        }
    }

    /** Tells the person who submitted a workflow that it was approved or rejected. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void workflowDecided(String submittedBy, String recordId, String recordType, boolean approved) {
        try {
            findUserId(submittedBy).ifPresent(userId -> {
                if (approved) {
                    notificationService.notifyRecordApproved(userId, recordId, recordType);
                } else {
                    notificationService.notifyRecordRejected(userId, recordId, recordType);
                }
            });
        } catch (Exception e) {
            log.warn("Could not send the workflow decision notification: {}", e.getMessage());
        }
    }

    /** Tells the uploader that their file was saved. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void uploadFinished(String username, String description) {
        try {
            findUserId(username).ifPresent(userId ->
                    notificationService.notifyUploadComplete(userId, description, username));
        } catch (Exception e) {
            log.warn("Could not send the upload notification: {}", e.getMessage());
        }
    }

    private Optional<Long> findUserId(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username)
                .map(user -> user.getUserId().longValue());
    }
}
