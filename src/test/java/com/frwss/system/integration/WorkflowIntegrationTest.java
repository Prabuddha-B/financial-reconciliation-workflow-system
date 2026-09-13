package com.frwss.system.integration;

import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.model.WorkflowRecord;
import com.frwss.system.workflow.model.WorkflowState;
import com.frwss.system.workflow.model.WorkflowTransitionLog;
import com.frwss.system.workflow.repository.WorkflowRecordRepository;
import com.frwss.system.workflow.repository.WorkflowTransitionLogRepository;
import com.frwss.system.workflow.service.WorkflowServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Workflow Integration Tests")
class WorkflowIntegrationTest {

    @Autowired
    private WorkflowServiceImpl workflowService;

    @Autowired
    private WorkflowRecordRepository workflowRecordRepository;

    @Autowired
    private WorkflowTransitionLogRepository workflowTransitionLogRepository;

    // =========================================================================
    // Workflow lifecycle
    // =========================================================================

    @Test
    @DisplayName("Should create, transition, persist, and retrieve workflow history")
    void workflowLifecycle_createAndReview_persistsWorkflowAndHistory() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        WorkflowRequestDto createRequest = new WorkflowRequestDto();
        createRequest.setSourceType("WORKFLOW");
        createRequest.setSourceRecordId("INTEGRATION-001");
        createRequest.setCreatedBy("creator");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto createdResponse = workflowService.createWorkflow(createRequest);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(
                createdResponse.getWorkflowId()
        );

        assertEquals(
                WorkflowState.ENTERED,
                createdResponse.getCurrentState()
        );

        Optional<WorkflowRecord> storedBeforeTransition = workflowRecordRepository.findById(
                createdResponse.getWorkflowId()
        );

        assertTruePresent(
                storedBeforeTransition
        );

        assertEquals(
                WorkflowState.ENTERED,
                storedBeforeTransition.get().getCurrentState()
        );

        WorkflowStateUpdateRequestDto updateRequest = new WorkflowStateUpdateRequestDto();
        updateRequest.setNewState(WorkflowState.REVIEWED);
        updateRequest.setPerformedBy("reviewer");
        updateRequest.setJustification("Integration review completed");

        WorkflowResponseDto updatedResponse = workflowService.updateWorkflowState(
                createdResponse.getWorkflowId(),
                updateRequest
        );

        Optional<WorkflowRecord> storedAfterTransition = workflowRecordRepository.findById(
                createdResponse.getWorkflowId()
        );

        List<WorkflowTransitionLog> storedLogs = workflowTransitionLogRepository.findByWorkflowId(
                createdResponse.getWorkflowId()
        );

        List<WorkflowHistoryResponseDto> history = workflowService.getWorkflowHistory(
                createdResponse.getWorkflowId()
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                WorkflowState.REVIEWED,
                updatedResponse.getCurrentState()
        );

        assertTruePresent(
                storedAfterTransition
        );

        assertEquals(
                WorkflowState.REVIEWED,
                storedAfterTransition.get().getCurrentState()
        );

        assertFalse(
                storedLogs.isEmpty()
        );

        WorkflowTransitionLog storedLog = storedLogs.get(0);

        assertEquals(
                createdResponse.getWorkflowId(),
                storedLog.getWorkflowId()
        );

        assertEquals(
                "ENTERED",
                storedLog.getFromState()
        );

        assertEquals(
                "REVIEWED",
                storedLog.getToState()
        );

        assertEquals(
                "reviewer",
                storedLog.getPerformedBy()
        );

        assertEquals(
                "Integration review completed",
                storedLog.getJustification()
        );

        assertEquals(
                1,
                history.size()
        );

        assertEquals(
                "REVIEWED",
                history.get(0).getToState()
        );
    }

    private void assertTruePresent(Optional<WorkflowRecord> workflowRecord) {
        assertTrue(
                workflowRecord.isPresent()
        );
    }
}
