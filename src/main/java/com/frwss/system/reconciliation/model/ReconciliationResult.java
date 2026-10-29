package com.frwss.system.reconciliation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_results")
public class ReconciliationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // main ledger record from accounting_records table
    @Column(name = "accounting_record_id", nullable = false)
    private String accountingRecordId;

    // Matched record from payroll / receipts / stock_purchases
    @Column(name = "matched_record_id")
    private String matchedRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "matched_source_type", nullable = false)
    private MatchedSourceType matchedSourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconciliationStatus status;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "reconciled_at", nullable = false)
    private LocalDateTime reconciledAt;

    @Column(name = "reconciled_by")
    private String reconciledBy;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "submitted_to_workflow_at")
    private LocalDateTime submittedToWorkflowAt;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "reference_match_status")
    private String referenceMatchStatus; // "MATCH", "PARTIAL", "MISMATCH"

    @Column(name = "amount_match_status")
    private String amountMatchStatus; // "MATCH", "PARTIAL", "MISMATCH"

    public String getReferenceMatchStatus() {
        return referenceMatchStatus;
    }

    public void setReferenceMatchStatus(String referenceMatchStatus) {
        this.referenceMatchStatus = referenceMatchStatus;
    }

    public String getAmountMatchStatus() {
        return amountMatchStatus;
    }

    public void setAmountMatchStatus(String amountMatchStatus) {
        this.amountMatchStatus = amountMatchStatus;
    }

    public String getDateMatchStatus() {
        return dateMatchStatus;
    }

    public void setDateMatchStatus(String dateMatchStatus) {
        this.dateMatchStatus = dateMatchStatus;
    }

    @Column(name = "date_match_status")
    private String dateMatchStatus; // "MATCH", "PARTIAL", "MISMATCH"

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountingRecordId() {
        return accountingRecordId;
    }

    public void setAccountingRecordId(String accountingRecordId) {
        this.accountingRecordId = accountingRecordId;
    }

    public String getMatchedRecordId() {
        return matchedRecordId;
    }

    public void setMatchedRecordId(String matchedRecordId) {
        this.matchedRecordId = matchedRecordId;
    }

    public MatchedSourceType getMatchedSourceType() {
        return matchedSourceType;
    }

    public void setMatchedSourceType(MatchedSourceType matchedSourceType) {
        this.matchedSourceType = matchedSourceType;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public void setStatus(ReconciliationStatus status) {
        this.status = status;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
    }

    public String getReconciledBy() {
        return reconciledBy;
    }

    public void setReconciledBy(String reconciledBy) {
        this.reconciledBy = reconciledBy;
    }

    public Long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(Long workflowId) {
        this.workflowId = workflowId;
    }

    public LocalDateTime getSubmittedToWorkflowAt() {
        return submittedToWorkflowAt;
    }

    public void setSubmittedToWorkflowAt(LocalDateTime submittedToWorkflowAt) {
        this.submittedToWorkflowAt = submittedToWorkflowAt;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }
}
