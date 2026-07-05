package com.frwss.system.tax.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TaxEstimationResponseDto {

    private String module;
    private String category;

    private BigDecimal originalAmount;
    private BigDecimal taxRate;
    private BigDecimal estimatedTax;
    private BigDecimal totalWithTax;

    private String disclaimer;
    private LocalDateTime estimatedAt;
    private String estimatedBy;

    public TaxEstimationResponseDto() {
        this.disclaimer =
                "REFERENCE ONLY - This estimation must not be used for official tax filing.";
        this.estimatedAt = LocalDateTime.now();
    }

//    Getters and Setters
    public String getModule() {
        return module;
    }
    public void setModule(String module) {
        this.module = module;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }
    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }
    public BigDecimal getTaxRate() {
        return taxRate;
    }
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
    public BigDecimal getEstimatedTax() {
        return estimatedTax;
    }
    public void setEstimatedTax(BigDecimal estimatedTax) {
        this.estimatedTax = estimatedTax;
    }
    public BigDecimal getTotalWithTax() {
        return totalWithTax;
    }
    public void setTotalWithTax(BigDecimal totalWithTax) {
        this.totalWithTax = totalWithTax;
    }
    public String getDisclaimer() {
        return disclaimer;
    }
    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
    public LocalDateTime getEstimatedAt() {
        return estimatedAt;
    }
    public void setEstimatedAt(LocalDateTime estimatedAt) {
        this.estimatedAt = estimatedAt;
    }
    public String getEstimatedBy() {
        return estimatedBy;
    }
    public void setEstimatedBy(String estimatedBy) {
        this.estimatedBy = estimatedBy;
    }
}