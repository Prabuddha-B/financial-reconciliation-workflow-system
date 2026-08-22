package com.frwss.system.payment.dto;

import java.math.BigDecimal;

/**
 * Request DTO for initiating a payment
 */

public class PaymentRequestDto {

    private String accountingRecordId;
    private BigDecimal amount;
    private String currency = "LKR";
    private String paymentMethod; // CARD, BANK_TRANSFER, CASH
    private String customerName;
    private String customerEmail;
    private String description;
    private String gateway = "SIMULATED";

    // ========= Getters and Setters

    public String getAccountingRecordId() {
        return accountingRecordId;
    }

    public void setAccountingRecordId(String accountingRecordId) {
        this.accountingRecordId = accountingRecordId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
}
