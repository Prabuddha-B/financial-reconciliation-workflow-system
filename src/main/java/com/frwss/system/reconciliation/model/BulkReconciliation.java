package com.frwss.system.reconciliation.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_reconciliations")
public class BulkReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bulk_record_id", nullable = false)
    private String bulkRecordId;

    @Column(name = "bulk_reference_no")
    private String bulkReferenceNo;

    @Column(name = "total_expected_amount", nullable = false)
    private BigDecimal totalExpectedAmount;

    @Column(name = "total_matched_amount")
    private BigDecimal totalMatchedAmount;

    @Column(name = "difference_amount")
    private BigDecimal differenceAmount;

    @Column(name = "child_count")
    private Integer childCount;

    @Column(name = "matched_chile_count")
    private Integer matchedChildCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReconciliationStatus status;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "reconciled_by")
    private String reconciledBy;

    @Column(name = "child_record_ids", columnDefinition = "TEXT")
    private String childRecordIds; // comma separated list of child record IDs

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBulkRecordId() {
        return bulkRecordId;
    }

    public void setBulkRecordId(String bulkRecordId) {
        this.bulkRecordId = bulkRecordId;
    }

    public String getBulkReferenceNo() {
        return bulkReferenceNo;
    }

    public void setBulkReferenceNo(String bulkReferenceNo) {
        this.bulkReferenceNo = bulkReferenceNo;
    }

    public BigDecimal getTotalExpectedAmount() {
        return totalExpectedAmount;
    }

    public void setTotalExpectedAmount(BigDecimal totalExpectedAmount) {
        this.totalExpectedAmount = totalExpectedAmount;
    }

    public BigDecimal getTotalMatchedAmount() {
        return totalMatchedAmount;
    }

    public void setTotalMatchedAmount(BigDecimal totalMatchedAmount) {
        this.totalMatchedAmount = totalMatchedAmount;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(BigDecimal differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public Integer getChildCount() {
        return childCount;
    }

    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }

    public Integer getMatchedChildCount() {
        return matchedChildCount;
    }

    public void setMatchedChildCount(Integer matchedChildCount) {
        this.matchedChildCount = matchedChildCount;
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

    public String getChildRecordIds() {
        return childRecordIds;
    }

    public void setChildRecordIds(String childRecordIds) {
        this.childRecordIds = childRecordIds;
    }
}
