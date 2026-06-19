package com.frwss.system.reconciliation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OrphanRecordView {

    private Long reconciliationResultId;
    private String accountingRecordId;
    private String referenceNo;
    private String module;
    private BigDecimal amount;
    private LocalDate recordDate;
    private String remarks;

    public OrphanRecordView(
            Long reconciliationResultId,
            String accountingRecordId,
            String referenceNo,
            String module,
            BigDecimal amount,
            LocalDate recordDate,
            String remarks
    ){
        this.reconciliationResultId = reconciliationResultId;
        this.accountingRecordId = accountingRecordId;
        this.referenceNo = referenceNo;
        this.module = module;
        this.amount = amount;
        this.recordDate = recordDate;
        this.remarks = remarks;
    }

    public Long getReconciliationResultId(){
        return reconciliationResultId;
    }

    public String getAccountingRecordId(){
        return accountingRecordId;
    }

    public String getReferenceNo(){
        return referenceNo;
    }

    public String getModule(){
        return module;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public String getRemarks() {
        return remarks;
    }
}

