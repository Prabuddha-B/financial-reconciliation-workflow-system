package com.frwss.system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="receipts")
public class Receipt {

    @Id
    @Column(name="receipt_id")
    private String receiptId;

    @Column(name="reference_no")
    private String referenceNo;

    @Column(name="payer_name")
    private String payerName;

    private BigDecimal amount;

    @Column(name="date")
    private LocalDate receiptDate;

    public String getReceiptId(){ return receiptId; }
    public void setReceiptId(String x){receiptId=x;}

    public String getReferenceNo(){ return referenceNo; }
    public void setReferenceNo(String x){referenceNo=x;}

    public String getPayerName(){ return payerName; }
    public void setPayerName(String x){payerName=x;}

    public BigDecimal getAmount(){ return amount; }
    public void setAmount(BigDecimal x){amount=x;}

    public LocalDate getReceiptDate(){ return receiptDate; }
    public void setReceiptDate(LocalDate x){receiptDate=x;}

}