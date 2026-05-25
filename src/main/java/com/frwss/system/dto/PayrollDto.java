package com.frwss.system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayrollDto {

    private String payrollId;
    private String employeeId;
    private String employeeName;
    private BigDecimal salary;
    private LocalDate paymentDate;
    private String referenceNo;
    private String status;
    private LocalDateTime createdAt;
    private String enteredBy;

    private boolean valid;
    private String errorMessage;

    // Constructor
    public PayrollDto(String payrollId, String employeeId, String employeeName, BigDecimal salary, LocalDate paymentDate, String referenceNo, String status, LocalDateTime createdAt, String enteredBy) {
        setPayrollId(payrollId);
        setEmployeeId(employeeId);
        setEmployeeName(employeeName);
        setSalary(salary);
        setPaymentDate(paymentDate);
        setReferenceNo(referenceNo);
        setStatus(status);
        setCreatedAt(createdAt);
        setEnteredBy(enteredBy);
        setValid(true);
        setErrorMessage("");
    }

    // Getters and Setters

    public String getPayrollId() {
        return payrollId;
    }

    public void setPayrollId(String payrollId) {
        this.payrollId = payrollId;
    }

    public String getEmployeeId() {
        return employeeId;
    }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public BigDecimal getSalary() {
        return salary;
    }
    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getReferenceNo() {
        return referenceNo;
    }
    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
