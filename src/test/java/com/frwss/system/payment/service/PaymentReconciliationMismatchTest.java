package com.frwss.system.payment.service;

import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.payment.model.PaymentTransaction;
import com.frwss.system.payment.repository.PaymentTransactionRepository;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Covers the mismatch details returned by payment reconciliation, so the
 * dashboard can show exactly which payments did not match and why.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Reconciliation Mismatch Details Unit Tests")
class PaymentReconciliationMismatchTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private AccountingRecordRepository accountingRecordRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @InjectMocks
    private PaymentReconciliationService paymentReconciliationService;

    @BeforeEach
    void setUp() {
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");
    }

    @Test
    @DisplayName("Should report the paid amount, expected amount and difference when the amounts differ")
    void reconcileAllPayments_amountDiffers_reportsExpectedPaidAndDifference() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        PaymentTransaction payment = payment("TXN-010", "ACC-010", "100.00");
        AccountingRecord record = record("ACC-010", "999.99");

        when(paymentTransactionRepository.findAllSuccessful()).thenReturn(List.of(payment));
        when(accountingRecordRepository.findById("ACC-010")).thenReturn(Optional.of(record));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, results.get("unmatched"));

        List<Map<String, Object>> mismatches = mismatchesOf(results);
        assertEquals(1, mismatches.size());

        Map<String, Object> detail = mismatches.get(0);
        assertEquals("TXN-010", detail.get("transactionId"));
        assertEquals("ACC-010", detail.get("accountingRecordId"));
        assertEquals(new BigDecimal("100.00"), detail.get("paidAmount"));
        assertEquals(new BigDecimal("999.99"), detail.get("expectedAmount"));
        assertEquals(new BigDecimal("-899.99"), detail.get("difference"));
        assertEquals("Paid amount differs from the accounting record", detail.get("reason"));
    }

    @Test
    @DisplayName("Should report that the record was not found when the payment points to a missing record")
    void reconcileAllPayments_recordMissing_reportsRecordNotFound() {

        PaymentTransaction payment = payment("TXN-011", "ACC-9999", "500.00");

        when(paymentTransactionRepository.findAllSuccessful()).thenReturn(List.of(payment));
        when(accountingRecordRepository.findById("ACC-9999")).thenReturn(Optional.empty());

        Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();

        List<Map<String, Object>> mismatches = mismatchesOf(results);
        assertEquals(1, mismatches.size());
        assertEquals("Accounting record not found", mismatches.get(0).get("reason"));
        assertEquals(new BigDecimal("500.00"), mismatches.get(0).get("paidAmount"));
        assertFalse(mismatches.get(0).containsKey("expectedAmount"));
    }

    @Test
    @DisplayName("Should report that the payment is not linked when it has no accounting record ID")
    void reconcileAllPayments_noRecordId_reportsNotLinked() {

        PaymentTransaction payment = payment("TXN-012", null, "75.00");

        when(paymentTransactionRepository.findAllSuccessful()).thenReturn(List.of(payment));

        Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();

        List<Map<String, Object>> mismatches = mismatchesOf(results);
        assertEquals(1, mismatches.size());
        assertEquals("Payment is not linked to an accounting record", mismatches.get(0).get("reason"));
    }

    @Test
    @DisplayName("Should return an empty mismatch list when every payment matches")
    void reconcileAllPayments_allMatch_returnsNoMismatches() {

        PaymentTransaction payment = payment("TXN-013", "ACC-013", "175527.03");
        AccountingRecord record = record("ACC-013", "175527.03");

        when(paymentTransactionRepository.findAllSuccessful()).thenReturn(List.of(payment));
        when(accountingRecordRepository.findById("ACC-013")).thenReturn(Optional.of(record));

        Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();

        assertEquals(1, results.get("matched"));
        assertEquals(0, results.get("unmatched"));
        assertTrue(mismatchesOf(results).isEmpty());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mismatchesOf(Map<String, Object> results) {
        return (List<Map<String, Object>>) results.get("mismatches");
    }

    private PaymentTransaction payment(String transactionId, String recordId, String amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setAccountingRecordId(recordId);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setGateway("SIMULATED");
        return transaction;
    }

    private AccountingRecord record(String recordId, String amount) {
        AccountingRecord record = new AccountingRecord();
        record.setRecordId(recordId);
        record.setAmount(new BigDecimal(amount));
        return record;
    }
}