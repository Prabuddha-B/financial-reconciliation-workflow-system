package com.frwss.system.reconciliation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CandidateMatchView {

    private String sourceType;
    private String recordId;
    private String referenceNo;
    private BigDecimal amount;
    private LocalDate recordDate;
    private String description;
    private Double suggestedConfidence;
    private String reason;

    public CandidateMatchView(
            String sourceType,
            String recordId,
            String referenceNo,
            BigDecimal amount,
            LocalDate recordDate,
            String description,
            Double suggestedConfidence,
            String reason
    ){
        this.sourceType = sourceType;
        this.recordId = recordId;
        this.referenceNo = referenceNo;
        this.amount = amount;
        this.recordDate = recordDate;
        this.description = description;
        this.suggestedConfidence = suggestedConfidence;
        this.reason = reason;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public String getDescription() {
        return description;
    }

    public Double getSuggestedConfidence() {
        return suggestedConfidence;
    }

    public String getReason() {
        return reason;
    }
}
