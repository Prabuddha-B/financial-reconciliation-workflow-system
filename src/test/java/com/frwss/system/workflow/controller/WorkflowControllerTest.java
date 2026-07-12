package com.frwss.system.workflow.controller;

import com.frwss.system.workflow.dto.*;
import com.frwss.system.workflow.service.WorkflowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Workflow REST Controller Unit Tests")
class WorkflowControllerTest {

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private WorkflowController workflowController;

    // =========================================================================
    // createWorkflow()
    // =========================================================================

    @Test
    @DisplayName("Should create workflow successfully")
    void createWorkflow_validRequest_returnsWorkflowResponse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        WorkflowRequestDto request = new WorkflowRequestDto();

        WorkflowResponseDto response = new WorkflowResponseDto();
        response.setWorkflowId(1L);

        when(workflowService.createWorkflow(request))
                .thenReturn(response);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto result =
                workflowController.createWorkflow(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(result);
        assertEquals(1L, result.getWorkflowId());

        verify(workflowService).createWorkflow(request);
    }

    // =========================================================================
    // updateWorkflowState()
    // =========================================================================

    @Test
    @DisplayName("Should update workflow state successfully")
    void updateWorkflowState_validRequest_returnsUpdatedWorkflow() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowStateUpdateRequestDto request =
                new WorkflowStateUpdateRequestDto();

        WorkflowResponseDto response =
                new WorkflowResponseDto();

        response.setWorkflowId(workflowId);

        when(workflowService.updateWorkflowState(workflowId, request))
                .thenReturn(response);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto result =
                workflowController.updateWorkflowState(
                        workflowId,
                        request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(workflowId, result.getWorkflowId());

        verify(workflowService)
                .updateWorkflowState(workflowId, request);
    }

    // =========================================================================
    // getWorkflowHistory()
    // =========================================================================

    @Test
    @DisplayName("Should return workflow history")
    void getWorkflowHistory_returnsHistoryList() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowHistoryResponseDto history =
                new WorkflowHistoryResponseDto();

        when(workflowService.getWorkflowHistory(workflowId))
                .thenReturn(List.of(history));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowHistoryResponseDto> result =
                workflowController.getWorkflowHistory(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.size());

        verify(workflowService)
                .getWorkflowHistory(workflowId);
    }

    // =========================================================================
    // getWorkflowById()
    // =========================================================================

    @Test
    @DisplayName("Should return workflow by ID")
    void getWorkflowById_existingWorkflow_returnsWorkflow() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        WorkflowResponseDto workflow =
                new WorkflowResponseDto();

        workflow.setWorkflowId(workflowId);

        when(workflowService.getWorkflowById(workflowId))
                .thenReturn(workflow);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto result =
                workflowController.getWorkflowById(workflowId);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(workflowId, result.getWorkflowId());

        verify(workflowService)
                .getWorkflowById(workflowId);
    }

    // =========================================================================
    // getAllWorkflows()
    // =========================================================================

    @Test
    @DisplayName("Should return all workflows")
    void getAllWorkflows_returnsWorkflowList() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        WorkflowResponseDto workflow =
                new WorkflowResponseDto();

        when(workflowService.getAllWorkflows())
                .thenReturn(List.of(workflow));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowResponseDto> result =
                workflowController.getAllWorkflows();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.size());

        verify(workflowService)
                .getAllWorkflows();
    }

}