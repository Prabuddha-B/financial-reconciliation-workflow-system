package com.frwss.system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="payroll")
public class Payroll {

    @Id
    @Column(name="payroll_id")
    private String payrollId;

    @Column(name="employee_id")
    private String employeeId;

    @Column(name="employee_name")
    private String employeeName;

    private BigDecimal salary;

    @Column(name="payment_date")
    private LocalDate paymentDate;

    @Column(name="reference_no")
    private String referenceNo;

    private String status;

    public String getPayrollId(){return payrollId;}
    public void setPayrollId(String x){payrollId=x;}

    public String getEmployeeId(){return employeeId;}
    public void setEmployeeId(String x){employeeId=x;}

    public String getEmployeeName(){return employeeName;}
    public void setEmployeeName(String x){employeeName=x;}

    public BigDecimal getSalary(){return salary;}
    public void setSalary(BigDecimal x){salary=x;}

    public LocalDate getPaymentDate(){return paymentDate;}
    public void setPaymentDate(LocalDate x){paymentDate=x;}

    public String getReferenceNo(){return referenceNo;}
    public void setReferenceNo(String x){referenceNo=x;}

    public String getStatus(){return status;}
    public void setStatus(String x){status=x;}

}