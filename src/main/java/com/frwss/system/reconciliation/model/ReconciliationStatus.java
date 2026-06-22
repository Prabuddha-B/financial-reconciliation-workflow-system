package com.frwss.system.reconciliation.model;

public enum ReconciliationStatus {
    EXACT,
    PARTIAL,
    DUPLICATE,
    ORPHAN,
    MANUALLY_RESOLVED,
//    SUBMITTED_FOR_REVIEW,             // For Workflow Integration
    PENDING_APPROVAL,                 // Waiting for Approval
    APPROVED,
    REJECTED
}
