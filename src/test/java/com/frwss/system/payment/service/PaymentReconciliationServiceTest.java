package com.frwss.system.payment.service;

import com.frwss.system.audit.model.AuditActionType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Reconciliation Service Unit Tests")
class PaymentReconciliationServiceTest {

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
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // reconcilePaymentTransaction()
    // =========================================================================

    @Test
    @DisplayName("Should update the accounting record and return true when amounts match")
    void reconcilePaymentTransaction_amountMatches_updatesRecordAndReturnsTrue() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        PaymentTransaction transaction = paymentTransaction("TXN-001", "ACC-001", new BigDecimal("1500.00"));
        AccountingRecord record = accountingRecord("ACC-001", new BigDecimal("1500.00"));

        when(accountingRecordRepository.findById("ACC-001")).thenReturn(Optional.of(record));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result = paymentReconciliationService.reconcilePaymentTransaction(transaction);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(result);

        ArgumentCaptor<AccountingRecord> captor = ArgumentCaptor.forClass(AccountingRecord.class);
        verify(accountingRecordRepository).save(captor.capture());

        AccountingRecord saved = captor.getValue();
        assertEquals("TXN-001", saved.getPaymentTransactionId());
        assertEquals("PAID", saved.getPaymentStatus());
        assertEquals("SIMULATED", saved.getPaymentGateway());
        assertTrue(saved.getPaymentReconciled());

        verify(auditService).log(
                eq(AuditActionType.PAYMENT_RECONCILIATION),
                eq("PaymentTransaction"),
                eq("TXN-001"),
                eq("admin"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should return false and skip saving when the transaction has no accounting record ID")
    void reconcilePaymentTransaction_noAccountingRecordId_returnsFalseWithoutSaving() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        PaymentTransaction transaction = paymentTransaction("TXN-002", null, new BigDecimal("100.00"));

        boolean result = paymentReconciliationService.reconcilePaymentTransaction(transaction);

        assertFalse(result);
        verifyNoInteractions(accountingRecordRepository);
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Should return false and skip saving when the accounting record cannot be found")
    void reconcilePaymentTransaction_accountingRecordMissing_returnsFalseWithoutSaving() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        PaymentTransaction transaction = paymentTransaction("TXN-003", "ACC-999", new BigDecimal("200.00"));

        when(accountingRecordRepository.findById("ACC-999")).thenReturn(Optional.empty());

        boolean result = paymentReconciliationService.reconcilePaymentTransaction(transaction);

        assertFalse(result);
        verify(accountingRecordRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Should return false and skip saving when the transaction amount does not match the accounting record")
    void reconcilePaymentTransaction_amountMismatch_returnsFalseWithoutSaving() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        PaymentTransaction transaction = paymentTransaction("TXN-004", "ACC-004", new BigDecimal("100.00"));
        AccountingRecord record = accountingRecord("ACC-004", new BigDecimal("999.99"));

        when(accountingRecordRepository.findById("ACC-004")).thenReturn(Optional.of(record));

        boolean result = paymentReconciliationService.reconcilePaymentTransaction(transaction);

        assertFalse(result);
        verify(accountingRecordRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // =========================================================================
    // reconcileAllPayments()
    // =========================================================================

    @Test
    @DisplayName("Should tally matched, unmatched, and error counts across all successful transactions")
    void reconcileAllPayments_mixedOutcomes_tallysCountsAndAudits() {

        // ---------------------------------------------------------------------
        // Arrange: TXN-M matches, TXN-U has no linkable accounting record
        // ---------------------------------------------------------------------

        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        PaymentTransaction matching = paymentTransaction("TXN-M", "ACC-M", new BigDecimal("300.00"));
        PaymentTransaction unmatched = paymentTransaction("TXN-U", "ACC-U", new BigDecimal("400.00"));

        AccountingRecord matchingRecord = accountingRecord("ACC-M", new BigDecimal("300.00"));

        when(paymentTransactionRepository.findAllSuccessful()).thenReturn(List.of(matching, unmatched));
        when(accountingRecordRepository.findById("ACC-M")).thenReturn(Optional.of(matchingRecord));
        when(accountingRecordRepository.findById("ACC-U")).thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, results.get("matched"));
        assertEquals(1, results.get("unmatched"));
        assertEquals(0, results.get("errors"));
        assertEquals(2, results.get("total"));

        verify(accountingRecordRepository).save(matchingRecord);

        verify(auditService).log(
                eq(AuditActionType.PAYMENT_RECONCILIATION),
                eq("PaymentReconciliation"),
                eq("BATCH"),
                eq("admin"),
                contains("Matched: 1, Unmatched: 1, Errors: 0")
        );
    }

    @Test
    @DisplayName("Should count a transaction as an error when reconciling it throws an exception")
    void reconcileAllPayments_reconciliationThrows_countsAsError() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        PaymentTransaction transaction = paymentTransaction("TXN-ERR", "ACC-ERR", new BigDecimal("50.00"));

        when(paymentTransactionRepository.findAllSuccessful()).thenReturn(List.of(transaction));
        when(accountingRecordRepository.findById("ACC-ERR")).thenThrow(new RuntimeException("DB unavailable"));

        Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();

        assertEquals(0, results.get("matched"));
        assertEquals(0, results.get("unmatched"));
        assertEquals(1, results.get("errors"));
        assertEquals(1, results.get("total"));
    }

    @Test
    @DisplayName("Should report all-zero counts and still audit-log when there are no successful transactions")
    void reconcileAllPayments_noSuccessfulTransactions_reportsZeroCounts() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");
        when(paymentTransactionRepository.findAllSuccessful()).thenReturn(List.of());

        Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();

        assertEquals(0, results.get("matched"));
        assertEquals(0, results.get("unmatched"));
        assertEquals(0, results.get("errors"));
        assertEquals(0, results.get("total"));

        verify(auditService).log(
                eq(AuditActionType.PAYMENT_RECONCILIATION),
                eq("PaymentReconciliation"),
                eq("BATCH"),
                eq("admin"),
                anyString()
        );
    }

    // =========================================================================
    // getReconciliationStats()
    // =========================================================================

    @Test
    @DisplayName("Should aggregate transaction counts by status into the stats map")
    void getReconciliationStats_aggregatesCountsByStatus() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(paymentTransactionRepository.count()).thenReturn(10L);
        when(paymentTransactionRepository.countByStatus("SUCCESS")).thenReturn(6L);
        when(paymentTransactionRepository.countByStatus("PENDING")).thenReturn(1L);
        when(paymentTransactionRepository.countByStatus("FAILED")).thenReturn(2L);
        when(paymentTransactionRepository.countByStatus("REFUNDED")).thenReturn(1L);
        when(accountingRecordRepository.count()).thenReturn(20L);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Map<String, Object> stats = paymentReconciliationService.getReconciliationStats();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(10L, stats.get("totalTransactions"));
        assertEquals(6L, stats.get("successfulTransactions"));
        assertEquals(1L, stats.get("pendingTransactions"));
        assertEquals(2L, stats.get("failedTransactions"));
        assertEquals(1L, stats.get("refundedTransactions"));
        assertEquals(20L, stats.get("reconciledRecords"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PaymentTransaction paymentTransaction(String transactionId, String accountingRecordId, BigDecimal amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setAccountingRecordId(accountingRecordId);
        transaction.setAmount(amount);
        transaction.setGateway("SIMULATED");
        return transaction;
    }

    private AccountingRecord accountingRecord(String recordId, BigDecimal amount) {
        AccountingRecord record = new AccountingRecord();
        record.setRecordId(recordId);
        record.setAmount(amount);
        return record;
    }
}