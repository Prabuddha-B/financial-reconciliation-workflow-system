package com.frwss.system.reconciliation.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
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
import com.frwss.system.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

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
    // runPartialReconciliation()
    // =========================================================================

    @Test
    @DisplayName("Should upgrade an orphan to PARTIAL when a payroll record matches within tolerance")
    void runPartialReconciliation_payrollWithinTolerance_upgradesOrphanToPartial() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord record = accountingRecord(
                "ACC-010", "REF-010", new BigDecimal("500.00"), LocalDate.of(2026, 7, 10));

        ReconciliationResult orphanResult = new ReconciliationResult();
        orphanResult.setId(10L);
        orphanResult.setAccountingRecordId("ACC-010");
        orphanResult.setStatus(ReconciliationStatus.ORPHAN);

        Payroll payroll = new Payroll();
        payroll.setPayrollId("PAY-010");
        payroll.setPaymentDate(LocalDate.of(2026, 7, 12)); // 2 days later, within a 3-day tolerance

        when(reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(
                eq(ReconciliationStatus.ORPHAN), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(orphanResult));
        when(accountingRecordRepository.findById("ACC-010")).thenReturn(Optional.of(record));
        when(payrollRepository.findByReferenceNoAndSalaryAndPaymentDateBetween(
                eq("REF-010"), eq(new BigDecimal("500.00")), any(), any()))
                .thenReturn(List.of(payroll));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runPartialReconciliation("admin", 3);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();
        assertEquals(ReconciliationStatus.PARTIAL, saved.getStatus());
        assertEquals(MatchedSourceType.PAYROLL, saved.getMatchedSourceType());
        assertEquals("PAY-010", saved.getMatchedRecordId());
        // 2 days late, 3-day tolerance: 100 - (2 * 30/3) = 80.0
        assertEquals(80.0, saved.getConfidenceScore());
        assertEquals("PARTIAL", saved.getDateMatchStatus());

        verify(auditService).log(
                eq(AuditActionType.PARTIAL_MATCH_FOUND),
                eq("ReconciliationResult"),
                eq("ACC-010"),
                eq("admin"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should fall back to receipt, then stock purchase, when payroll has no partial candidate")
    void runPartialReconciliation_noPayrollCandidate_fallsBackToReceiptThenStock() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord record = accountingRecord(
                "ACC-011", "REF-011", new BigDecimal("300.00"), LocalDate.of(2026, 7, 15));

        ReconciliationResult orphanResult = new ReconciliationResult();
        orphanResult.setId(11L);
        orphanResult.setAccountingRecordId("ACC-011");
        orphanResult.setStatus(ReconciliationStatus.ORPHAN);

        StockPurchase stockPurchase = new StockPurchase();
        stockPurchase.setPurchaseId("STK-011");
        stockPurchase.setPurchaseDate(LocalDate.of(2026, 7, 16)); // 1 day later

        when(reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(
                eq(ReconciliationStatus.ORPHAN), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(orphanResult));
        when(accountingRecordRepository.findById("ACC-011")).thenReturn(Optional.of(record));
        when(payrollRepository.findByReferenceNoAndSalaryAndPaymentDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(receiptRepository.findByReferenceNoAndAmountAndReceiptDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(stockPurchaseRepository.findByInvoiceNoAndAmountAndPurchaseDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of(stockPurchase));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runPartialReconciliation("admin", 5);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();
        assertEquals(MatchedSourceType.STOCK_PURCHASE, saved.getMatchedSourceType());
        assertEquals("STK-011", saved.getMatchedRecordId());
    }

    @Test
    @DisplayName("Should NOT treat a same-day candidate as a partial match (already handled by exact matching)")
    void runPartialReconciliation_onlySameDayCandidate_findsNoMatch() {

        // ---------------------------------------------------------------------
        // Arrange: findClosest*Match requires a strictly positive day difference,
        // so a same-day (0-day) candidate must be ignored here - it should have
        // already been picked up by exact reconciliation instead.
        // ---------------------------------------------------------------------

        AccountingRecord record = accountingRecord(
                "ACC-012", "REF-012", new BigDecimal("100.00"), LocalDate.of(2026, 7, 20));

        ReconciliationResult orphanResult = new ReconciliationResult();
        orphanResult.setId(12L);
        orphanResult.setAccountingRecordId("ACC-012");
        orphanResult.setStatus(ReconciliationStatus.ORPHAN);

        Payroll sameDayPayroll = new Payroll();
        sameDayPayroll.setPayrollId("PAY-012");
        sameDayPayroll.setPaymentDate(LocalDate.of(2026, 7, 20)); // 0 days difference

        when(reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(
                eq(ReconciliationStatus.ORPHAN), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(orphanResult));
        when(accountingRecordRepository.findById("ACC-012")).thenReturn(Optional.of(record));
        when(payrollRepository.findByReferenceNoAndSalaryAndPaymentDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of(sameDayPayroll));
        when(receiptRepository.findByReferenceNoAndAmountAndReceiptDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of());
        when(stockPurchaseRepository.findByInvoiceNoAndAmountAndPurchaseDateBetween(any(), any(), any(), any()))
                .thenReturn(List.of());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runPartialReconciliation("admin", 3);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        verify(reconciliationResultRepository, never()).save(any());
        verify(auditService, never()).log(
                eq(AuditActionType.PARTIAL_MATCH_FOUND), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip an orphan result whose accounting record no longer exists")
    void runPartialReconciliation_accountingRecordMissing_skipsWithoutError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReconciliationResult orphanResult = new ReconciliationResult();
        orphanResult.setId(13L);
        orphanResult.setAccountingRecordId("ACC-013");
        orphanResult.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findByStatusOrderByReconciledAtDesc(
                eq(ReconciliationStatus.ORPHAN), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(orphanResult));
        when(accountingRecordRepository.findById("ACC-013")).thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act & Assert - must not throw, must not attempt to save anything
        // ---------------------------------------------------------------------

        assertDoesNotThrow(() -> reconciliationService.runPartialReconciliation("admin", 3));

        verify(reconciliationResultRepository, never()).save(any());
        verifyNoInteractions(payrollRepository, receiptRepository, stockPurchaseRepository);
    }

    // =========================================================================
    // runOneToManyReconciliation()
    // =========================================================================

    @Test
    @DisplayName("Should record an EXACT bulk match when children sum exactly to the bulk amount")
    void runOneToManyReconciliation_childrenSumExactly_recordsExactMatch() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord bulk = accountingRecord(
                "ACC-BULK-01", "PO-2026-BULK-001", new BigDecimal("1000.00"), LocalDate.of(2026, 7, 1));

        AccountingRecord child1 = accountingRecord("ACC-C1", "C1", new BigDecimal("600.00"), LocalDate.of(2026, 7, 1));
        AccountingRecord child2 = accountingRecord("ACC-C2", "C2", new BigDecimal("400.00"), LocalDate.of(2026, 7, 1));

        when(accountingRecordRepository.findUnreconciledBulkTransactions())
                .thenReturn(List.of(bulk));
        when(accountingRecordRepository.findChildrenByParentId("ACC-BULK-01"))
                .thenReturn(List.of(child1, child2));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runOneToManyReconciliation("admin", 10);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<BulkReconciliation> captor = ArgumentCaptor.forClass(BulkReconciliation.class);
        verify(bulkReconciliationRepository).save(captor.capture());

        BulkReconciliation saved = captor.getValue();
        assertEquals(ReconciliationStatus.EXACT, saved.getStatus());
        assertEquals(100.0, saved.getConfidenceScore());
        assertEquals(2, saved.getMatchedChildCount());
        assertEquals(new BigDecimal("1000.00"), saved.getTotalMatchedAmount());

        verify(auditService).log(
                eq(AuditActionType.EXACT_MATCH_FOUND),
                eq("BulkReconciliation"),
                eq("ACC-BULK-01"),
                eq("admin"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should record a PARTIAL bulk match when children sum within tolerance but not exact")
    void runOneToManyReconciliation_childrenWithinTolerance_recordsPartialMatch() {

        // ---------------------------------------------------------------------
        // Arrange: bulk = 1000, children sum = 950 -> 5% difference, 10% tolerance
        // ---------------------------------------------------------------------

        AccountingRecord bulk = accountingRecord(
                "ACC-BULK-02", "PO-2026-BULK-002", new BigDecimal("1000.00"), LocalDate.of(2026, 7, 2));
        AccountingRecord child = accountingRecord("ACC-C3", "C3", new BigDecimal("950.00"), LocalDate.of(2026, 7, 2));

        when(accountingRecordRepository.findUnreconciledBulkTransactions())
                .thenReturn(List.of(bulk));
        when(accountingRecordRepository.findChildrenByParentId("ACC-BULK-02"))
                .thenReturn(List.of(child));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runOneToManyReconciliation("admin", 10);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<BulkReconciliation> captor = ArgumentCaptor.forClass(BulkReconciliation.class);
        verify(bulkReconciliationRepository).save(captor.capture());

        BulkReconciliation saved = captor.getValue();
        assertEquals(ReconciliationStatus.PARTIAL, saved.getStatus());
        // confidence = 100 - (5 / 10 * 30) = 85.0
        assertEquals(85.0, saved.getConfidenceScore());

        verify(auditService).log(
                eq(AuditActionType.PARTIAL_MATCH_FOUND),
                eq("BulkReconciliation"),
                eq("ACC-BULK-02"),
                eq("admin"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should classify as no-match when the difference exceeds tolerance")
    void runOneToManyReconciliation_childrenExceedTolerance_recordsNoMatch() {

        // ---------------------------------------------------------------------
        // Arrange: bulk = 1000, children sum = 500 -> 50% difference, 10% tolerance
        // ---------------------------------------------------------------------

        AccountingRecord bulk = accountingRecord(
                "ACC-BULK-03", "PO-2026-BULK-003", new BigDecimal("1000.00"), LocalDate.of(2026, 7, 3));
        AccountingRecord child = accountingRecord("ACC-C4", "C4", new BigDecimal("500.00"), LocalDate.of(2026, 7, 3));

        when(accountingRecordRepository.findUnreconciledBulkTransactions())
                .thenReturn(List.of(bulk));
        when(accountingRecordRepository.findChildrenByParentId("ACC-BULK-03"))
                .thenReturn(List.of(child));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runOneToManyReconciliation("admin", 10);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<BulkReconciliation> captor = ArgumentCaptor.forClass(BulkReconciliation.class);
        verify(bulkReconciliationRepository).save(captor.capture());

        BulkReconciliation saved = captor.getValue();
        assertEquals(ReconciliationStatus.ORPHAN, saved.getStatus());
        assertEquals(0.0, saved.getConfidenceScore());
        assertEquals(0, saved.getMatchedChildCount());

        verify(auditService).log(
                eq(AuditActionType.ORPHAN_RECORD_FOUND),
                eq("BulkReconciliation"),
                eq("ACC-BULK-03"),
                eq("admin"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should fall back to reference-prefix matching when no children are linked by parent ID")
    void runOneToManyReconciliation_noParentIdChildren_fallsBackToReferencePrefix() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord bulk = accountingRecord(
                "ACC-BULK-04", "PO-2026-BULK-004", new BigDecimal("200.00"), LocalDate.of(2026, 7, 4));
        AccountingRecord child = accountingRecord("ACC-C5", "PO-2026-BULK-C5", new BigDecimal("200.00"), LocalDate.of(2026, 7, 4));

        when(accountingRecordRepository.findUnreconciledBulkTransactions())
                .thenReturn(List.of(bulk));
        when(accountingRecordRepository.findChildrenByParentId("ACC-BULK-04"))
                .thenReturn(List.of()); // nothing linked directly by parent id
        when(accountingRecordRepository.findChildrenByReferencePrefix(eq("PO-2026-BULK-"), eq("ACC-BULK-04")))
                .thenReturn(List.of(child));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runOneToManyReconciliation("admin", 10);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<BulkReconciliation> captor = ArgumentCaptor.forClass(BulkReconciliation.class);
        verify(bulkReconciliationRepository).save(captor.capture());
        assertEquals(ReconciliationStatus.EXACT, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should count a bulk transaction as unmatched when no children are found by either method")
    void runOneToManyReconciliation_noChildrenFoundAtAll_countsAsUnmatched() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecord bulk = accountingRecord(
                "ACC-BULK-05", "PO-2026-BULK-005", new BigDecimal("300.00"), LocalDate.of(2026, 7, 5));

        when(accountingRecordRepository.findUnreconciledBulkTransactions())
                .thenReturn(List.of(bulk));
        when(accountingRecordRepository.findChildrenByParentId("ACC-BULK-05"))
                .thenReturn(List.of());
        when(accountingRecordRepository.findChildrenByReferencePrefix(anyString(), anyString()))
                .thenReturn(List.of());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.runOneToManyReconciliation("admin", 10);

        // ---------------------------------------------------------------------
        // Assert - no BulkReconciliation is created for a bulk record with no
        // children at all; it just contributes to the unmatched count in the
        // completion log.
        // ---------------------------------------------------------------------

        verify(bulkReconciliationRepository, never()).save(any());
        verify(auditService).log(
                eq(AuditActionType.RECONCILIATION_COMPLETED),
                eq("ReconciliationRun"),
                eq("ONE_TO_MANY_RUN"),
                eq("admin"),
                contains("Unmatched: 1")
        );
    }

    // =========================================================================
    // resolveOrphanManually()
    // =========================================================================

    @Test
    @DisplayName("Should resolve an orphan record and mark it MANUALLY_RESOLVED")
    void resolveOrphanManually_validOrphan_marksManuallyResolved() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(20L);
        orphan.setAccountingRecordId("ACC-020");
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findById(20L)).thenReturn(Optional.of(orphan));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.resolveOrphanManually(
                20L, "RECEIPT", "RCT-020", 90.0, "Confirmed via bank statement", "auditor1");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();
        assertEquals(ReconciliationStatus.MANUALLY_RESOLVED, saved.getStatus());
        assertEquals(MatchedSourceType.RECEIPT, saved.getMatchedSourceType());
        assertEquals("RCT-020", saved.getMatchedRecordId());
        assertEquals(90.0, saved.getConfidenceScore());
        assertTrue(saved.getRemarks().contains("Confirmed via bank statement"));

        verify(auditService).log(
                eq(AuditActionType.MANUAL_MODIFICATION),
                eq("ReconciliationResult"),
                eq("ACC-020"),
                eq("auditor1"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should throw when the reconciliation result does not exist")
    void resolveOrphanManually_resultNotFound_throwsRuntimeException() {

        when(reconciliationResultRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                reconciliationService.resolveOrphanManually(
                        999L, "RECEIPT", "RCT-999", 90.0, "n/a", "auditor1"));

        verify(reconciliationResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should refuse to resolve a record that isn't currently an orphan")
    void resolveOrphanManually_notAnOrphan_throwsRuntimeException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReconciliationResult alreadyExact = new ReconciliationResult();
        alreadyExact.setId(21L);
        alreadyExact.setStatus(ReconciliationStatus.EXACT);

        when(reconciliationResultRepository.findById(21L)).thenReturn(Optional.of(alreadyExact));

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        assertThrows(RuntimeException.class, () ->
                reconciliationService.resolveOrphanManually(
                        21L, "RECEIPT", "RCT-021", 90.0, "n/a", "auditor1"));

        verify(reconciliationResultRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // =========================================================================
    // approveManualResolution()
    // =========================================================================

    @Test
    @DisplayName("Should approve a manually resolved record")
    void approveManualResolution_manuallyResolvedRecord_marksApproved() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReconciliationResult manuallyResolved = new ReconciliationResult();
        manuallyResolved.setId(30L);
        manuallyResolved.setAccountingRecordId("ACC-030");
        manuallyResolved.setStatus(ReconciliationStatus.MANUALLY_RESOLVED);
        manuallyResolved.setRemarks("Manually resolved with RECEIPT record: RCT-030.");

        when(reconciliationResultRepository.findById(30L)).thenReturn(Optional.of(manuallyResolved));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.approveManualResolution(30L, "supervisor1");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();
        assertEquals(ReconciliationStatus.APPROVED, saved.getStatus());
        assertEquals("supervisor1", saved.getReconciledBy());
        assertTrue(saved.getRemarks().contains("APPROVED by supervisor1"));

        verify(auditService).log(
                eq(AuditActionType.MANUAL_MODIFICATION),
                eq("ReconciliationResult"),
                eq("ACC-030"),
                eq("supervisor1"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should refuse to approve a record that isn't manually resolved")
    void approveManualResolution_notManuallyResolved_throwsRuntimeException() {

        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(31L);
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findById(31L)).thenReturn(Optional.of(orphan));

        assertThrows(RuntimeException.class, () ->
                reconciliationService.approveManualResolution(31L, "supervisor1"));

        verify(reconciliationResultRepository, never()).save(any());
    }

    // =========================================================================
    // rejectManualResolution()
    // =========================================================================

    @Test
    @DisplayName("Should reject a manually resolved record with a reason")
    void rejectManualResolution_manuallyResolvedRecord_marksRejected() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReconciliationResult manuallyResolved = new ReconciliationResult();
        manuallyResolved.setId(40L);
        manuallyResolved.setAccountingRecordId("ACC-040");
        manuallyResolved.setStatus(ReconciliationStatus.MANUALLY_RESOLVED);
        manuallyResolved.setRemarks("Manually resolved with RECEIPT record: RCT-040.");

        when(reconciliationResultRepository.findById(40L)).thenReturn(Optional.of(manuallyResolved));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.rejectManualResolution(40L, "Reference number does not match any known vendor", "supervisor1");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();
        assertEquals(ReconciliationStatus.REJECTED, saved.getStatus());
        assertTrue(saved.getRemarks().contains("REJECTED by supervisor1"));
        assertTrue(saved.getRemarks().contains("Reference number does not match any known vendor"));

        verify(auditService).log(
                eq(AuditActionType.MANUAL_MODIFICATION),
                eq("ReconciliationResult"),
                eq("ACC-040"),
                eq("supervisor1"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should refuse to reject a record that isn't manually resolved")
    void rejectManualResolution_notManuallyResolved_throwsRuntimeException() {

        ReconciliationResult exact = new ReconciliationResult();
        exact.setId(41L);
        exact.setStatus(ReconciliationStatus.EXACT);

        when(reconciliationResultRepository.findById(41L)).thenReturn(Optional.of(exact));

        assertThrows(RuntimeException.class, () ->
                reconciliationService.rejectManualResolution(41L, "n/a", "supervisor1"));

        verify(reconciliationResultRepository, never()).save(any());
    }

    // =========================================================================
    // sendToWorkflowReview()
    // =========================================================================

    @Test
    @DisplayName("Should submit a manually resolved record to workflow and mark it PENDING_APPROVAL")
    void sendToWorkflowReview_manuallyResolvedRecord_submitsToWorkflow() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReconciliationResult manuallyResolved = new ReconciliationResult();
        manuallyResolved.setId(50L);
        manuallyResolved.setAccountingRecordId("ACC-050");
        manuallyResolved.setStatus(ReconciliationStatus.MANUALLY_RESOLVED);

        WorkflowResponseDto workflowResponse = new WorkflowResponseDto();
        workflowResponse.setWorkflowId(777L);

        when(reconciliationResultRepository.findById(50L)).thenReturn(Optional.of(manuallyResolved));
        when(workflowService.createWorkflow(any(WorkflowRequestDto.class))).thenReturn(workflowResponse);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ReconciliationResult result = reconciliationService.sendToWorkflowReview(
                50L, "auditor1", "Please review this manual match");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<WorkflowRequestDto> requestCaptor = ArgumentCaptor.forClass(WorkflowRequestDto.class);
        verify(workflowService).createWorkflow(requestCaptor.capture());

        WorkflowRequestDto sentRequest = requestCaptor.getValue();
        assertEquals("RECONCILIATION", sentRequest.getSourceType());
        assertEquals("50", sentRequest.getSourceRecordId());
        assertEquals("auditor1", sentRequest.getCreatedBy());
        assertEquals("Please review this manual match", sentRequest.getComments());

        assertEquals(ReconciliationStatus.PENDING_APPROVAL, result.getStatus());
        assertEquals(777L, result.getWorkflowId());
        assertEquals("auditor1", result.getSubmittedBy());
        assertNotNull(result.getSubmittedToWorkflowAt());

        verify(reconciliationResultRepository).save(manuallyResolved);

        verify(auditService).log(
                eq(AuditActionType.WORKFLOW_STATE_CHANGED),
                eq("ReconciliationResult"),
                eq("ACC-050"),
                eq("auditor1"),
                contains("777")
        );
    }

    @Test
    @DisplayName("Should refuse to send a record to workflow unless it is manually resolved")
    void sendToWorkflowReview_notManuallyResolved_throwsRuntimeExceptionAndSkipsWorkflow() {

        ReconciliationResult alreadyPending = new ReconciliationResult();
        alreadyPending.setId(51L);
        alreadyPending.setStatus(ReconciliationStatus.PENDING_APPROVAL);

        when(reconciliationResultRepository.findById(51L)).thenReturn(Optional.of(alreadyPending));

        assertThrows(RuntimeException.class, () ->
                reconciliationService.sendToWorkflowReview(51L, "auditor1", "n/a"));

        verifyNoInteractions(workflowService);
        verify(reconciliationResultRepository, never()).save(any());
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