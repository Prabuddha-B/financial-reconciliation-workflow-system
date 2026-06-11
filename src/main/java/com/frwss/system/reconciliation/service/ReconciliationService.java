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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

    @Transactional
    public void runPartialReconciliation(String performedBy, int toleranceDays){
        auditService.log(
                AuditActionType.RECONCILIATION_STARTED,
                "ReconciliationRun",
                "PARTIAL_MATCH_RUN",
                performedBy,
                "Partial reconciliation process started with date tolerance ±" + toleranceDays + " days"
        );

        List<ReconciliationResult> orphanResults = reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(ReconciliationStatus.ORPHAN);
        int partialCount = 0;

        for(ReconciliationResult orphanResult : orphanResults){
            Optional<AccountingRecord> optionalAccountingRecord = accountingRecordRepository.findById(orphanResult.getAccountingRecordId());

            if(optionalAccountingRecord.isEmpty()){
                continue;
            }

            AccountingRecord accountingRecord = optionalAccountingRecord.get();

            ReconciliationResult partialResult = findPartialMatchForAccountingRecord(accountingRecord, orphanResult, performedBy, toleranceDays);

            if(partialResult != null){
                reconciliationResultRepository.save(partialResult);
                partialCount++;

                auditService.log(
                        AuditActionType.PARTIAL_MATCH_FOUND,
                        "ReconciliationResult",
                        partialResult.getAccountingRecordId(),
                        performedBy,
                        partialResult.getRemarks()
                );
            }
        }

        auditService.log(
                AuditActionType.RECONCILIATION_COMPLETED,
                "ReconciliationRun",
                "PARTIAL_MATCH_RUN",
                performedBy,
                "Partial reconciliation completed. Partial matches found: " + partialCount
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

    private ReconciliationResult findPartialMatchForAccountingRecord(
            AccountingRecord accountingRecord,
            ReconciliationResult existingResult,
            String performedBy,
            int toleranceDays
    ) {
        LocalDate startDate = accountingRecord.getRecordDate().minusDays(toleranceDays);
        LocalDate endDate = accountingRecord.getRecordDate().plusDays(toleranceDays);

        List<Payroll> payrollMatches = payrollRepository.findByReferenceNoAndSalaryAndPaymentDateBetween(
                accountingRecord.getReferenceNo(),
                accountingRecord.getAmount(),
                startDate,
                endDate
        );

        Payroll bestPayrollMatch = findClosestPayrollMatch(accountingRecord, payrollMatches);

        if(bestPayrollMatch != null){
            long daysDifference = Math.abs(ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), bestPayrollMatch.getPaymentDate()));

            updateToPartialResult(
                    existingResult,
                    bestPayrollMatch.getPayrollId(),
                    MatchedSourceType.PAYROLL,
                    performedBy,
                    calculateConfidence(daysDifference, toleranceDays),
                    "Partial match found with payroll record: " + bestPayrollMatch.getPayrollId() + ". Date Difference: " +
                            daysDifference + " day(s)."
            );
            return existingResult;
        }

        List<Receipt> receiptMatches = receiptRepository.findByReferenceNoAndAmountAndReceiptDateBetween(
                accountingRecord.getReferenceNo(),
                accountingRecord.getAmount(),
                startDate,
                endDate
        );

        Receipt bestReceiptMatch = findClosestReceiptMatch(accountingRecord, receiptMatches);

        if(bestReceiptMatch != null){
            long daysDifference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), bestReceiptMatch.getReceiptDate())
            );

            updateToPartialResult(
                    existingResult,
                    bestReceiptMatch.getReceiptId(),
                    MatchedSourceType.RECEIPT,
                    performedBy,
                    calculateConfidence(daysDifference,toleranceDays),
                    "Partial match found with receipt record: " + bestReceiptMatch.getReceiptId() +
                            ". Date difference: " + daysDifference + " day(s)."
            );

            return existingResult;
        }

        List<StockPurchase> stockPurchaseMatches =
                stockPurchaseRepository.findByInvoiceNoAndAmountAndPurchaseDateBetween(
                        accountingRecord.getReferenceNo(),
                        accountingRecord.getAmount(),
                        startDate,
                        endDate
                );

        StockPurchase bestStockPurchaseMatch = findClosestStockPurchaseMatch(accountingRecord, stockPurchaseMatches);

        if(bestStockPurchaseMatch != null){
            long daysDifference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), bestStockPurchaseMatch.getPurchaseDate())
            );

            updateToPartialResult(
                    existingResult,
                    bestStockPurchaseMatch.getPurchaseId(),
                    MatchedSourceType.STOCK_PURCHASE,
                    performedBy,
                    calculateConfidence(daysDifference, toleranceDays),
                    "Partial match found with stock purchase record: " + bestStockPurchaseMatch.getPurchaseId() +
                            ". Date difference: " + daysDifference + " day(s)."
            );

            return existingResult;
        }

        return null;

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

    private Payroll findClosestPayrollMatch(AccountingRecord accountingRecord, List<Payroll> matches){
        Payroll closest =  null;
        long smallestDifference = Long.MAX_VALUE;

        for(Payroll match : matches){
            long difference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), match.getPaymentDate())
            );

            if(difference > 0 && difference < smallestDifference){
                smallestDifference = difference;
                closest = match;
            }
        }

        return closest;
    }

    private StockPurchase findClosestStockPurchaseMatch(AccountingRecord accountingRecord, List<StockPurchase> matches){
        StockPurchase closest =  null;
        long smallestDifference = Long.MAX_VALUE;

        for(StockPurchase match : matches){
            long difference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), match.getPurchaseDate())
            );

            if(difference > 0 && difference < smallestDifference){
                smallestDifference = difference;
                closest = match;
            }
        }

        return closest;
    }

    private Receipt findClosestReceiptMatch(AccountingRecord accountingRecord, List<Receipt> matches){
        Receipt closest = null;
        long smallestDifference = Long.MAX_VALUE;

        for(Receipt match : matches){
            long difference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), match.getReceiptDate())
            );

            if(difference > 0 && difference < smallestDifference){
                smallestDifference = difference;
                closest = match;
            }
        }

        return closest;
    }

    private void updateToPartialResult(
            ReconciliationResult result,
            String matchedRecordId,
            MatchedSourceType matchedSourceType,
            String performedBy,
            double confidenceScore,
            String remarks
    ){
        result.setMatchedRecordId(matchedRecordId);
        result.setMatchedSourceType(matchedSourceType);
        result.setStatus(ReconciliationStatus.PARTIAL);
        result.setConfidenceScore(confidenceScore);
        result.setRemarks(remarks);
        result.setReconciledAt(LocalDateTime.now());
        result.setReconciledBy(performedBy);
    }

    private double calculateConfidence(long daysDifference, int toleranceDays){
        if(daysDifference == 0){
            return 100.0;
        }

        double penaltyPerDay = 30.0 / toleranceDays;
        double confidence = 100.0 - (daysDifference * penaltyPerDay);

        return Math.max(confidence, 70.0);
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
