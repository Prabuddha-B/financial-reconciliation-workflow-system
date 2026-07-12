package com.frwss.system.workflow.controller;

import com.frwss.system.security.SecurityContextAdapter;
import com.frwss.system.workflow.service.WorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.model.WorkflowState;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Workflow View Controller Unit Tests")
class WorkflowViewControllerTest {

    @Mock
    private WorkflowService workflowService;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @InjectMocks
    private WorkflowViewController workflowViewController;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies automatically.
    }

    // =========================================================================
    // showWorkflowApprovals()
    // =========================================================================

    @Test
    @DisplayName("Should display workflow approvals page")
    void showWorkflowApprovals_returnsApprovalView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        WorkflowResponseDto workflow1 = new WorkflowResponseDto();
        workflow1.setCurrentState(WorkflowState.REVIEWED);

        WorkflowResponseDto workflow2 = new WorkflowResponseDto();
        workflow2.setCurrentState(WorkflowState.ENTERED);

        when(workflowService.getAllWorkflows())
                .thenReturn(List.of(workflow1, workflow2));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                workflowViewController.showWorkflowApprovals(model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("workflow/approvals", view);

        assertNotNull(model.getAttribute("workflows"));
        assertEquals(1L, model.getAttribute("needsAttentionCount"));

        verify(workflowService).getAllWorkflows();
    }

    @Test
    @DisplayName("Should return status page when workflow service throws an exception")
    void updateWorkflowState_serviceThrowsException_returnsStatusPage() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        Model model = new ConcurrentModel();

        WorkflowStateUpdateRequestDto request =
                new WorkflowStateUpdateRequestDto();

        request.setNewState(WorkflowState.REVIEWED);

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("approver");

        when(securityContextAdapter.getCurrentUserRole())
                .thenReturn("APPROVER");

        doThrow(new RuntimeException("Workflow update failed"))
                .when(workflowService)
                .updateWorkflowState(workflowId, request);

        when(workflowService.getWorkflowById(workflowId))
                .thenReturn(new WorkflowResponseDto());

        when(workflowService.getWorkflowHistory(workflowId))
                .thenReturn(List.of());

        when(workflowService.getLatestWorkflowActivity(workflowId))
                .thenReturn(null);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                workflowViewController.updateWorkflowState(
                        workflowId,
                        request,
                        model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("workflow/status", view);

        assertEquals(
                "Workflow update failed",
                model.getAttribute("errorMessage"));
    }

    // =========================================================================
    // showWorkflowStatus()
    // =========================================================================

    @Test
    @DisplayName("Should display workflow status page with workflow details and history")
    void showWorkflowStatus_returnsStatusView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        Model model = new ConcurrentModel();

        WorkflowResponseDto workflow = new WorkflowResponseDto();

        WorkflowHistoryResponseDto history = new WorkflowHistoryResponseDto();
        history.setTransitionTime(LocalDateTime.now());

        when(workflowService.getWorkflowById(workflowId))
                .thenReturn(workflow);

        when(workflowService.getWorkflowHistory(workflowId))
                .thenReturn(List.of(history));

        when(workflowService.getLatestWorkflowActivity(workflowId))
                .thenReturn(history);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                workflowViewController.showWorkflowStatus(workflowId, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("workflow/status", view);

        assertNotNull(model.getAttribute("workflow"));
        assertNotNull(model.getAttribute("history"));
        assertNotNull(model.getAttribute("latestTransition"));
        assertNotNull(model.getAttribute("lastUpdated"));

        verify(workflowService).getWorkflowById(workflowId);
        verify(workflowService).getWorkflowHistory(workflowId);
        verify(workflowService).getLatestWorkflowActivity(workflowId);
    }

    // =========================================================================
    // updateWorkflowState()
    // =========================================================================

    @Test
    @DisplayName("Should redirect to workflow status page after successful transition")
    void updateWorkflowState_successfulTransition_redirectsToStatusPage() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        Model model = new ConcurrentModel();

        WorkflowStateUpdateRequestDto request =
                new WorkflowStateUpdateRequestDto();

        request.setNewState(WorkflowState.REVIEWED);

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("approver");

        when(securityContextAdapter.getCurrentUserRole())
                .thenReturn("APPROVER");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                workflowViewController.updateWorkflowState(
                        workflowId,
                        request,
                        model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("redirect:/workflow/status/1", view);

        verify(workflowService)
                .updateWorkflowState(workflowId, request);
    }

    @Test
    @DisplayName("Should return status page when user lacks required role")
    void updateWorkflowState_invalidRole_returnsStatusView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        Model model = new ConcurrentModel();

        WorkflowStateUpdateRequestDto request =
                new WorkflowStateUpdateRequestDto();

        request.setNewState(WorkflowState.REVIEWED);

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("reviewer");

        when(securityContextAdapter.getCurrentUserRole())
                .thenReturn("AUDITOR");

        when(workflowService.getWorkflowById(workflowId))
                .thenReturn(new WorkflowResponseDto());

        when(workflowService.getWorkflowHistory(workflowId))
                .thenReturn(List.of());

        when(workflowService.getLatestWorkflowActivity(workflowId))
                .thenReturn(null);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                workflowViewController.updateWorkflowState(
                        workflowId,
                        request,
                        model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("workflow/status", view);

        assertEquals(
                "Only Approvers can review workflows.",
                model.getAttribute("errorMessage"));
    }

    @Test
    @DisplayName("Should return status page when workflow service throws an exception")
    void updateWorkflowState_serviceThrowsException_returnsStatusView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long workflowId = 1L;

        Model model = new ConcurrentModel();

        WorkflowStateUpdateRequestDto request =
                new WorkflowStateUpdateRequestDto();

        request.setNewState(WorkflowState.REVIEWED);

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("approver");

        when(securityContextAdapter.getCurrentUserRole())
                .thenReturn("APPROVER");

        doThrow(new RuntimeException("Workflow update failed"))
                .when(workflowService)
                .updateWorkflowState(workflowId, request);

        when(workflowService.getWorkflowById(workflowId))
                .thenReturn(new WorkflowResponseDto());

        when(workflowService.getWorkflowHistory(workflowId))
                .thenReturn(List.of());

        when(workflowService.getLatestWorkflowActivity(workflowId))
                .thenReturn(null);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                workflowViewController.updateWorkflowState(
                        workflowId,
                        request,
                        model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("workflow/status", view);

        assertEquals(
                "Workflow update failed",
                model.getAttribute("errorMessage"));
    }
}