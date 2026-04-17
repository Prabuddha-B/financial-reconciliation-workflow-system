package com.frwss.system.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_records")
public class AccountingRecord{

    @Id
    @Column(name = "record_id")
    private String recordId;

    @Column(name = "reference_no")
    private String referenceNo;

    private String category; // e.g., PAYROLL, PURCHASE, RECEIPT

    private BigDecimal amount;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "entered_by")
    private String enteredBy;

    public String getRecordId() {
        return recordId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Standard Setters
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public void setCategory(String category) { this.category = category; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public void setEnteredBy(String enteredBy) { this.enteredBy = enteredBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setModule(String module) {
    }
}