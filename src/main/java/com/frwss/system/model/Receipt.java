package com.frwss.system.model;

public class Receipt {

//    Attributes
    private int receiptId;
    private String referenceNo;
    private String payerName;
    private double amount;
    private String date;
    private String enteredBy;

//    Constructor
    public Receipt(int receiptId, String referenceNo, String payerName, double amount, String date, String enteredBy) {
        setReceiptId(receiptId);
        setReferenceNo(referenceNo);
        setPayerName(payerName);
        setAmount(amount);
        setDate(date);
        setEnteredBy(enteredBy);
    }

//    Getters and Setter

    public int getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(int receiptId) {
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
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


}
