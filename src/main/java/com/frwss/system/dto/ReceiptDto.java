package com.frwss.system.dto;

import java.math.BigDecimal;

public class ReceiptDto {

    //    Attributes
    private String receiptId;
    private String referenceNo;
    private String payerName;
    private BigDecimal amount;
    private String date;
    private String enteredBy;
    private boolean valid;
    private String errorMessage;

    //    Constructor
    public ReceiptDto(String receiptId, String referenceNo, String payerName, BigDecimal amount, String date, String enteredBy) {
        setReceiptId(receiptId);
        setReferenceNo(referenceNo);
        setPayerName(payerName);
        setAmount(amount);
        setDate(date);
        setEnteredBy(enteredBy);
        setValid(true);
        setErrorMessage("");
    }

//    Getters and Setter

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(String enteredBy) {
        this.enteredBy = enteredBy;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
