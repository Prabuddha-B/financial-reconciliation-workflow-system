package com.frwss.system.workflow.service;

import com.frwss.system.audit.service.AuditService;
import com.frwss.system.reconciliation.service.ReconciliationService;
import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.model.WorkflowRecord;
import com.frwss.system.workflow.model.WorkflowState;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Workflow Submission Details Unit Tests")
class WorkflowSubmissionDetailsTest {

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
        // Mockito injects mocked dependencies.
    }

    @Test
    @DisplayName("Should store the submitter's comments and details when a workflow is created")
    void createWorkflow_withCommentsAndDetails_storesBoth() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        WorkflowRequestDto request = new WorkflowRequestDto();
        request.setSourceType("RECONCILIATION");
        request.setSourceRecordId("60");
        request.setCreatedBy("entry");
        request.setComments("Please check quickly");
        request.setDetails("Accounting record: ACC-060 | Justification: Confirmed against the bank statement");

        when(workflowRecordRepository.save(any(WorkflowRecord.class)))
                .thenAnswer(invocation -> {
                    WorkflowRecord record = invocation.getArgument(0);
                    record.setWorkflowId(1L);
                    return record;
                });

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto response = workflowService.createWorkflow(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<WorkflowRecord> captor = ArgumentCaptor.forClass(WorkflowRecord.class);
        verify(workflowRecordRepository).save(captor.capture());

        assertEquals("Please check quickly", captor.getValue().getComments());
        assertEquals("Accounting record: ACC-060 | Justification: Confirmed against the bank statement",
                captor.getValue().getDetails());

        assertEquals("Please check quickly", response.getComments());
        assertEquals("Accounting record: ACC-060 | Justification: Confirmed against the bank statement",
                response.getDetails());
    }

    @Test
    @DisplayName("Should leave comments and details empty when none are supplied")
    void createWorkflow_withoutCommentsOrDetails_leavesThemNull() {

        WorkflowRequestDto request = new WorkflowRequestDto();
        request.setSourceType("PAYROLL");
        request.setSourceRecordId("PAY-001");
        request.setCreatedBy("entry");

        when(workflowRecordRepository.save(any(WorkflowRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowResponseDto response = workflowService.createWorkflow(request);

        assertNull(response.getComments());
        assertNull(response.getDetails());
    }

    @Test
    @DisplayName("Should return the stored comments and details when a workflow is retrieved")
    void getWorkflowById_returnsCommentsAndDetails() {

        WorkflowRecord record = new WorkflowRecord();
        record.setWorkflowId(5L);
        record.setSourceType("RECONCILIATION");
        record.setSourceRecordId("60");
        record.setCurrentState(WorkflowState.ENTERED);
        record.setCreatedBy("entry");
        record.setComments("Please check quickly");
        record.setDetails("Accounting record: ACC-060");

        when(workflowRecordRepository.findById(5L)).thenReturn(Optional.of(record));

        WorkflowResponseDto response = workflowService.getWorkflowById(5L);

        assertEquals("Please check quickly", response.getComments());
        assertEquals("Accounting record: ACC-060", response.getDetails());
        assertEquals("entry", response.getCreatedBy());
    }
}