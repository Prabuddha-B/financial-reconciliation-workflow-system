package com.frwss.system.messaging.model;

/**
 * Types of notifications that can be generated
 */

public enum NotificationType {
    MESSAGE,  // new message received
    APPROVAL, // Approval needed for workflow
    REMINDER, // system reminder
    SYSTEM, // system notification
    TASK, // task assigned
    RECONCILIATION, // reconciliation complete
    WORKFLOW, // workflow state change
    APPROVED, // record approval
    REJECTED, // record rejected
    UPLOAD // file upload
}
