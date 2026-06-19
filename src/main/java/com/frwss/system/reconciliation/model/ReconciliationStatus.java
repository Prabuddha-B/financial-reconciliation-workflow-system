package com.frwss.system.reconciliation.model;

public enum ReconciliationStatus {
    EXACT,
    PARTIAL,
    DUPLICATE,
    ORPHAN,
    MANUALLY_RESOLVED,
    APPROVED,
    REJECTED
}
