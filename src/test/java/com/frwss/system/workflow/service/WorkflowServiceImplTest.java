package com.frwss.system.workflow.service;

import com.frwss.system.audit.service.AuditService;
import com.frwss.system.reconciliation.service.ReconciliationService;
import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.exception.InvalidWorkflowTransitionException;
import com.frwss.system.workflow.model.WorkflowRecord;
import com.frwss.system.workflow.model.WorkflowState;
import com.frwss.system.workflow.model.WorkflowTransitionLog;
import com.frwss.system.workflow.repository.WorkflowRecordRepository;
import com.frwss.system.workflow.repository.WorkflowTransitionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.exception.WorkflowNotFoundException;

import java.time.LocalDateTime;
import java.util.*;


import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.workflow.model.WorkflowTransitionLog;

@ExtendWith(MockitoExtension.class)
@DisplayName("Workflow Service Unit Tests")
class WorkflowServiceImplTest {

    @Mock
    private WorkflowRecordRepository workflowRecordRepository;

    @Mock
    private WorkflowTransitionLogRepository workflowTransitionLogRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ReconciliationService reconciliationService;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    @BeforeEach
    void setUp() {
        // Mockito automatically injects all mocked dependencies.
    }

    // =========================================================================
    // createWorkflow()
    // =========================================================================

    @Test
    @DisplayName("Should create a workflow successfully with default ENTERED state")
    void createWorkflow_validRequest_returnsSuccessfulResponse() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare a valid workflow creation request.
        // ---------------------------------------------------------------------

        WorkflowRequestDto request = new WorkflowRequestDto();
        request.setSourceType("RECONCILIATION");
        request.setSourceRecordId("1001");
        request.setCreatedBy("john.doe");

        WorkflowRecord savedRecord = new WorkflowRecord();
        savedRecord.setWorkflowId(1L);
        savedRecord.setSourceType("RECONCILIATION");
        savedRecord.setSourceRecordId("1001");
        savedRecord.setCreatedBy("john.doe");
        savedRecord.setCurrentState(WorkflowState.ENTERED);

        when(workflowRecordRepository.save(any(WorkflowRecord.class)))
                .thenReturn(savedRecord);

        // ---------------------------------------------------------------------
        // Act: Invoke the service method under test.
        // ---------------------------------------------------------------------

        WorkflowResponseDto response = workflowService.createWorkflow(request);

        // ---------------------------------------------------------------------
        // Assert: Verify repository interaction and returned response.
        // ---------------------------------------------------------------------

        verify(workflowRecordRepository, times(1))
                .save(any(WorkflowRecord.class));

        ArgumentCaptor<WorkflowRecord> captor =
                ArgumentCaptor.forClass(WorkflowRecord.class);

        verify(workflowRecordRepository).save(captor.capture());

        WorkflowRecord capturedRecord = captor.getValue();

        assertEquals("RECONCILIATION", capturedRecord.getSourceType());
        assertEquals("1001", capturedRecord.getSourceRecordId());
        assertEquals("john.doe", capturedRecord.getCreatedBy());
        assertEquals(WorkflowState.ENTERED, capturedRecord.getCurrentState());

        assertNotNull(capturedRecord.getCreatedAt());
        assertNotNull(capturedRecord.getUpdatedAt());

