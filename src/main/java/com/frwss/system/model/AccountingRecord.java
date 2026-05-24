package com.frwss.system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="accounting_records")
public class AccountingRecord {

    @Id
    @Column(name="record_id")
    private String recordId;

    @Column(name="reference_no")
    private String referenceNo;

    @Column(name="module")
    private String module;

    private BigDecimal amount;

    @Column(name="data")
    private LocalDate recordDate;

    public String getRecordId(){ return recordId; }
    public void setRecordId(String x){ recordId=x; }

    public String getReferenceNo(){ return referenceNo; }
    public void setReferenceNo(String x){ referenceNo=x; }

    public String getModule(){ return module; }
    public void setModule(String x){ module=x; }

    public BigDecimal getAmount(){ return amount; }
    public void setAmount(BigDecimal x){ amount=x; }

    public LocalDate getRecordDate(){ return recordDate; }
    public void setRecordDate(LocalDate x){ recordDate=x; }

}