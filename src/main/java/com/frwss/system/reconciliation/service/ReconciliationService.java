package com.frwss.system.reconciliation.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
import com.frwss.system.reconciliation.dto.OrphanRecordView;
import com.frwss.system.reconciliation.dto.CandidateMatchView;
import com.frwss.system.reconciliation.model.MatchedSourceType;
import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.repository.ReconciliationResultRepository;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frwss.system.workflow.service.WorkflowService;



import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    private final WorkflowService workflowService;

    public ReconciliationService(
            AccountingRecordRepository accountingRecordRepository,
            PayrollRepository payrollRepository,
            ReceiptRepository receiptRepository,
            StockPurchaseRepository stockPurchaseRepository,
            ReconciliationResultRepository reconciliationResultRepository,
            AuditService auditService,
            WorkflowService workflowService
    ) {
        this.accountingRecordRepository = accountingRecordRepository;
        this.payrollRepository = payrollRepository;
        this.receiptRepository = receiptRepository;
        this.stockPurchaseRepository = stockPurchaseRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.auditService = auditService;
        this.workflowService = workflowService;
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

        List<AccountingRecord> accountingRecords =
                accountingRecordRepository.findRecentRecordsWithoutReconciliationResult(PageRequest.of(0,10));

        int exactCount = 0;
        int orphanCount = 0;

        for(AccountingRecord accountingRecord : accountingRecords){
//            if(reconciliationResultRepository.existsByAccountingRecordId(accountingRecord.getRecordId())){
//                continue;
//            }

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


        List<ReconciliationResult> orphanResults =
                reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(
                        ReconciliationStatus.ORPHAN,
                        PageRequest.of(0,10)
                );
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
                "Partial reconciliation completed for latest " + orphanResults.size() +
                        " orphan records. Partial matches found : " + partialCount
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

    @Transactional(readOnly = true)
    public List<OrphanRecordView> getOrphanRecordViews(){
        List<ReconciliationResult> orphanResults = reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(ReconciliationStatus.ORPHAN);

        List<OrphanRecordView> orphanViews = new ArrayList<>();

        for(ReconciliationResult result : orphanResults){
            accountingRecordRepository.findById(result.getAccountingRecordId())
                    .ifPresent(accountingRecord -> orphanViews.add(
                            new OrphanRecordView(
                                    result.getId(),
                                    accountingRecord.getRecordId(),
                                    accountingRecord.getReferenceNo(),
                                    accountingRecord.getModule(),
                                    accountingRecord.getAmount(),
                                    accountingRecord.getRecordDate(),
                                    result.getRemarks()
                            )
                    ));
        }

        return orphanViews;
    }

    @Transactional(readOnly = true)
    public List<CandidateMatchView> getCandidateMatchesForOrphan(Long reconciliationResultId){
        Optional<ReconciliationResult> optionalResult = reconciliationResultRepository.findById(reconciliationResultId);

        if(optionalResult.isEmpty()){
            return new ArrayList<>();
        }

        ReconciliationResult orphanResult = optionalResult.get();

        if(orphanResult.getStatus() != ReconciliationStatus.ORPHAN){
            return new ArrayList<>();
        }

        Optional<AccountingRecord> optionalAccountingRecord = accountingRecordRepository.findById(orphanResult.getAccountingRecordId());

        if(optionalAccountingRecord.isEmpty()){
            return new ArrayList<>();
        }

        AccountingRecord accountingRecord = optionalAccountingRecord.get();

        int widerToleranceDays = 7;

        LocalDate startDate = accountingRecord.getRecordDate().minusDays(widerToleranceDays);
        LocalDate endDate = accountingRecord.getRecordDate().plusDays(widerToleranceDays);

        List<CandidateMatchView> candidates = new ArrayList<>();

        List<Payroll> payrollCandidates = payrollRepository.findBySalaryAndPaymentDateBetween(
                accountingRecord.getAmount(),
                startDate,
                endDate
        );

        for(Payroll payroll : payrollCandidates){
            long daysDifference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(),payroll.getPaymentDate())
            );

            candidates.add(new CandidateMatchView(
                    "PAYROLL",
                    payroll.getPayrollId(),
                    payroll.getReferenceNo(),
                    payroll.getSalary(),
                    payroll.getPaymentDate(),
                    payroll.getEmployeeName(),
                    calculateCandidateConfidence(accountingRecord.getReferenceNo(), payroll.getReferenceNo(), daysDifference),
                    "Same amount with date difference of " + daysDifference + " day(s)"
            ));
        }

        List<Receipt> receiptCandidates = receiptRepository.findByAmountAndReceiptDateBetween(
                accountingRecord.getAmount(),
                startDate,
                endDate
        );

        for(Receipt receipt : receiptCandidates){
            long daysDifference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), receipt.getReceiptDate())
            );

            candidates.add(new CandidateMatchView(
                    "RECEIPT",
                    receipt.getReceiptId(),
                    receipt.getReferenceNo(),
                    receipt.getAmount(),
                    receipt.getReceiptDate(),
                    receipt.getPayerName(),
                    calculateCandidateConfidence(accountingRecord.getReferenceNo(), receipt.getReferenceNo(), daysDifference),
                    "Same amount with date difference of " + daysDifference + " days(s)"
            ));
        }

        List<StockPurchase> stockPurchaseCandidates = stockPurchaseRepository.findByAmountAndPurchaseDateBetween(
                accountingRecord.getAmount(),
                startDate,
                endDate
        );

        for(StockPurchase stockPurchase : stockPurchaseCandidates){
            long daysDifference = Math.abs(
                    ChronoUnit.DAYS.between(accountingRecord.getRecordDate(), stockPurchase.getPurchaseDate())
            );

            candidates.add(new CandidateMatchView(
                    "STOCK_PURCHASE",
                    stockPurchase.getPurchaseId(),
                    stockPurchase.getInvoiceNo(),
                    stockPurchase.getAmount(),
                    stockPurchase.getPurchaseDate(),
                    stockPurchase.getVendorName(),
                    calculateCandidateConfidence(accountingRecord.getReferenceNo(), stockPurchase.getInvoiceNo(), daysDifference),
                    "Same amount with date difference of " + daysDifference + " day(s)"
            ));
        }
        candidates.sort((c1, c2) ->
                Double.compare(c2.getSuggestedConfidence(), c1.getSuggestedConfidence())
        );

        return candidates;
    }

    private double calculateCandidateConfidence(
            String accountingReference,
            String candidateReference,
            long daysDifference
    ){
        // base score because candidate already has the same amount
        double confidence = 60.0;

        // strong bonus only if full reference number matches only
        if(accountingReference != null && candidateReference != null){
            if(accountingReference.equalsIgnoreCase(candidateReference)){
                confidence += 20.0;
            }
        }

        // date proximity bonus
        if(daysDifference == 0){
            confidence += 15.0;
        } else if(daysDifference <= 3){
            confidence += 10.0;
        } else if (daysDifference <= 7){
            confidence += 5.0;
        }

        return Math.min(confidence, 95.0);
    }
    @Transactional
    public void resolveOrphanManually(
            Long reconciliationResultId,
            String sourceType,
            String matchedRecordId,
            Double confidenceScore,
            String justification,
            String performedBy
    ){
        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new RuntimeException("Reconciliation result not found"));

        if(result.getStatus() != ReconciliationStatus.ORPHAN){
            throw new RuntimeException("Only orphan records can be manually resolved");
        }

        MatchedSourceType matchedSourceType = MatchedSourceType.valueOf(sourceType);

        result.setMatchedRecordId(matchedRecordId);
        result.setMatchedSourceType(matchedSourceType);
        result.setStatus(ReconciliationStatus.MANUALLY_RESOLVED);
        result.setConfidenceScore(confidenceScore);
        result.setReconciledAt(LocalDateTime.now());
        result.setReconciledBy(performedBy);
        result.setRemarks(
                "Manually resolved with " + sourceType +
                        " record: " + matchedRecordId +
                        ". Justification: " + justification
        );

        reconciliationResultRepository.save(result);

        auditService.log(
                AuditActionType.MANUAL_MODIFICATION,
                "ReconciliationResult",
                result.getAccountingRecordId(),
                performedBy,
                "Manual orphan resolution completed. Matched with " +
                        sourceType + " record " + matchedRecordId +
                        ". Justification: " + justification
        );
    }
    @Transactional(readOnly = true)
    public List<ReconciliationResult> getManualReviewRecords(){
        return reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(
                ReconciliationStatus.MANUALLY_RESOLVED
        );
    }

    @Transactional
    public void approveManualResolution(
            Long reconciliationResultId,
            String reviewedBy
    ){
        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new RuntimeException("Reconciliation result not found"));

        if(result.getStatus() != ReconciliationStatus.MANUALLY_RESOLVED){
            throw new RuntimeException("Only manually resolved records can be approved");
        }

        result.setStatus(ReconciliationStatus.APPROVED);
        result.setReconciledAt(LocalDateTime.now());
        result.setReconciledBy(reviewedBy);
        result.setRemarks(
                result.getRemarks() + " Review decision: APPROVED by " + reviewedBy + "."
        );

        reconciliationResultRepository.save(result);

        auditService.log(
                AuditActionType.MANUAL_MODIFICATION,
                "ReconciliationResult",
                result.getAccountingRecordId(),
                reviewedBy,
                "Manual resolution approved for accounting record " +
                        result.getAccountingRecordId() +
                        ". Matched record: " + result.getMatchedRecordId()
        );
    }

    @Transactional
    public void rejectManualResolution(
            Long reconciliationResultId,
            String rejectionReason,
            String reviewedBy
    ){
        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new RuntimeException("Reconciliation result not found"));

        if(result.getStatus() != ReconciliationStatus.MANUALLY_RESOLVED){
            throw new RuntimeException("Only manually resolved records can be rejected");
        }

        result.setStatus(ReconciliationStatus.REJECTED);
        result.setReconciledAt(LocalDateTime.now());
        result.setReconciledBy(reviewedBy);
        result.setRemarks(
                result.getRemarks() +
                        " Review decision: REJECTED by " + reviewedBy +
                        ". Reason: " + rejectionReason
        );

        reconciliationResultRepository.save(result);

        auditService.log(
                AuditActionType.MANUAL_MODIFICATION,
                "ReconciliationResult",
                result.getAccountingRecordId(),
                reviewedBy,
                "Manual resolution rejected for accounting record " +
                        result.getAccountingRecordId() +
                        ". Reason: " + rejectionReason
        );

    }
}
