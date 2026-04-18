package com.frwss.system.dto;

public class AccountingRecordDto {

    private String recordId;
    private String referenceNo;
    private String module;
    private double amount;
    private String recordDate;
    private String enteredBy;

    // Constructor
    public AccountingRecordDto(String recordId, String referenceNo, String module, double amount, String recordDate, String enteredBy) {
        this.recordId = recordId;
        this.referenceNo = referenceNo;
        this.module = module;
        this.amount = amount;
        this.recordDate = recordDate;
        this.enteredBy = enteredBy;
    }

    private boolean valid;
    private String errorMessage;

    // Getters & Setters

    public String getRecordId() {
        return recordId;
    }
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getReferenceNo() {
        return referenceNo;
    }
    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getModule() {
        return module;
    }
    public void setModule(String module) {
        this.module = module;
    }

    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getRecordDate() {
        return recordDate;
    }
    public void setRecordDate(String recordDate) {
        this.recordDate = recordDate;
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
