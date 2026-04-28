package com.frwss.system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="stock_purchases")
public class StockPurchase {

    @Id
    @Column(name="purchase_id")
    private String purchaseId;

    @Column(name="vendor_name")
    private String vendorName;

    @Column(name="invoice_no")
    private String invoiceNo;

    private BigDecimal amount;

    @Column(name="purchase_date")
    private LocalDate purchaseDate;

    public String getPurchaseId(){return purchaseId;}
    public void setPurchaseId(String x){purchaseId=x;}

    public String getVendorName(){return vendorName;}
    public void setVendorName(String x){vendorName=x;}

    public String getInvoiceNo(){return invoiceNo;}
    public void setInvoiceNo(String x){invoiceNo=x;}

    public BigDecimal getAmount(){return amount;}
    public void setAmount(BigDecimal x){amount=x;}

    public LocalDate getPurchaseDate(){return purchaseDate;}
    public void setPurchaseDate(LocalDate x){purchaseDate=x;}

}