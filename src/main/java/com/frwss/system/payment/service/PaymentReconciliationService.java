package com.frwss.system.payment.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.payment.model.PaymentTransaction;
import com.frwss.system.payment.repository.PaymentTransactionRepository;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to reconcile payment transactions with accounting records
 */
@Service
public class PaymentReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AccountingRecordRepository accountingRecordRepository;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    public PaymentReconciliationService(
            PaymentTransactionRepository paymentTransactionRepository,
            AccountingRecordRepository accountingRecordRepository,
            AuditService auditService,
            SecurityContextAdapter securityContextAdapter) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.accountingRecordRepository = accountingRecordRepository;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

    /**
     * Reconcile all successful payment transactions with accounting records
     */
    @Transactional
    public Map<String, Object> reconcileAllPayments(){
        String performedBy = securityContextAdapter.getCurrentUsername();
        log.info("🔄 Starting payment reconciliation");

        Map<String, Object> results = new HashMap<>();
        int matched = 0;
        int unmatched = 0;
        int errors = 0;

        List<PaymentTransaction> successfulTransactions = paymentTransactionRepository.findAllSuccessful();

        for (PaymentTransaction transaction : successfulTransactions) {
            try {
                if (reconcilePaymentTransaction(transaction)) {
                    matched++;
                } else {
                    unmatched++;
                }
            } catch (Exception e) {
                log.error("Error reconciling transaction {}: {}", transaction.getTransactionId(), e.getMessage());
                errors++;
            }
        }

        results.put("matched", matched);
        results.put("unmatched", unmatched);
        results.put("errors", errors);
        results.put("total", successfulTransactions.size());

        auditService.log(
                AuditActionType.PAYMENT_RECONCILIATION,
                "PaymentReconciliation",
                "BATCH",
                performedBy,
                String.format("Payment reconciliation completed. Matched: %d, Unmatched: %d, Errors: %d",
                        matched, unmatched, errors)
        );

        log.info("Payment reconciliation completed. Matched: {}, Unmatched: {}, Errors: {}", matched, unmatched, errors);
        return results;
    }

    /**
     * Reconcile a single payment transaction with its accounting record
     */
    @Transactional
    public boolean  reconcilePaymentTransaction(PaymentTransaction transaction){
        String performedBy = securityContextAdapter.getCurrentUsername();

        if (transaction.getAccountingRecordId() == null) {
            log.warn("Transaction {} has no accounting record ID", transaction.getTransactionId());
            return false;
        }

        // Find the accounting record
        AccountingRecord record = accountingRecordRepository.findById(transaction.getAccountingRecordId())
                .orElse(null);

        if (record == null) {
            log.warn("Accounting record {} not found for transaction {}",
                    transaction.getAccountingRecordId(), transaction.getTransactionId());
            return false;
        }

        // Check if amount matches
        if (record.getAmount().compareTo(transaction.getAmount()) != 0) {
            log.warn("Amount mismatch for transaction {}: expected {}, got {}",
                    transaction.getTransactionId(), record.getAmount(), transaction.getAmount());
            return false;
        }

        // Update the accounting record with payment info
        record.setPaymentTransactionId(transaction.getTransactionId());
        record.setPaymentStatus("PAID");
        record.setPaymentGateway(transaction.getGateway());
        record.setPaymentProcessedAt(transaction.getProcessedAt());
        record.setPaymentReconciled(true);

        accountingRecordRepository.save(record);

        auditService.log(
                AuditActionType.PAYMENT_RECONCILIATION,
                "PaymentTransaction",
                transaction.getTransactionId(),
                performedBy,
                String.format("Reconciled transaction %s with accounting record %s",
                        transaction.getTransactionId(), record.getRecordId())
        );

        return true;
    }

    /**
     * Get reconciliation statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getReconciliationStats(){
        Map<String, Object> stats = new HashMap<>();

        long totalTransactions = paymentTransactionRepository.count();
        long successfulTransactions = paymentTransactionRepository.countByStatus("SUCCESS");
        long pendingTransactions = paymentTransactionRepository.countByStatus("PENDING");
        long failedTransactions = paymentTransactionRepository.countByStatus("FAILED");
        long refundedTransactions = paymentTransactionRepository.countByStatus("REFUNDED");

        // Count reconciled records
        long reconciledRecords = accountingRecordRepository.count();
        // Note: You might want to add a method to count only reconciled records
        // For now, we'll just count all

        stats.put("totalTransactions", totalTransactions);
        stats.put("successfulTransactions", successfulTransactions);
        stats.put("pendingTransactions", pendingTransactions);
        stats.put("failedTransactions", failedTransactions);
        stats.put("refundedTransactions", refundedTransactions);
        stats.put("reconciledRecords", reconciledRecords);

        return stats;
    }
}
