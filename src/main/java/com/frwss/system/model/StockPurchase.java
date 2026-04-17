package com.frwss.system.model;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_purchases")
public class StockPurchase {

    @Id
    @Column(name = "purchase_id")
    private String purchaseId;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "invoice_no")
    private String invoiceNo;

    private BigDecimal amount;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "entered_by")
    private String enteredBy; // Now a String/VARCHAR(50)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and Setters
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public void setEnteredBy(String enteredBy) { this.enteredBy = enteredBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}