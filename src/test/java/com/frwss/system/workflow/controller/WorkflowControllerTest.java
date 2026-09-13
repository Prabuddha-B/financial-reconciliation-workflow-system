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

    @Test
    @DisplayName("Should propagate workflow creation service exceptions")
    void createWorkflow_serviceFailure_propagatesException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(workflowService.createWorkflow(null))
                .thenThrow(new RuntimeException("Creation failed"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> workflowController.createWorkflow(null)
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Creation failed",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Should propagate update exceptions for an invalid ID")
    void updateWorkflowState_invalidId_serviceFailure_propagatesException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(workflowService.updateWorkflowState(-1L, null))
                .thenThrow(new RuntimeException("Invalid workflow ID"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> workflowController.updateWorkflowState(-1L, null)
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Invalid workflow ID",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Should pass a null update DTO to the service")
    void updateWorkflowState_nullDto_delegatesToService() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        WorkflowResponseDto response = new WorkflowResponseDto();
        response.setWorkflowId(1L);
        when(workflowService.updateWorkflowState(1L, null))
                .thenReturn(response);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        WorkflowResponseDto result = workflowController.updateWorkflowState(1L, null);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                1L,
                result.getWorkflowId()
        );
        verify(workflowService).updateWorkflowState(1L, null);
    }

    @Test
    @DisplayName("Should return an empty history for an invalid ID response")
    void getWorkflowHistory_invalidId_returnsEmptyResponse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(workflowService.getWorkflowHistory(-1L))
                .thenReturn(List.of());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowHistoryResponseDto> result = workflowController.getWorkflowHistory(-1L);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                result.isEmpty()
        );
        verify(workflowService).getWorkflowHistory(-1L);
    }

    @Test
    @DisplayName("Should propagate workflow lookup exceptions")
    void getWorkflowById_invalidId_serviceFailure_propagatesException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(workflowService.getWorkflowById(-1L))
                .thenThrow(new RuntimeException("Workflow not found"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> workflowController.getWorkflowById(-1L)
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Workflow not found",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Should return an empty workflow response")
    void getAllWorkflows_emptyResponse_returnsEmptyList() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(workflowService.getAllWorkflows())
                .thenReturn(List.of());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<WorkflowResponseDto> result = workflowController.getAllWorkflows();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                result.isEmpty()
        );
        verify(workflowService).getAllWorkflows();
    }

    @Test
    @DisplayName("Should propagate workflow list service exceptions")
    void getAllWorkflows_serviceFailure_propagatesException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(workflowService.getAllWorkflows())
                .thenThrow(new RuntimeException("Workflow list failed"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> workflowController.getAllWorkflows()
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Workflow list failed",
                exception.getMessage()
        );
    }
}
