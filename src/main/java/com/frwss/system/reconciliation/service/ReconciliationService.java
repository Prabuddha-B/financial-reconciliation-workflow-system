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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core of the reconciliation module. It compares accounting records with the source records
 * (payroll, receipts and stock purchases) in four steps:
 *
 * 1. Exact matching: same reference number, amount and date. The record becomes EXACT, or ORPHAN
 * when nothing matches.
 *
 * 2. Partial matching: same reference and amount, but the date may differ by a few days. An ORPHAN
 * is upgraded to PARTIAL with a confidence score.
 *
 * 3. One-to-many matching: one bulk record is compared with the sum of its child records.
 *
 * 4. Manual resolution: a reviewer settles an orphan, then the result goes to the workflow module
 * for approval.
 *
 * Every step is written to the audit log.
 */
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

    /**
     * Exact matching run. Takes the 10 most recent accounting records that have no reconciliation
     * result yet (so each click processes the next batch of 10) and looks for a source record with
     * the SAME reference number, amount and date. Each record gets a result: EXACT (confidence
     * 100) or ORPHAN (nothing matched). A start entry, one entry per record and a completion entry
     * are written to the audit log.
     *
     * @param performedBy username stored on every result and audit entry
     */
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

    /**
     * Partial matching run. Takes the 10 most recent ORPHAN results and tries again, this time
     * allowing the source record's date to differ by up to toleranceDays (the reference and amount
     * must still be identical). A hit upgrades the orphan to PARTIAL and sets a confidence score
     * that falls as the date gap grows (see calculateConfidence). Records that still have no
     * candidate stay ORPHAN.
     *
     * @param performedBy username stored on the results and audit entries
     * @param toleranceDays how many days earlier or later a source record may be
     */
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

    /**
     * Looks up the child accounting records behind each bulk reconciliation result so the bulk
     * results page can show them in the detail panel. The children are read from the comma
     * separated {@code childRecordIds} stored on every result, using a single query for all results.
     *
     * @param bulkResults the bulk results being displayed
     * @return a map of bulk result id to its child records, in the stored order. Every result has
     *         an entry; the list is empty when it has no stored children or a child record no
     *         longer exists.
     */
    @Transactional(readOnly = true)
    public Map<Long, List<AccountingRecord>> getChildRecordsForBulkResults(List<BulkReconciliation> bulkResults) {
        Map<Long, List<String>> idsByResult = new LinkedHashMap<>();
        List<String> allIds = new ArrayList<>();

        for (BulkReconciliation result : bulkResults) {
            List<String> ids = new ArrayList<>();
            String stored = result.getChildRecordIds();
            if (stored != null && !stored.isBlank()) {
                Arrays.stream(stored.split(","))
                        .map(String::trim)
                        .filter(id -> !id.isEmpty())
                        .forEach(ids::add);
            }
            idsByResult.put(result.getId(), ids);
            allIds.addAll(ids);
        }

        Map<String, AccountingRecord> recordsById = new HashMap<>();
        if (!allIds.isEmpty()) {
            for (AccountingRecord record : accountingRecordRepository.findAllById(allIds)) {
                recordsById.put(record.getRecordId(), record);
            }
        }

        Map<Long, List<AccountingRecord>> childrenByResult = new LinkedHashMap<>();
        idsByResult.forEach((resultId, ids) -> {
            List<AccountingRecord> children = new ArrayList<>();
            for (String id : ids) {
                AccountingRecord child = recordsById.get(id);
                if (child != null) {
                    children.add(child);
                }
            }
            childrenByResult.put(resultId, children);
        });
        return childrenByResult;
    }

    // Get Bulk Reconciliation Results

    @Transactional(readOnly = true)
    public List<BulkReconciliation> getAllBulkReconciliationResults() {
        return bulkReconciliationRepository.findAllOrderByReconciledAtDesc();
    }

    /**
     * Bulk (one-to-many) results with the given status, for the Bulk Results page.
     */
    @Transactional(readOnly = true)
    public List<BulkReconciliation> getBulkReconciliationByStatus(ReconciliationStatus status) {
        return bulkReconciliationRepository.findByStatus(status);
    }

    /**
     * Number of bulk (one-to-many) results stored so far.
     */
    @Transactional(readOnly = true)
    public long getBulkReconciliationCount() {
        return bulkReconciliationRepository.count();
    }

    /**
     * Looks for an exact match for one accounting record. Payroll is checked first, then receipts,
     * then stock purchases, and the search stops at the first hit. Returns an EXACT result when
     * found, otherwise an ORPHAN result. The result is built here; the caller saves it.
     */
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

    /**
     * Looks for a partial match for one orphan. Payroll, receipts and stock purchases are searched
     * in that order for a record with the same reference and amount whose date lies within the
     * tolerance window; the one closest in date wins. On a hit the existing ORPHAN result is
     * updated to PARTIAL (no new row is created) and returned. Returns null when no source has a
     * candidate.
     *
     * @param toleranceDays size of the date window on each side of the accounting record's date
     */
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

    /**
     * Creates the result row for an exact match: status EXACT, confidence 100, and the reference,
     * amount and date all marked MATCH.
     */
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

        // Set individual field match statuses
        result.setReferenceMatchStatus("MATCH");
        result.setAmountMatchStatus("MATCH");
        result.setDateMatchStatus("MATCH");

        return result;
    }

    /**
     * Creates the result row for a record with no match: status ORPHAN, no matched record,
     * confidence 0, and the reference, amount and date all marked MISMATCH.
     */
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

        // Set individual field match statuses for orphan
        result.setReferenceMatchStatus("MISMATCH");
        result.setAmountMatchStatus("MISMATCH");
        result.setDateMatchStatus("MISMATCH");

        return result;
    }

    /**
     * Picks the candidate whose date is closest to the accounting record's date. Candidates on the
     * SAME day (difference 0) are skipped on purpose, because exact matching would already have
     * found them. Returns null when no usable candidate exists.
     */
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

    /**
     * Picks the candidate whose date is closest to the accounting record's date. Candidates on the
     * SAME day (difference 0) are skipped on purpose, because exact matching would already have
     * found them. Returns null when no usable candidate exists.
     */
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

    /**
     * Picks the candidate whose date is closest to the accounting record's date. Candidates on the
     * SAME day (difference 0) are skipped on purpose, because exact matching would already have
     * found them. Returns null when no usable candidate exists.
     */
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

    /**
     * Turns an existing ORPHAN result into a PARTIAL match: stores the matched record, its type
     * and the confidence, marks the reference and amount as MATCH and the date as PARTIAL, and
     * records who did it and when.
     */
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

        // Set individual field match statuses for partial match
        result.setReferenceMatchStatus("MATCH"); // Reference matched
        result.setAmountMatchStatus("MATCH");    // Amount matched
        result.setDateMatchStatus("PARTIAL");    // Date is within tolerance
    }

    /**
     * Confidence score of a partial match. Same day gives 100. Otherwise 100 minus a penalty of
     * (30 / toleranceDays) for every day of difference, never lower than 70. Example: with a 3-day
     * tolerance, a record 2 days off scores 100 - 2 x 10 = 80.
     */
    private double calculateConfidence(long daysDifference, int toleranceDays){
        if(daysDifference == 0){
            return 100.0;
        }

        double penaltyPerDay = 30.0 / toleranceDays;
        double confidence = 100.0 - (daysDifference * penaltyPerDay);

        return Math.max(confidence, 70.0);
    }

    /**
     * All reconciliation results, newest first.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationResult> getAllResults(){
        return reconciliationResultRepository.findAllByOrderByReconciledAtDesc();
    }

    /**
     * Reconciliation results with the given status (EXACT, PARTIAL, ORPHAN, ...).
     */
    @Transactional(readOnly = true)
    public List<ReconciliationResult> getResultsByStatus(ReconciliationStatus status){
        return reconciliationResultRepository.findByStatus(status);
    }

    /**
     * Builds the rows for the Orphans page: every ORPHAN result joined with the reference, amount,
     * module and date of its accounting record. The records are loaded with one query for the
     * whole list instead of one query per row.
     */
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

    /**
     * Suggests possible matches for a reviewer who is resolving one orphan by hand. Payroll,
     * receipts and stock purchases are searched for records with the SAME amount whose date is
     * within 7 days (a wider window than automatic partial matching, and the reference does not
     * have to match). Each candidate is scored by calculateCandidateConfidence and the list is
     * returned best first. Returns an empty list when the result does not exist or is no longer an
     * orphan.
     */
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

    /**
     * Score shown next to a manual-review candidate (it is only a hint and is never used for
     * automatic matching). Base 60 because the amount already matches, plus 20 if the reference
     * numbers are equal, plus a date bonus (15 same day, 10 within 3 days, 5 within 7 days).
     * Capped at 95.
     */
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
    /**
     * A reviewer settles an orphan by choosing the source record it belongs to. Only ORPHAN
     * results can be resolved. Stores the chosen record, its source type and the confidence, sets
     * the status to MANUALLY_RESOLVED, keeps the justification in the remarks and writes a
     * MANUAL_MODIFICATION audit entry. A second person still has to approve the decision through
     * the workflow.
     */
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
    /**
     * Results waiting for review (status MANUALLY_RESOLVED), newest first.
     */
    @Transactional(readOnly = true)
    public List<ReconciliationResult> getManualReviewRecords(){
        return reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(
                ReconciliationStatus.MANUALLY_RESOLVED
        );
    }

    /**
     * Marks a reviewed result APPROVED and adds the reviewer's name to the remarks. Accepts
     * MANUALLY_RESOLVED and also PENDING_APPROVAL, which is the status a result has after Send to
     * workflow; this is what lets the workflow's final VERIFIED step succeed. Writes an audit
     * entry. Called by the workflow module, not directly by a page button.
     */
    @Transactional
    public void approveManualResolution(
            Long reconciliationResultId,
            String reviewedBy
    ){
        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new RuntimeException("Reconciliation result not found"));

        // A record sent to workflow review is PENDING_APPROVAL, so both states are valid here
        if(result.getStatus() != ReconciliationStatus.MANUALLY_RESOLVED
                && result.getStatus() != ReconciliationStatus.PENDING_APPROVAL){
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

    /**
     * Marks a reviewed result REJECTED and adds the reviewer's name and the rejection reason to
     * the remarks. Accepts MANUALLY_RESOLVED and PENDING_APPROVAL, like approveManualResolution.
     * Writes an audit entry.
     */
    @Transactional
    public void rejectManualResolution(
            Long reconciliationResultId,
            String rejectionReason,
            String reviewedBy
    ){
        ReconciliationResult result = reconciliationResultRepository.findById(reconciliationResultId)
                .orElseThrow(() -> new RuntimeException("Reconciliation result not found"));

        // A record sent to workflow review is PENDING_APPROVAL, so both states are valid here
        if(result.getStatus() != ReconciliationStatus.MANUALLY_RESOLVED
                && result.getStatus() != ReconciliationStatus.PENDING_APPROVAL){
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

    /**
     * Submits a manually resolved result to the workflow module as a new workflow item (source
     * type RECONCILIATION). Only MANUALLY_RESOLVED results can be sent. The matched record,
     * confidence and justification travel with it (buildWorkflowDetails), together with the
     * sender's comment, so the approver can see why the match was chosen. The result then becomes
     * PENDING_APPROVAL and keeps the workflow id.
     *
     * @return the updated result
     */
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
        workflowRequest.setDetails(buildWorkflowDetails(result));

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

    // Builds the summary the workflow approver sees: which record was matched,
    // how confident the match is, and the justification given by the resolver
    private String buildWorkflowDetails(ReconciliationResult result){
        String details = "Accounting record: " + result.getAccountingRecordId()
                + " | Matched " + result.getMatchedSourceType()
                + " record: " + result.getMatchedRecordId()
                + " | Confidence: " + result.getConfidenceScore() + "%"
                + " | Resolved by: " + result.getReconciledBy()
                + " | " + result.getRemarks();

        // the workflow details column holds at most 2000 characters
        if(details.length() > 1900){
            details = details.substring(0, 1900) + "...";
        }

        return details;
    }
}
