package com.frwss.system.tax.dto;

import java.math.BigDecimal;

public class TaxEstimationRequestDto {

    private String module;
    private String category;
    private BigDecimal amount;

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
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}