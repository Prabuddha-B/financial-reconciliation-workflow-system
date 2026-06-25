// ------------------------- Exposes Workflow REST Endpoints ------------------

package com.frwss.system.workflow.controller;

import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import java.util.*;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

//     Creates a new workflow record
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponseDto createWorkflow(
            @RequestBody WorkflowRequestDto request) {

        return workflowService.createWorkflow(request);
    }

//      Updates Workflow State
    @PutMapping("/{workflowId}/state")
    public WorkflowResponseDto updateWorkflowState(
            @PathVariable Long workflowId,
            @RequestBody WorkflowStateUpdateRequestDto request) {

        return workflowService.updateWorkflowState(workflowId, request);
    }

//    Retrieves  Workflow History
    @GetMapping("/{workflowId}/history")
    public List<WorkflowHistoryResponseDto> getWorkflowHistory(@PathVariable Long workflowId) {
        return workflowService.getWorkflowHistory(workflowId);
    }

//    Retrieves a single workflow
    @GetMapping("/{workflowId}")
    public WorkflowResponseDto getWorkflowById(@PathVariable Long workflowId) {
        return workflowService.getWorkflowById(workflowId);
    }

//    Retrieves all workflows
    @GetMapping
    public List<WorkflowResponseDto> getAllWorkflows() {
        return workflowService.getAllWorkflows();
    }
}
