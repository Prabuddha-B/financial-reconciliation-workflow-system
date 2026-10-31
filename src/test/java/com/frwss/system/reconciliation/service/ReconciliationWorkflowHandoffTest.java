package com.frwss.system.reconciliation.service;

import com.frwss.system.audit.service.AuditService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the hand-off between the reconciliation module and the workflow module:
 * the details sent to the approver, and the approve/reject decisions coming back
 * for a record that is waiting in the workflow (PENDING_APPROVAL).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Reconciliation to Workflow Hand-off Unit Tests")
class ReconciliationWorkflowHandoffTest {

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
    // sendToWorkflowReview() - details passed to the workflow module
    // =========================================================================

    @Test
    @DisplayName("Should send the match details and justification to the workflow module")
    void sendToWorkflowReview_includesMatchDetailsAndJustification() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReconciliationResult result = manuallyResolvedResult(60L);

        WorkflowResponseDto workflowResponse = new WorkflowResponseDto();
        workflowResponse.setWorkflowId(900L);

        when(reconciliationResultRepository.findById(60L)).thenReturn(Optional.of(result));
        when(workflowService.createWorkflow(any(WorkflowRequestDto.class))).thenReturn(workflowResponse);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        reconciliationService.sendToWorkflowReview(60L, "entry", "Please check quickly");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<WorkflowRequestDto> captor = ArgumentCaptor.forClass(WorkflowRequestDto.class);
        verify(workflowService).createWorkflow(captor.capture());

        WorkflowRequestDto sent = captor.getValue();
        assertEquals("Please check quickly", sent.getComments());

        assertNotNull(sent.getDetails());
        assertTrue(sent.getDetails().contains("ACC-060"));
        assertTrue(sent.getDetails().contains("RCT-060"));
        assertTrue(sent.getDetails().contains("RECEIPT"));
        assertTrue(sent.getDetails().contains("90.0"));
        assertTrue(sent.getDetails().contains("Confirmed against the bank statement"));
    }

    @Test
    @DisplayName("Should truncate very long details so they fit the workflow column")
    void sendToWorkflowReview_veryLongRemarks_truncatesDetails() {

        ReconciliationResult result = manuallyResolvedResult(61L);
        result.setRemarks("x".repeat(5000));

        WorkflowResponseDto workflowResponse = new WorkflowResponseDto();
        workflowResponse.setWorkflowId(901L);

        when(reconciliationResultRepository.findById(61L)).thenReturn(Optional.of(result));
        when(workflowService.createWorkflow(any(WorkflowRequestDto.class))).thenReturn(workflowResponse);

        reconciliationService.sendToWorkflowReview(61L, "entry", null);

        ArgumentCaptor<WorkflowRequestDto> captor = ArgumentCaptor.forClass(WorkflowRequestDto.class);
        verify(workflowService).createWorkflow(captor.capture());

        assertTrue(captor.getValue().getDetails().length() <= 2000);
        assertTrue(captor.getValue().getDetails().endsWith("..."));
    }

    // =========================================================================
    // approveManualResolution() - decision coming back from the workflow
    // =========================================================================

    @Test
    @DisplayName("Should approve a record that is waiting in the workflow (PENDING_APPROVAL)")
    void approveManualResolution_pendingApproval_marksApproved() {

        ReconciliationResult pending = new ReconciliationResult();
        pending.setId(70L);
        pending.setAccountingRecordId("ACC-070");
        pending.setStatus(ReconciliationStatus.PENDING_APPROVAL);
        pending.setRemarks("Manually resolved with RECEIPT record: RCT-070.");

        when(reconciliationResultRepository.findById(70L)).thenReturn(Optional.of(pending));

        reconciliationService.approveManualResolution(70L, "auditor");

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        assertEquals(ReconciliationStatus.APPROVED, captor.getValue().getStatus());
        assertEquals("auditor", captor.getValue().getReconciledBy());
        assertTrue(captor.getValue().getRemarks().contains("APPROVED by auditor"));
    }

    @Test
    @DisplayName("Should still refuse to approve a record that was never manually resolved")
    void approveManualResolution_orphan_throwsRuntimeException() {

        ReconciliationResult orphan = new ReconciliationResult();
        orphan.setId(71L);
        orphan.setStatus(ReconciliationStatus.ORPHAN);

        when(reconciliationResultRepository.findById(71L)).thenReturn(Optional.of(orphan));

        assertThrows(RuntimeException.class, () ->
                reconciliationService.approveManualResolution(71L, "auditor"));

        verify(reconciliationResultRepository, never()).save(any());
    }

    // =========================================================================
    // rejectManualResolution() - decision coming back from the workflow
    // =========================================================================

    @Test
    @DisplayName("Should reject a record that is waiting in the workflow (PENDING_APPROVAL)")
    void rejectManualResolution_pendingApproval_marksRejected() {

        ReconciliationResult pending = new ReconciliationResult();
        pending.setId(80L);
        pending.setAccountingRecordId("ACC-080");
        pending.setStatus(ReconciliationStatus.PENDING_APPROVAL);
        pending.setRemarks("Manually resolved with RECEIPT record: RCT-080.");

        when(reconciliationResultRepository.findById(80L)).thenReturn(Optional.of(pending));

        reconciliationService.rejectManualResolution(80L, "Amounts do not agree", "auditor");

        ArgumentCaptor<ReconciliationResult> captor = ArgumentCaptor.forClass(ReconciliationResult.class);
        verify(reconciliationResultRepository).save(captor.capture());

        assertEquals(ReconciliationStatus.REJECTED, captor.getValue().getStatus());
        assertTrue(captor.getValue().getRemarks().contains("REJECTED by auditor"));
        assertTrue(captor.getValue().getRemarks().contains("Amounts do not agree"));
    }

    @Test
    @DisplayName("Should still refuse to reject a record that is already finalised")
    void rejectManualResolution_alreadyApproved_throwsRuntimeException() {

        ReconciliationResult approved = new ReconciliationResult();
        approved.setId(81L);
        approved.setStatus(ReconciliationStatus.APPROVED);

        when(reconciliationResultRepository.findById(81L)).thenReturn(Optional.of(approved));

        assertThrows(RuntimeException.class, () ->
                reconciliationService.rejectManualResolution(81L, "n/a", "auditor"));

        verify(reconciliationResultRepository, never()).save(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ReconciliationResult manuallyResolvedResult(long id) {
        ReconciliationResult result = new ReconciliationResult();
        result.setId(id);
        result.setAccountingRecordId("ACC-0" + id);
        result.setStatus(ReconciliationStatus.MANUALLY_RESOLVED);
        result.setMatchedSourceType(MatchedSourceType.RECEIPT);
        result.setMatchedRecordId("RCT-0" + id);
        result.setConfidenceScore(90.0);
        result.setReconciledBy("entry");
        result.setRemarks("Manually resolved with RECEIPT record: RCT-0" + id
                + ". Justification: Confirmed against the bank statement");
        return result;
    }
}