package com.frwss.system.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @Column(name = "receipt_id")
    private String receiptId;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "payer_name")
    private String payerName;

    private BigDecimal amount;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "entered_by")
    private String enteredBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and Setters for all fields...


    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(String enteredBy) {
        this.enteredBy = enteredBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}