package com.frwss.system.reconciliation.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.reconciliation.model.MatchedSourceType;
import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.repository.BulkReconciliationRepository;
import com.frwss.system.reconciliation.repository.ReconciliationResultRepository;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import com.frwss.system.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reconciliation Service Unit Tests")
class ReconciliationServiceTest {

    @Mock
    private AccountingRecordRepository accountingRecordRepository;

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private StockPurchaseRepository stockPurchaseRepository;

    @Mock
    private ReconciliationResultRepository reconciliationResultRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private BulkReconciliationRepository bulkReconciliationRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // runExactReconciliation()
    // =========================================================================

    @Test
    @DisplayName("Should save an exact match when a payroll record matches")
    void runExactReconciliation_payrollMatches_savesExactResult() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord record = accountingRecord(
                "ACC-001", "REF-001", new BigDecimal("1500.00"), LocalDate.of(2026, 7, 1));

        Payroll payroll = new Payroll();
        payroll.setPayrollId("PAY-001");

        when(accountingRecordRepository.findRecentRecordsWithoutReconciliationResult(any()))
                .thenReturn(List.of(record));
        when(payrollRepository.findFirstByReferenceNoAndSalaryAndPaymentDate(
                "REF-001", new BigDecimal("1500.00"), LocalDate.of(2026, 7, 1)))
                .thenReturn(Optional.of(payroll));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runExactReconciliation("admin");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();

        assertEquals("ACC-001", saved.getAccountingRecordId());
        assertEquals(ReconciliationStatus.EXACT, saved.getStatus());
        assertEquals(MatchedSourceType.PAYROLL, saved.getMatchedSourceType());
        assertEquals("PAY-001", saved.getMatchedRecordId());
        assertEquals(100.0, saved.getConfidenceScore());
        assertEquals("admin", saved.getReconciledBy());

        verifyNoInteractions(receiptRepository, stockPurchaseRepository);

        verify(auditService).log(
                eq(AuditActionType.EXACT_MATCH_FOUND),
                eq("ReconciliationResult"),
                eq("ACC-001"),
                eq("admin"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should fall back to receipt matching when no payroll record matches")
    void runExactReconciliation_noPayrollMatch_fallsBackToReceipt() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord record = accountingRecord(
                "ACC-002", "REF-002", new BigDecimal("250.00"), LocalDate.of(2026, 7, 2));

        Receipt receipt = new Receipt();
        receipt.setReceiptId("RCT-001");

        when(accountingRecordRepository.findRecentRecordsWithoutReconciliationResult(any()))
                .thenReturn(List.of(record));
        when(payrollRepository.findFirstByReferenceNoAndSalaryAndPaymentDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(receiptRepository.findFirstByReferenceNoAndAmountAndReceiptDate(
                "REF-002", new BigDecimal("250.00"), LocalDate.of(2026, 7, 2)))
                .thenReturn(Optional.of(receipt));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runExactReconciliation("admin");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();

        assertEquals(ReconciliationStatus.EXACT, saved.getStatus());
        assertEquals(MatchedSourceType.RECEIPT, saved.getMatchedSourceType());
        assertEquals("RCT-001", saved.getMatchedRecordId());

        verifyNoInteractions(stockPurchaseRepository);
    }

    @Test
    @DisplayName("Should mark the record as an orphan when no source matches")
    void runExactReconciliation_noSourceMatches_marksOrphan() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord record = accountingRecord(
                "ACC-003", "REF-003", new BigDecimal("999.99"), LocalDate.of(2026, 7, 3));

        when(accountingRecordRepository.findRecentRecordsWithoutReconciliationResult(any()))
                .thenReturn(List.of(record));
        when(payrollRepository.findFirstByReferenceNoAndSalaryAndPaymentDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(receiptRepository.findFirstByReferenceNoAndAmountAndReceiptDate(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(stockPurchaseRepository.findFirstByInvoiceNoAndAmountAndPurchaseDate(any(), any(), any()))
                .thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runExactReconciliation("admin");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();

        assertEquals(ReconciliationStatus.ORPHAN, saved.getStatus());
        assertEquals(MatchedSourceType.NONE, saved.getMatchedSourceType());
        assertNull(saved.getMatchedRecordId());
        assertEquals(0.0, saved.getConfidenceScore());

        verify(auditService).log(
                eq(AuditActionType.ORPHAN_RECORD_FOUND),
                eq("ReconciliationResult"),
                eq("ACC-003"),
                eq("admin"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should log run start and completion even when there is nothing to reconcile")
    void runExactReconciliation_noRecordsToProcess_stillLogsStartAndCompletion() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(accountingRecordRepository.findRecentRecordsWithoutReconciliationResult(any()))
                .thenReturn(List.of());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runExactReconciliation("admin");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        verify(auditService).log(
                eq(AuditActionType.RECONCILIATION_STARTED),
                eq("ReconciliationRun"),
                eq("EXACT_MATCH_RUN"),
                eq("admin"),
                anyString()
        );
        verify(auditService).log(
                eq(AuditActionType.RECONCILIATION_COMPLETED),
                eq("ReconciliationRun"),
                eq("EXACT_MATCH_RUN"),
                eq("admin"),
                anyString()
        );

        verifyNoInteractions(reconciliationResultRepository);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private AccountingRecord accountingRecord(String recordId, String referenceNo,
                                              BigDecimal amount, LocalDate recordDate) {
        AccountingRecord record = new AccountingRecord();
        record.setRecordId(recordId);
        record.setReferenceNo(referenceNo);
        record.setAmount(amount);
        record.setRecordDate(recordDate);
        return record;
    }
}