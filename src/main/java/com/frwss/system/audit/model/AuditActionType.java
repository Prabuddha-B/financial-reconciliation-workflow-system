package com.frwss.system.audit.model;

public enum AuditActionType {
    DATA_INGESTION,
    DUPLICATE_DETECTED,

    RECONCILIATION_STARTED,
    EXACT_MATCH_FOUND,
    PARTIAL_MATCH_FOUND,
    ORPHAN_RECORD_FOUND,
    RECONCILIATION_COMPLETED,

    WORKFLOW_STATE_CHANGED,
    MANUAL_MODIFICATION,
    APPROVAL_GRANTED,
    APPROVAL_DENIED,

    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    ACCESS_DENIED,

    REPORT_EXPORTED,
    AUDIT_CHAIN_VERIFIED,
    AUDIT_CHAIN_CORRECTED,

    TAX_ESTIMATION,
    TAX_ESTIMATION_ERROR,

    USER_CREATED,
    USER_UPDATED,

    USER_ENABLED,
    USER_DISABLED,

    PIN_RESET,
    ACCOUNT_SWITCHED,

    PASSWORD_RESET,

    // Payment Module Actions
    PAYMENT_PROCESSED,          // Payment successfully processed
    PAYMENT_FAILED,             // Payment processing failed
    PAYMENT_REFUNDED,           // Payment refunded
    PAYMENT_WEBHOOK_RECEIVED,   // Webhook received from payment gateway
    PAYMENT_RECONCILIATION,     // Payment reconciliation run


}