        assertNotNull(response);
        assertEquals(1L, response.getWorkflowId());
        assertEquals("RECONCILIATION", response.getSourceType());
        assertEquals("1001", response.getSourceRecordId());
        assertEquals(WorkflowState.ENTERED, response.getCurrentState());
        assertEquals("Workflow Created Successfully", response.getMessage());
    }


    // =========================================================================
    // updateWorkflowState()
    // =========================================================================

    @Test
    @DisplayName("Should throw WorkflowNotFoundException when workflow does not exist")
    void updateWorkflowState_workflowNotFound_throwsWorkflowNotFoundException() {

        // ---------------------------------------------------------------------
        // Arrange: Mock repository to return no workflow.
        // ---------------------------------------------------------------------

        Long workflowId = 999L;

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("reviewer");
        request.setJustification("Verified after review");
        request.setNewState(WorkflowState.REVIEWED);

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act & Assert: Verify exception is thrown.
        // ---------------------------------------------------------------------

        WorkflowNotFoundException exception =
                assertThrows(
                        WorkflowNotFoundException.class,
                        () -> workflowService.updateWorkflowState(workflowId, request)
                );

        assertEquals("Workflow not found", exception.getMessage());

        verify(workflowRecordRepository).findById(workflowId);

        verifyNoInteractions(
                workflowTransitionLogRepository,
                reconciliationService,
                auditService
        );
    }

    @Test
    @DisplayName("Should throw InvalidWorkflowTransitionException for an invalid workflow transition")
    void updateWorkflowState_invalidTransition_throwsInvalidWorkflowTransitionException() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare an existing workflow with an invalid transition.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflowRecord = new WorkflowRecord();
        workflowRecord.setWorkflowId(workflowId);
        workflowRecord.setCreatedBy("creator");
        workflowRecord.setCurrentState(WorkflowState.ENTERED);

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("reviewer");
        request.setJustification("Attempting invalid transition");
        request.setNewState(WorkflowState.VERIFIED); // ENTERED -> VERIFIED is invalid

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowRecord));

        // ---------------------------------------------------------------------
        // Act & Assert: Verify invalid transition is rejected.
        // ---------------------------------------------------------------------

        InvalidWorkflowTransitionException exception =
                assertThrows(
                        InvalidWorkflowTransitionException.class,
                        () -> workflowService.updateWorkflowState(workflowId, request)
                );

        assertEquals(
                "Segregation of Duties Violation: Users cannot verify records they previously reviewed.",
                exception.getMessage()
        );

        verify(workflowRecordRepository).findById(workflowId);

        verify(workflowRecordRepository, never()).save(any());
        verifyNoInteractions(
                workflowTransitionLogRepository,
                reconciliationService,
                auditService
        );
    }

    @Test
    @DisplayName("Should throw RuntimeException when justification is missing")
    void updateWorkflowState_missingJustification_throwsRuntimeException() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare an existing workflow with no justification.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflowRecord = new WorkflowRecord();
        workflowRecord.setWorkflowId(workflowId);
        workflowRecord.setCreatedBy("creator");
        workflowRecord.setCurrentState(WorkflowState.ENTERED);

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("reviewer");
        request.setNewState(WorkflowState.REVIEWED);
        request.setJustification("");   // Missing justification

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowRecord));

        // ---------------------------------------------------------------------
        // Act & Assert: Verify missing justification is rejected.
        // ---------------------------------------------------------------------

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> workflowService.updateWorkflowState(workflowId, request)
                );

        assertEquals("Justification is required", exception.getMessage());

        verify(workflowRecordRepository).findById(workflowId);

        verify(workflowRecordRepository, never()).save(any());

        verifyNoInteractions(
                workflowTransitionLogRepository,
                reconciliationService,
                auditService
        );
    }

    @Test
    @DisplayName("Should prevent workflow creator from reviewing their own workflow")
    void updateWorkflowState_creatorCannotApproveOwnWorkflow_throwsRuntimeException() {

        // ---------------------------------------------------------------------
        // Arrange: Creator attempts to review their own workflow.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflowRecord = new WorkflowRecord();
        workflowRecord.setWorkflowId(workflowId);
        workflowRecord.setCreatedBy("john.doe");
        workflowRecord.setCurrentState(WorkflowState.ENTERED);

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("john.doe");   // Same user as creator
        request.setNewState(WorkflowState.REVIEWED);
        request.setJustification("Self approval attempt");

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowRecord));

        // ---------------------------------------------------------------------
        // Act & Assert: Verify creator cannot review their own workflow.
        // ---------------------------------------------------------------------

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> workflowService.updateWorkflowState(workflowId, request)
                );

        assertEquals(
                "Creator cannot review or approve their own workflow",
                exception.getMessage()
        );

        verify(workflowRecordRepository).findById(workflowId);

        verify(workflowRecordRepository, never()).save(any());

        verifyNoInteractions(
                workflowTransitionLogRepository,
                reconciliationService,
                auditService
        );
    }

    @Test
    @DisplayName("Should prevent the same user from performing consecutive workflow transitions")
    void updateWorkflowState_sameUserPerformsConsecutiveTransition_throwsRuntimeException() {

        // ---------------------------------------------------------------------
        // Arrange: Same reviewer attempts consecutive workflow transitions.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflowRecord = new WorkflowRecord();
        workflowRecord.setWorkflowId(workflowId);
        workflowRecord.setCreatedBy("creator");
        workflowRecord.setCurrentState(WorkflowState.ENTERED);

        WorkflowTransitionLog latestTransition = new WorkflowTransitionLog();
        latestTransition.setPerformedBy("reviewer");

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("reviewer");
        request.setNewState(WorkflowState.REVIEWED);
        request.setJustification("Second consecutive review");

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowRecord));

        when(workflowTransitionLogRepository
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId))
                .thenReturn(latestTransition);

        // ---------------------------------------------------------------------
        // Act & Assert: Verify consecutive transitions are rejected.
        // ---------------------------------------------------------------------

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> workflowService.updateWorkflowState(workflowId, request)
                );

        assertEquals(
                "Same user cannot perform consecutive workflow transitions",
                exception.getMessage()
        );

        verify(workflowRecordRepository).findById(workflowId);

        verify(workflowTransitionLogRepository)
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId);

        verify(workflowRecordRepository, never()).save(any());

        verify(auditService, never()).log(any(), any(), any(), any(), any());

        verifyNoInteractions(reconciliationService);
    }

    @Test
    @DisplayName("Should successfully update workflow state")
    void updateWorkflowState_validRequest_updatesWorkflowSuccessfully() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare a valid workflow update request.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflowRecord = new WorkflowRecord();
        workflowRecord.setWorkflowId(workflowId);
        workflowRecord.setSourceType("WORKFLOW");
        workflowRecord.setSourceRecordId("1001");
        workflowRecord.setCreatedBy("creator");
        workflowRecord.setCurrentState(WorkflowState.ENTERED);

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("reviewer");
        request.setNewState(WorkflowState.REVIEWED);
        request.setJustification("Reviewed successfully");

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowRecord));

        when(workflowTransitionLogRepository
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId))
                .thenReturn(null);

        when(workflowRecordRepository.save(any(WorkflowRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---------------------------------------------------------------------
        // Act: Execute the workflow update.
        // ---------------------------------------------------------------------

        WorkflowResponseDto response =
                workflowService.updateWorkflowState(workflowId, request);

        // ---------------------------------------------------------------------
        // Assert: Verify workflow record was updated correctly.
        // ---------------------------------------------------------------------

        ArgumentCaptor<WorkflowRecord> workflowCaptor =
                ArgumentCaptor.forClass(WorkflowRecord.class);

        verify(workflowRecordRepository)
                .save(workflowCaptor.capture());

        WorkflowRecord savedWorkflow = workflowCaptor.getValue();

        assertEquals(
                WorkflowState.REVIEWED,
                savedWorkflow.getCurrentState()
        );

        assertNotNull(savedWorkflow.getUpdatedAt());

        // ---------------------------------------------------------------------
        // Assert: Verify transition log was created.
        // ---------------------------------------------------------------------

        ArgumentCaptor<WorkflowTransitionLog> logCaptor =
                ArgumentCaptor.forClass(WorkflowTransitionLog.class);

        verify(workflowTransitionLogRepository)
                .save(logCaptor.capture());

        WorkflowTransitionLog savedLog = logCaptor.getValue();

        assertEquals("ENTERED", savedLog.getFromState());
        assertEquals("REVIEWED", savedLog.getToState());
        assertEquals("reviewer", savedLog.getPerformedBy());
        assertEquals("Reviewed successfully", savedLog.getJustification());
        assertNotNull(savedLog.getTransitionTime());

        // ---------------------------------------------------------------------
        // Assert: Verify audit log was written.
        // ---------------------------------------------------------------------

        verify(auditService).log(
                AuditActionType.WORKFLOW_STATE_CHANGED,
                "WorkflowRecord",
                workflowId.toString(),
                "reviewer",
                "Workflow moved from ENTERED to REVIEWED"
        );

        // ---------------------------------------------------------------------
        // Assert: Verify returned response.
        // ---------------------------------------------------------------------

        assertNotNull(response);

        assertEquals(workflowId, response.getWorkflowId());
        assertEquals(WorkflowState.REVIEWED, response.getCurrentState());
        assertEquals("Workflow State Updated Successfully", response.getMessage());
        assertEquals("creator", response.getCreatedBy());

        // ---------------------------------------------------------------------
        // Assert: Reconciliation service should NOT be called.
        // ---------------------------------------------------------------------

        verifyNoInteractions(reconciliationService);
    }

    @Test
    @DisplayName("Should approve reconciliation when workflow reaches VERIFIED state")
    void updateWorkflowState_verifiedReconciliation_callsApproveManualResolution() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare a reconciliation workflow for approval.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflowRecord = new WorkflowRecord();
        workflowRecord.setWorkflowId(workflowId);
        workflowRecord.setSourceType("RECONCILIATION");
        workflowRecord.setSourceRecordId("1001");
        workflowRecord.setCreatedBy("creator");
        workflowRecord.setCurrentState(WorkflowState.REVIEWED);

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("approver");
        request.setNewState(WorkflowState.VERIFIED);
        request.setJustification("Verified successfully");

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowRecord));

        when(workflowTransitionLogRepository
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId))
                .thenReturn(null);

        when(workflowRecordRepository.save(any(WorkflowRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        workflowService.updateWorkflowState(workflowId, request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        verify(reconciliationService)
                .approveManualResolution(1001L, "approver");

        verify(auditService).log(
                eq(AuditActionType.WORKFLOW_STATE_CHANGED),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Should reject reconciliation when workflow reaches REJECTED state")
    void updateWorkflowState_rejectedReconciliation_callsRejectManualResolution() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare a reconciliation workflow for rejection.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflowRecord = new WorkflowRecord();
        workflowRecord.setWorkflowId(workflowId);
        workflowRecord.setSourceType("RECONCILIATION");
        workflowRecord.setSourceRecordId("1001");
        workflowRecord.setCreatedBy("creator");
        workflowRecord.setCurrentState(WorkflowState.REVIEWED);

        WorkflowStateUpdateRequestDto request = new WorkflowStateUpdateRequestDto();
        request.setPerformedBy("approver");
        request.setNewState(WorkflowState.REJECTED);
        request.setJustification("Rejected after review");

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflowRecord));

        when(workflowTransitionLogRepository
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId))
                .thenReturn(null);

        when(workflowRecordRepository.save(any(WorkflowRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        workflowService.updateWorkflowState(workflowId, request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        verify(reconciliationService)
                .rejectManualResolution(
                        1001L,
                        "Rejected after review",
                        "approver"
                );

        verify(auditService).log(
                eq(AuditActionType.WORKFLOW_STATE_CHANGED),
                any(),
                any(),
                any(),
                any()
        );
    }

    // =========================================================================
    // getWorkflowHistory()
    // =========================================================================

    @Test
    @DisplayName("Should return workflow history successfully")
    void getWorkflowHistory_existingHistory_returnsHistoryList() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare workflow transition history.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowTransitionLog log = new WorkflowTransitionLog();
        log.setFromState("ENTERED");
        log.setToState("REVIEWED");
        log.setPerformedBy("reviewer");
        log.setJustification("Reviewed successfully");
        log.setTransitionTime(LocalDateTime.now());

        when(workflowTransitionLogRepository.findByWorkflowId(workflowId))
                .thenReturn(List.of(log));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowHistoryResponseDto> history =
                workflowService.getWorkflowHistory(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(history);
        assertEquals(1, history.size());

        verify(workflowTransitionLogRepository)
                .findByWorkflowId(workflowId);
    }

    @Test
    @DisplayName("Should return an empty history list when no workflow history exists")
    void getWorkflowHistory_noHistory_returnsEmptyList() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 99L;

        when(workflowTransitionLogRepository.findByWorkflowId(workflowId))
                .thenReturn(Collections.emptyList());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowHistoryResponseDto> history =
                workflowService.getWorkflowHistory(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(history.isEmpty());

        verify(workflowTransitionLogRepository)
                .findByWorkflowId(workflowId);
    }

    @Test
    @DisplayName("Should correctly map workflow history entity to response DTO")
    void getWorkflowHistory_mapsEntityToDtoCorrectly() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        LocalDateTime transitionTime = LocalDateTime.now();

        WorkflowTransitionLog log = new WorkflowTransitionLog();
        log.setFromState("REVIEWED");
        log.setToState("VERIFIED");
        log.setPerformedBy("approver");
        log.setJustification("Approved");
        log.setTransitionTime(transitionTime);

        when(workflowTransitionLogRepository.findByWorkflowId(workflowId))
                .thenReturn(List.of(log));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowHistoryResponseDto dto =
                workflowService.getWorkflowHistory(workflowId).get(0);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("REVIEWED", dto.getFromState());
        assertEquals("VERIFIED", dto.getToState());
        assertEquals("approver", dto.getPerformedBy());
        assertEquals("Approved", dto.getJustification());
        assertEquals(transitionTime, dto.getTransitionTime());
    }

    // =========================================================================
    // getWorkflowById()
    // =========================================================================

    @Test
    @DisplayName("Should return workflow when workflow ID exists")
    void getWorkflowById_existingWorkflow_returnsWorkflow() {

        // ---------------------------------------------------------------------
        // Arrange: Prepare an existing workflow.
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflow = new WorkflowRecord();
        workflow.setWorkflowId(workflowId);
        workflow.setSourceType("RECONCILIATION");
        workflow.setSourceRecordId("1001");
        workflow.setCurrentState(WorkflowState.REVIEWED);
        workflow.setCreatedBy("creator");

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflow));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto response =
                workflowService.getWorkflowById(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(response);
        assertEquals(workflowId, response.getWorkflowId());

        verify(workflowRecordRepository)
                .findById(workflowId);
    }

    @Test
    @DisplayName("Should throw WorkflowNotFoundException when workflow ID does not exist")
    void getWorkflowById_workflowNotFound_throwsWorkflowNotFoundException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 999L;

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        WorkflowNotFoundException exception =
                assertThrows(
                        WorkflowNotFoundException.class,
                        () -> workflowService.getWorkflowById(workflowId)
                );

        assertEquals("Workflow not found", exception.getMessage());

        verify(workflowRecordRepository)
                .findById(workflowId);
    }

    @Test
    @DisplayName("Should correctly map workflow entity to response DTO")
    void getWorkflowById_mapsEntityToDtoCorrectly() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowRecord workflow = new WorkflowRecord();
        workflow.setWorkflowId(workflowId);
        workflow.setSourceType("INGESTION");
        workflow.setSourceRecordId("ABC-100");
        workflow.setCurrentState(WorkflowState.VERIFIED);
        workflow.setCreatedBy("john");

        when(workflowRecordRepository.findById(workflowId))
                .thenReturn(Optional.of(workflow));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto response =
                workflowService.getWorkflowById(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(workflowId, response.getWorkflowId());
        assertEquals("INGESTION", response.getSourceType());
        assertEquals("ABC-100", response.getSourceRecordId());
        assertEquals(WorkflowState.VERIFIED, response.getCurrentState());
        assertEquals("john", response.getCreatedBy());
    }

    // =========================================================================
    // getAllWorkflows()
    // =========================================================================

    @Test
    @DisplayName("Should return all workflows successfully")
    void getAllWorkflows_returnsWorkflowList() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        WorkflowRecord workflow = new WorkflowRecord();
        workflow.setWorkflowId(1L);
        workflow.setSourceType("INGESTION");
        workflow.setSourceRecordId("1001");
        workflow.setCurrentState(WorkflowState.ENTERED);

        when(workflowRecordRepository.findAll())
                .thenReturn(new ArrayList<>(List.of(workflow)));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowResponseDto> response =
                workflowService.getAllWorkflows();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, response.size());

        verify(workflowRecordRepository)
                .findAll();
    }

    @Test
    @DisplayName("Should return empty workflow list when repository has no records")
    void getAllWorkflows_emptyRepository_returnsEmptyList() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(workflowRecordRepository.findAll())
                .thenReturn(Collections.emptyList());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowResponseDto> response =
                workflowService.getAllWorkflows();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(response.isEmpty());

        verify(workflowRecordRepository)
                .findAll();
    }


    // =========================================================================
    // getLatestWorkflowActivity()
    // =========================================================================

    @Test
    @DisplayName("Should return latest workflow activity successfully")
    void getLatestWorkflowActivity_existingActivity_returnsLatestActivity() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        LocalDateTime transitionTime = LocalDateTime.now();

        WorkflowTransitionLog log = new WorkflowTransitionLog();
        log.setFromState("REVIEWED");
        log.setToState("VERIFIED");
        log.setPerformedBy("approver");
        log.setJustification("Approved");
        log.setTransitionTime(transitionTime);

        when(workflowTransitionLogRepository
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId))
                .thenReturn(log);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowHistoryResponseDto response =
                workflowService.getLatestWorkflowActivity(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(response);

        assertEquals("REVIEWED", response.getFromState());
        assertEquals("VERIFIED", response.getToState());

        verify(workflowTransitionLogRepository)
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId);
    }

    @Test
    @DisplayName("Should return null when workflow has no activity")
    void getLatestWorkflowActivity_noActivity_returnsNull() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 99L;

        when(workflowTransitionLogRepository
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId))
                .thenReturn(null);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowHistoryResponseDto response =
                workflowService.getLatestWorkflowActivity(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNull(response);

        verify(workflowTransitionLogRepository)
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId);
    }

    @Test
    @DisplayName("Should correctly map latest workflow activity to response DTO")
    void getLatestWorkflowActivity_mapsEntityToDtoCorrectly() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        LocalDateTime transitionTime = LocalDateTime.now();

        WorkflowTransitionLog log = new WorkflowTransitionLog();
        log.setFromState("ENTERED");
        log.setToState("REVIEWED");
        log.setPerformedBy("reviewer");
        log.setJustification("Initial review");
        log.setTransitionTime(transitionTime);

        when(workflowTransitionLogRepository
                .findTopByWorkflowIdOrderByTransitionTimeDesc(workflowId))
                .thenReturn(log);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowHistoryResponseDto response =
                workflowService.getLatestWorkflowActivity(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("ENTERED", response.getFromState());
        assertEquals("REVIEWED", response.getToState());
        assertEquals("reviewer", response.getPerformedBy());
        assertEquals("Initial review", response.getJustification());
        assertEquals(transitionTime, response.getTransitionTime());
    }

}