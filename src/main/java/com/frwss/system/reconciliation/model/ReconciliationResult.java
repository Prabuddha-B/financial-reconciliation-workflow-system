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
    @Column(name = "match_record_id")
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
}
