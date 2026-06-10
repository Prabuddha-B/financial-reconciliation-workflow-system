package com.frwss.system.reconciliation.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
import com.frwss.system.reconciliation.model.MatchedSourceType;
import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.repository.ReconciliationResultRepository;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {
    private final AccountingRecordRepository accountingRecordRepository;
    private final PayrollRepository payrollRepository;
    private final ReceiptRepository receiptRepository;
    private final StockPurchaseRepository stockPurchaseRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final AuditService auditService;

    public ReconciliationService(
            AccountingRecordRepository accountingRecordRepository,
            PayrollRepository payrollRepository,
            ReceiptRepository receiptRepository,
            StockPurchaseRepository stockPurchaseRepository,
            ReconciliationResultRepository reconciliationResultRepository,
            AuditService auditService
    ) {
        this.accountingRecordRepository = accountingRecordRepository;
        this.payrollRepository = payrollRepository;
        this.receiptRepository = receiptRepository;
        this.stockPurchaseRepository = stockPurchaseRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void runExactReconciliation(String performedBy){

        auditService.log(
                AuditActionType.RECONCILIATION_STARTED,
                "ReconciliationRun",
                "EXACT_MATCH_RUN",
                performedBy,
                "Exact reconciliation process started"
        );

        List<AccountingRecord> accountingRecords = accountingRecordRepository.findAll();

        int exactCount = 0;
        int orphanCount = 0;

        for(AccountingRecord accountingRecord : accountingRecords){
            if(reconciliationResultRepository.existsByAccountingRecordId(accountingRecord.getRecordId())){
                continue;
            }

            ReconciliationResult result = reconcileSingleAccountingRecord(accountingRecord, performedBy);

            reconciliationResultRepository.save(result);

            if(result.getStatus() == ReconciliationStatus.EXACT){
                exactCount++;

                auditService.log(
                        AuditActionType.EXACT_MATCH_FOUND,
                        "ReconciliationResult",
                        result.getAccountingRecordId(),
                        performedBy,
                        result.getRemarks()
                );
            } else if (result.getStatus() == ReconciliationStatus.ORPHAN){
                orphanCount++;

                auditService.log(
                        AuditActionType.ORPHAN_RECORD_FOUND,
                        "ReconciliationResult",
                        result.getAccountingRecordId(),
                        performedBy,
                        result.getRemarks()
                );
            }
        }
        auditService.log(
                AuditActionType.RECONCILIATION_COMPLETED,
                "ReconciliationRun",
                "EXACT_MATCH_RUN",
                performedBy,
                "Exact reconciliation completed. Exact matches: " + exactCount + ", Orphans: " + orphanCount
        );
    }

    private ReconciliationResult reconcileSingleAccountingRecord(
            AccountingRecord accountingRecord,
            String performedBy
    ){
        Optional<Payroll> payrollMatch =
                payrollRepository.findFirstByReferenceNoAndSalaryAndPaymentDate(
                        accountingRecord.getReferenceNo(),
                        accountingRecord.getAmount(),
                        accountingRecord.getRecordDate()
                );
        if(payrollMatch.isPresent()){
            return buildExactResult(
                    accountingRecord,
                    payrollMatch.get().getPayrollId(),
                    MatchedSourceType.PAYROLL,
                    performedBy,
                    "Exact match found with payroll record: "+ payrollMatch.get().getPayrollId()
            );
        }

        Optional<Receipt> receiptMatch = receiptRepository.findFirstByReferenceNoAndAmountAndReceiptDate(
                accountingRecord.getReferenceNo(),
                accountingRecord.getAmount(),
                accountingRecord.getRecordDate()
        );

        if (receiptMatch.isPresent()){
            return buildExactResult(
                    accountingRecord,
                    receiptMatch.get().getReceiptId(),
                    MatchedSourceType.RECEIPT,
                    performedBy,
                    "Exact match found with receipt record: " + receiptMatch.get().getReceiptId()
            );
        }

        Optional<StockPurchase> stockPurchaseMatch = stockPurchaseRepository.findFirstByInvoiceNoAndAmountAndPurchaseDate(
                accountingRecord.getReferenceNo(),
                accountingRecord.getAmount(),
                accountingRecord.getRecordDate()
        );

        if(stockPurchaseMatch.isPresent()){
            return buildExactResult(
                    accountingRecord,
                    stockPurchaseMatch.get().getPurchaseId(),
                    MatchedSourceType.STOCK_PURCHASE,
                    performedBy,
                    "Exact match found with stock purchase record: " + stockPurchaseMatch.get().getPurchaseId()
            );
        }

        return buildOrphanResult(
                accountingRecord,
                performedBy,
                "No exact match found for accounting record: " + accountingRecord.getRecordId()
        );
    }

    private ReconciliationResult buildExactResult(
            AccountingRecord accountingRecord,
            String matchedRecordId,
            MatchedSourceType matchedSourceType,
            String performedBy,
            String remarks
    ) {
        ReconciliationResult result = new ReconciliationResult();
        result.setAccountingRecordId(accountingRecord.getRecordId());
        result.setMatchedRecordId(matchedRecordId);
        result.setMatchedSourceType(matchedSourceType);
        result.setStatus(ReconciliationStatus.EXACT);
        result.setConfidenceScore(100.0);
        result.setRemarks(remarks);
        result.setReconciledAt(LocalDateTime.now());
        result.setReconciledBy(performedBy);

        return result;
    }

    private ReconciliationResult buildOrphanResult(
            AccountingRecord accountingRecord,
            String performedBy,
            String remarks
    ) {
        ReconciliationResult result = new ReconciliationResult();
        result.setAccountingRecordId(accountingRecord.getRecordId());
        result.setMatchedRecordId(null);
        result.setMatchedSourceType(MatchedSourceType.NONE);
        result.setStatus(ReconciliationStatus.ORPHAN);
        result.setConfidenceScore(0.0);
        result.setRemarks(remarks);
        result.setReconciledAt(LocalDateTime.now());
        result.setReconciledBy(performedBy);

        return result;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResult> getAllResults(){
        return reconciliationResultRepository.findAllByOrderByReconciledAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResult> getResultsByStatus(ReconciliationStatus status){
        return reconciliationResultRepository.findByStatus(status);
    }
}
