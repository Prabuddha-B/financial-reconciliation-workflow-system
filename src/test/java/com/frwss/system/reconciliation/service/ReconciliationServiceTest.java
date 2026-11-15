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
import com.frwss.system.reconciliation.dto.CandidateMatchView;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    // resolveOrphanWithManualEntry()
    // =========================================================================

    @Test
    @DisplayName("Should resolve an orphan with a typed-in ID that exists, using the fixed low confidence")
    void resolveOrphanWithManualEntry_validId_resolvesWithFixedConfidence() {

        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(30L);
        orphan.setAccountingRecordId("ACC-030");
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findById(30L)).thenReturn(Optional.of(orphan));
        when(receiptRepository.existsById("RCT-030")).thenReturn(true);

        reconciliationService.resolveOrphanWithManualEntry(
                30L, "RECEIPT", "  RCT-030  ", "Matches the signed paper receipt", "auditor1");

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        ReconciliationResult saved = captor.getValue();
        assertEquals(ReconciliationStatus.MANUALLY_RESOLVED, saved.getStatus());
        assertEquals(MatchedSourceType.RECEIPT, saved.getMatchedSourceType());
        assertEquals("RCT-030", saved.getMatchedRecordId());
        assertEquals(ReconciliationService.MANUAL_ENTRY_CONFIDENCE, saved.getConfidenceScore());
        assertTrue(saved.getRemarks().contains("Matches the signed paper receipt"));

        verify(auditService).log(
                eq(AuditActionType.MANUAL_MODIFICATION),
                eq("ReconciliationResult"),
                eq("ACC-030"),
                eq("auditor1"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should reject an ID that does not exist in the chosen source")
    void resolveOrphanWithManualEntry_unknownId_throwsAndSavesNothing() {

        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(31L);
        orphan.setAccountingRecordId("ACC-031");
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findById(31L)).thenReturn(Optional.of(orphan));
        when(receiptRepository.existsById("RCT-NOPE")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                reconciliationService.resolveOrphanWithManualEntry(
                        31L, "RECEIPT", "RCT-NOPE", "n/a", "auditor1"));

        verify(reconciliationResultRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Should reject an ID that exists in a different source than the one chosen")
    void resolveOrphanWithManualEntry_idInWrongSource_throwsAndSavesNothing() {

        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(32L);
        orphan.setAccountingRecordId("ACC-032");
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findById(32L)).thenReturn(Optional.of(orphan));
        // The ID belongs to a receipt, but the reviewer chose PAYROLL
        when(payrollRepository.existsById("RCT-032")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                reconciliationService.resolveOrphanWithManualEntry(
                        32L, "PAYROLL", "RCT-032", "n/a", "auditor1"));

        verifyNoInteractions(receiptRepository);
        verify(reconciliationResultRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Should reject a source that is not PAYROLL, RECEIPT or STOCK_PURCHASE")
    void resolveOrphanWithManualEntry_invalidSource_throwsAndSavesNothing() {

        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(33L);
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findById(33L)).thenReturn(Optional.of(orphan));

        assertThrows(RuntimeException.class, () ->
                reconciliationService.resolveOrphanWithManualEntry(
                        33L, "NONE", "X-1", "n/a", "auditor1"));

        verifyNoInteractions(payrollRepository, receiptRepository, stockPurchaseRepository);
        verify(reconciliationResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should refuse when the result is no longer an orphan")
    void resolveOrphanWithManualEntry_notAnOrphan_throwsAndSavesNothing() {

        ReconciliationResult alreadyExact = new ReconciliationResult();
        alreadyExact.setId(34L);
        alreadyExact.setStatus(ReconciliationStatus.EXACT);

        when(reconciliationResultRepository.findById(34L)).thenReturn(Optional.of(alreadyExact));

        assertThrows(RuntimeException.class, () ->
                reconciliationService.resolveOrphanWithManualEntry(
                        34L, "RECEIPT", "RCT-034", "n/a", "auditor1"));

        verifyNoInteractions(receiptRepository);
        verify(reconciliationResultRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Should require a justification")
    void resolveOrphanWithManualEntry_blankJustification_throwsBeforeAnyLookup() {

        assertThrows(RuntimeException.class, () ->
                reconciliationService.resolveOrphanWithManualEntry(
                        35L, "RECEIPT", "RCT-035", "   ", "auditor1"));

        verifyNoInteractions(reconciliationResultRepository, receiptRepository, auditService);
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
    // getChildRecordsForBulkResults()
    // =========================================================================

    @Test
    @DisplayName("Should return the stored child records for each bulk result, in stored order")
    void getChildRecordsForBulkResults_returnsChildrenInStoredOrder() {

        BulkReconciliation bulk = new BulkReconciliation();
        bulk.setId(1L);
        bulk.setChildRecordIds("ACC-C3,ACC-C1,ACC-C2");

        AccountingRecord c1 = accountingRecord("ACC-C1", "PO-2026-A01-1", new BigDecimal("400.00"), LocalDate.of(2026, 7, 1));
        AccountingRecord c2 = accountingRecord("ACC-C2", "PO-2026-A01-2", new BigDecimal("350.00"), LocalDate.of(2026, 7, 1));
        AccountingRecord c3 = accountingRecord("ACC-C3", "PO-2026-A01-3", new BigDecimal("250.00"), LocalDate.of(2026, 7, 1));

        when(accountingRecordRepository.findAllById(any())).thenReturn(List.of(c1, c2, c3));

        Map<Long, List<AccountingRecord>> result =
                reconciliationService.getChildRecordsForBulkResults(List.of(bulk));

        assertEquals(3, result.get(1L).size());
        assertEquals("ACC-C3", result.get(1L).get(0).getRecordId());
        assertEquals("ACC-C1", result.get(1L).get(1).getRecordId());
        assertEquals("ACC-C2", result.get(1L).get(2).getRecordId());
    }

    @Test
    @DisplayName("Should give a bulk result an empty child list when it has no stored children")
    void getChildRecordsForBulkResults_noStoredChildren_returnsEmptyListWithoutQuery() {

        BulkReconciliation noChildren = new BulkReconciliation();
        noChildren.setId(2L);
        noChildren.setChildRecordIds(null);

        BulkReconciliation blankChildren = new BulkReconciliation();
        blankChildren.setId(3L);
        blankChildren.setChildRecordIds("  ");

        Map<Long, List<AccountingRecord>> result =
                reconciliationService.getChildRecordsForBulkResults(List.of(noChildren, blankChildren));

        assertTrue(result.get(2L).isEmpty());
        assertTrue(result.get(3L).isEmpty());
        verifyNoInteractions(accountingRecordRepository);
    }

    @Test
    @DisplayName("Should skip child records that no longer exist")
    void getChildRecordsForBulkResults_missingChild_isSkipped() {

        BulkReconciliation bulk = new BulkReconciliation();
        bulk.setId(4L);
        bulk.setChildRecordIds("ACC-C1, ACC-GONE");

        AccountingRecord c1 = accountingRecord("ACC-C1", "PO-2026-A01-1", new BigDecimal("400.00"), LocalDate.of(2026, 7, 1));

        when(accountingRecordRepository.findAllById(any())).thenReturn(List.of(c1));

        Map<Long, List<AccountingRecord>> result =
                reconciliationService.getChildRecordsForBulkResults(List.of(bulk));

        assertEquals(1, result.get(4L).size());
        assertEquals("ACC-C1", result.get(4L).get(0).getRecordId());
    }

    // =========================================================================
    // Experimental: fuzzy reference matching and amount anomaly flags
    // =========================================================================

    private void stubOrphanWithOneReceiptCandidate() {
        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(40L);
        orphan.setAccountingRecordId("ACC-040");
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        AccountingRecord accounting = accountingRecord(
                "ACC-040", "RC-2026-001", new BigDecimal("1000.00"), LocalDate.of(2026, 10, 1));

        Receipt receipt = new Receipt();
        receipt.setReceiptId("RCT-040");
        receipt.setReferenceNo("RC-2026-002");
        receipt.setAmount(new BigDecimal("1000.00"));
        receipt.setReceiptDate(LocalDate.of(2026, 10, 1));

        when(reconciliationResultRepository.findById(40L)).thenReturn(Optional.of(orphan));
        when(accountingRecordRepository.findById("ACC-040")).thenReturn(Optional.of(accounting));
        when(receiptRepository.findByAmountAndReceiptDateBetween(any(), any(), any()))
                .thenReturn(List.of(receipt));
    }

    @Test
    @DisplayName("Fuzzy off: candidate score and similarity behave as before")
    void candidates_fuzzyOff_unchanged() {
        stubOrphanWithOneReceiptCandidate();

        List<CandidateMatchView> candidates = reconciliationService.getCandidateMatchesForOrphan(40L);

        assertEquals(1, candidates.size());
        // 60 base + 0 (references are not equal) + 15 (same day)
        assertEquals(75.0, candidates.get(0).getSuggestedConfidence(), 0.001);
        assertNull(candidates.get(0).getReferenceSimilarity());
    }

    @Test
    @DisplayName("Fuzzy on: a similar reference earns a scaled bonus and shows its similarity")
    void candidates_fuzzyOn_scalesReferenceBonus() {
        ReflectionTestUtils.setField(reconciliationService, "fuzzyEnabled", true);
        stubOrphanWithOneReceiptCandidate();

        List<CandidateMatchView> candidates = reconciliationService.getCandidateMatchesForOrphan(40L);

        assertEquals(1, candidates.size());
        assertEquals(88.9, candidates.get(0).getReferenceSimilarity(), 0.001);
        // 60 base + 17.78 (88.9% of 20) + 15 (same day) = 92.78, shown to one decimal
        assertEquals(92.8, candidates.get(0).getSuggestedConfidence(), 0.001);
    }

    @Test
    @DisplayName("Anomaly flags: nothing is read or flagged while the feature is off")
    void anomalyFlags_off_returnsEmptyWithoutReading() {
        ReconciliationResult result = new ReconciliationResult();
        result.setId(50L);
        result.setAccountingRecordId("ACC-050");

        Map<Long, String> flags = reconciliationService.getAmountAnomalyFlags(List.of(result));

        assertTrue(flags.isEmpty());
        verifyNoInteractions(accountingRecordRepository);
    }

    @Test
    @DisplayName("Anomaly flags: flags only the result whose amount is unusual for its module")
    void anomalyFlags_on_flagsOnlyTheUnusualAmount() {
        ReflectionTestUtils.setField(reconciliationService, "anomalyEnabled", true);

        ReconciliationResult unusual = new ReconciliationResult();
        unusual.setId(50L);
        unusual.setAccountingRecordId("ACC-050");

        ReconciliationResult typical = new ReconciliationResult();
        typical.setId(51L);
        typical.setAccountingRecordId("ACC-051");

        AccountingRecord unusualRecord = payrollRecord("ACC-050", "1000");
        AccountingRecord typicalRecord = payrollRecord("ACC-051", "100");

        // the whole PAYROLL module: eight typical amounts plus the two records above
        List<AccountingRecord> everything = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            everything.add(payrollRecord("ACC-1" + i, "100"));
        }
        everything.add(typicalRecord);
        everything.add(unusualRecord);

        when(accountingRecordRepository.findAllById(any())).thenReturn(List.of(unusualRecord, typicalRecord));
        when(accountingRecordRepository.findAll()).thenReturn(everything);

        Map<Long, String> flags = reconciliationService.getAmountAnomalyFlags(List.of(unusual, typical));

        assertEquals(1, flags.size());
        assertTrue(flags.get(50L).startsWith("Unusual amount: 1,000.00"));
        assertFalse(flags.containsKey(51L));
        // read-only: nothing is ever saved
        verify(accountingRecordRepository, never()).save(any());
        verifyNoInteractions(reconciliationResultRepository);
    }

    private AccountingRecord payrollRecord(String recordId, String amount) {
        AccountingRecord record = accountingRecord(
                recordId, "REF-" + recordId, new BigDecimal(amount), LocalDate.of(2026, 10, 1));
        record.setModule("PAYROLL");
        return record;
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