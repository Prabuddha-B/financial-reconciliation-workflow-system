package com.frwss.system.reconciliation.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
import com.frwss.system.reconciliation.dto.OrphanRecordView;
import com.frwss.system.reconciliation.dto.CandidateMatchView;
import com.frwss.system.reconciliation.model.BulkReconciliation;
import com.frwss.system.reconciliation.model.MatchedSourceType;
import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.repository.BulkReconciliationRepository;
import com.frwss.system.reconciliation.repository.ReconciliationResultRepository;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frwss.system.workflow.service.WorkflowService;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {
    private final AccountingRecordRepository accountingRecordRepository;
    private final PayrollRepository payrollRepository;
    private final ReceiptRepository receiptRepository;
    private final StockPurchaseRepository stockPurchaseRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final AuditService auditService;
    private final BulkReconciliationRepository bulkReconciliationRepository;

    private final WorkflowService workflowService;

    public ReconciliationService(
            AccountingRecordRepository accountingRecordRepository,
            PayrollRepository payrollRepository,
            ReceiptRepository receiptRepository,
            StockPurchaseRepository stockPurchaseRepository,
            ReconciliationResultRepository reconciliationResultRepository,
            AuditService auditService,
            WorkflowService workflowService,
            BulkReconciliationRepository bulkReconciliationRepository
    ) {
        this.accountingRecordRepository = accountingRecordRepository;
        this.payrollRepository = payrollRepository;
        this.receiptRepository = receiptRepository;
        this.stockPurchaseRepository = stockPurchaseRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.auditService = auditService;
        this.workflowService = workflowService;
        this.bulkReconciliationRepository = bulkReconciliationRepository;
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

    // ONE-TO-MANY RECONCILIATION

    @Transactional
    public void runOneToManyReconciliation(String performedBy, int tolerancePercentage){
        // Log start
        auditService.log(
                AuditActionType.RECONCILIATION_STARTED,
                "ReconciliationRun",
                "ONE_TO_MANY_RUN",
                performedBy,
                "One-to-many reconciliation process started with tolerance: ±" + tolerancePercentage + "%"
        );

        // find all unreconciled bulk transactions
        List<AccountingRecord> bulkTransactions = accountingRecordRepository.findUnreconciledBulkTransactions();

        if(bulkTransactions.isEmpty()){
            // try to detect bulk transactions by pattern
            bulkTransactions = detectPotentialBulkTransactions();
        }

        int matchedCount = 0;
        int partialCount = 0;
        int unmatchedCount = 0;

        for(AccountingRecord bulk: bulkTransactions){
            // find children for this bulk
            List<AccountingRecord> children = findChildrenForBulk(bulk);

            if(children.isEmpty()){
                // try to find children by reference prefix
                children = findChildrenByReferencePrefix(bulk);
            }

            if(children.isEmpty()){
                unmatchedCount++;
                continue;
            }

            // calculate total child amount
            BigDecimal totalChildAmount = children.stream()
                    .map(AccountingRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Classify the match
            BulkReconciliation result = classifyBulkMatch(
                    bulk,
                    children,
                    totalChildAmount,
                    tolerancePercentage,
                    performedBy
            );

            // save the result
            bulkReconciliationRepository.save(result);

            // log based on status
            if(result.getStatus() == ReconciliationStatus.EXACT){
                matchedCount++;
                auditService.log(
                        AuditActionType.EXACT_MATCH_FOUND,
                        "BulkReconciliation",
                        result.getBulkRecordId(),
                        performedBy,
                        "One-to-many exact match found for bulk: " + bulk.getRecordId() +
                                ". Children: " + children.size() +
                                ", Total: " + totalChildAmount
                );
            } else if (result.getStatus() == ReconciliationStatus.PARTIAL){
                partialCount++;
                auditService.log(
                        AuditActionType.PARTIAL_MATCH_FOUND,
                        "BulkReconciliation",
                        result.getBulkRecordId(),
                        performedBy,
                        "One-to-many partial match found for bulk: " +
                                bulk.getRecordId() +
                                ". Children: " + children.size() +
                                ", Expected: " + bulk.getAmount() +
                                ", Actual: " + totalChildAmount +
                                ", Difference: " + result.getDifferenceAmount()
                );
            } else {
                unmatchedCount++;
                auditService.log(
                        AuditActionType.ORPHAN_RECORD_FOUND,
                        "BulkReconciliation",
                        result.getBulkRecordId(),
                        performedBy,
                        "One-to-many no match for bulk: " + bulk.getRecordId() +
                                ". Children: " + children.size() +
                                ", Expected: " + bulk.getAmount() +
                                ", Actual: " + totalChildAmount
                );
            }
        }

        // Log completion
        auditService.log(
                AuditActionType.RECONCILIATION_COMPLETED,
                "ReconciliationRun",
                "ONE_TO_MANY_RUN",
                performedBy,
                "One-to-many reconciliation completed. Matched: " +
                        matchedCount +
                        ", Partial: " + partialCount +
                        ", Unmatched: " + unmatchedCount
        );
    }

    // Helper methods for bulk
    /**
     * Detect bulk transactions by reference patterns
     */

    private List<AccountingRecord> detectPotentialBulkTransactions() {
        List<AccountingRecord> bulkTransactions = new ArrayList<>();

        // Common bulk transaction patterns
        String[] patterns = {"BULK", "TOTAL", "PAYROLL", "AGGREGATE", "SUMMARY"};

        for (String pattern : patterns) {
            List<AccountingRecord> results = accountingRecordRepository
                    .findPotentialBulkTransactionsByPattern("%" + pattern + "%");

            // Filter to only include records not already reconciled
            for (AccountingRecord record : results) {
                if (!reconciliationResultRepository.existsByAccountingRecordId(record.getRecordId()) &&
                        !bulkReconciliationRepository.existsByBulkRecordId(record.getRecordId())) {
                    bulkTransactions.add(record);
                }
            }
        }

        return bulkTransactions;
    }

    /**
     * Find children for a bulk transaction (using parent ID)
     */

    private List<AccountingRecord> findChildrenForBulk(AccountingRecord bulk) {
        return accountingRecordRepository.findChildrenByParentId(bulk.getRecordId());
    }

    /**
     * Find children by reference prefix
     */
    private List<AccountingRecord> findChildrenByReferencePrefix(AccountingRecord bulk) {
        String prefix = extractReferencePrefix(bulk.getReferenceNo());
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>();
        }
        return accountingRecordRepository.findChildrenByReferencePrefix(prefix, bulk.getRecordId());
    }

    /**
     * Extract prefix from reference number
     * Example: PO-2026-BULK-001 → PO-2026-
     */
    private String extractReferencePrefix(String referenceNo) {
        if (referenceNo == null || referenceNo.isEmpty()) {
            return null;
        }

        // Remove the last part after the last dash
        int lastDash = referenceNo.lastIndexOf("-");
        if (lastDash > 0) {
            return referenceNo.substring(0, lastDash + 1);
        }

        // If no dash, try to find BULK or TOTAL pattern
        if (referenceNo.contains("BULK")) {
            return referenceNo.replace("BULK", "");
        }
        if (referenceNo.contains("TOTAL")) {
            return referenceNo.replace("TOTAL", "");
        }

        return referenceNo + "-";
    }

    /**
     * Classify a bulk match
     */
    private BulkReconciliation classifyBulkMatch(
            AccountingRecord bulk,
            List<AccountingRecord> children,
            BigDecimal totalChildAmount,
            int tolerancePercentage,
            String performedBy
    ){
        BulkReconciliation result = new BulkReconciliation();
        result.setBulkRecordId(bulk.getRecordId());
        result.setBulkReferenceNo(bulk.getReferenceNo());
        result.setTotalExpectedAmount(bulk.getAmount());
        result.setTotalMatchedAmount(totalChildAmount);
        result.setChildCount(children.size());

        // Calculate difference
        BigDecimal difference = bulk.getAmount().subtract(totalChildAmount).abs();
        result.setDifferenceAmount(difference);

        // Calculate difference percentage
        BigDecimal differencePercent = BigDecimal.ZERO;
        if (bulk.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            differencePercent = difference
                    .divide(bulk.getAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
        }

        // Determine status based on tolerance
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            // Exact match
            result.setStatus(ReconciliationStatus.EXACT);
            result.setConfidenceScore(100.0);
            result.setMatchedChildCount(children.size());
            result.setRemarks("One-to-many exact match. All children sum to bulk amount.");

        } else if (differencePercent.doubleValue() <= tolerancePercentage) {
            // Partial match within tolerance
            result.setStatus(ReconciliationStatus.PARTIAL);
            result.setConfidenceScore(calculateBulkConfidence(differencePercent.doubleValue(), tolerancePercentage));
            result.setMatchedChildCount(children.size());
            result.setRemarks(String.format(
                    "One-to-many partial match. Difference: %.2f (%s%%). Tolerance: %s%%",
                    difference.doubleValue(),
                    differencePercent.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                    tolerancePercentage
            ));

        } else {
            // No match - exceeds tolerance
            result.setStatus(ReconciliationStatus.ORPHAN);
            result.setConfidenceScore(0.0);
            result.setMatchedChildCount(0);
            result.setRemarks(String.format(
                    "One-to-many no match. Difference: %.2f (%s%%) exceeds tolerance %s%%",
                    difference.doubleValue(),
                    differencePercent.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                    tolerancePercentage
            ));
        }

        // Store child record IDs as comma-separated string
        String childIds = children.stream()
                .map(AccountingRecord::getRecordId)
                .collect(Collectors.joining(","));
        result.setChildRecordIds(childIds);

        // Set audit fields
        result.setReconciledAt(LocalDateTime.now());
        result.setReconciledBy(performedBy);

        return result;
    }

    /**
     * Calculate confidence score for bulk reconciliation
     */

    private Double calculateBulkConfidence(double differencePercent, int tolerancePercentage) {
        if (tolerancePercentage <= 0) {
            return 0.0;
        }

        // Confidence = 100 - (differencePercentage / tolerancePercentage * 30)
        // This gives a score between 70-100 for partial matches
        double confidence = 100.0 - (differencePercent / tolerancePercentage * 30.0);

        // Ensure confidence stays within reasonable bounds
        return Math.max(Math.min(confidence, 95.0), 70.0);
    }

    // Get Bulk Reconciliation Results

    @Transactional(readOnly = true)
    public List<BulkReconciliation> getAllBulkReconciliationResults() {
        return bulkReconciliationRepository.findAllOrderByReconciledAtDesc();
    }

    @Transactional(readOnly = true)
    public List<BulkReconciliation> getBulkReconciliationByStatus(ReconciliationStatus status) {
        return bulkReconciliationRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public long getBulkReconciliationCount() {
        return bulkReconciliationRepository.count();
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

        List<String> accountingRecordsIds = orphanResults.stream()
                .map(ReconciliationResult::getAccountingRecordId)
                .toList();

        Map<String, AccountingRecord> recordMap = accountingRecordRepository.findAllById(accountingRecordsIds)
                .stream()
                .collect(Collectors.toMap(AccountingRecord::getRecordId, r -> r));

        List<OrphanRecordView> orphanViews = new ArrayList<>();

        for(ReconciliationResult result : orphanResults){
            AccountingRecord accountingRecord = recordMap.get(result.getAccountingRecordId());
            if(accountingRecord != null){
                orphanViews.add(new OrphanRecordView(
                   result.getId(),
                   accountingRecord.getRecordId(),
                   accountingRecord.getReferenceNo(),
                   accountingRecord.getModule(),
                   accountingRecord.getAmount(),
                   accountingRecord.getRecordDate(),
                   result.getRemarks()
                ));
            }
        }

        return orphanViews;
    }

    // Used in Side by Side UI
    @Transactional(readOnly = true)
    public OrphanRecordView getOrphanRecordView(Long reconciliationResultId) {

        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new RuntimeException("Reconciliation result not found"));

        AccountingRecord accountingRecord = accountingRecordRepository
                .findById(result.getAccountingRecordId())
                .orElseThrow(() -> new RuntimeException("Accounting record not found"));

        return new OrphanRecordView(
                result.getId(),
                accountingRecord.getRecordId(),
                accountingRecord.getReferenceNo(),
                accountingRecord.getModule(),
                accountingRecord.getAmount(),
                accountingRecord.getRecordDate(),
                result.getRemarks()
        );
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

    @Transactional
    public ReconciliationResult sendToWorkflowReview(
            Long reconciliationResultId,
            String performedBy,
            String comments
    ){
        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new RuntimeException("Reconciliation result not found"));

        // Only allow MANUALLY_RESOLVED or PENDING_APPROVAL records
        if(result.getStatus() != ReconciliationStatus.MANUALLY_RESOLVED){
            throw new RuntimeException("Only manually resolved records can be send to workflow");
        }

        // create workflow request
        WorkflowRequestDto workflowRequest = new WorkflowRequestDto();
        workflowRequest.setSourceType("RECONCILIATION");
        workflowRequest.setSourceRecordId(reconciliationResultId.toString());
        workflowRequest.setCreatedBy(performedBy);
        workflowRequest.setComments(comments);

        // Call workflow service
        WorkflowResponseDto workflowResponse =workflowService.createWorkflow(workflowRequest);

        // Update reconciliation result
        result.setStatus(ReconciliationStatus.PENDING_APPROVAL);
        result.setWorkflowId(workflowResponse.getWorkflowId());
        result.setSubmittedToWorkflowAt(LocalDateTime.now());
        result.setSubmittedBy(performedBy);

        // Save and return
        reconciliationResultRepository.save(result);

        // Audit log
        auditService.log(
                AuditActionType.WORKFLOW_STATE_CHANGED,
                "ReconciliationResult",
                result.getAccountingRecordId(),
                performedBy,
                "Submitted to workflow review with ID: " + workflowResponse.getWorkflowId()
        );

        return result;
    }
}
