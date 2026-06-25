// --------------------------------- Handles Workflow UI Pages ---------------------------------------

package com.frwss.system.workflow.controller;


import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.service.WorkflowService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Comparator;
import java.util.List;
import com.frwss.system.security.SecurityContextAdapter;

@Controller
@RequestMapping("/workflow")
public class WorkflowViewController {
    private final WorkflowService workflowService;
    private final SecurityContextAdapter securityContextAdapter;


    public WorkflowViewController(WorkflowService workflowService, SecurityContextAdapter securityContextAdapter) {
        this.workflowService = workflowService;
        this.securityContextAdapter = securityContextAdapter;
    }

    //    Displays Workflow Approval Page
    @GetMapping("/approvals")
    public String showWorkflowApprovals(Model model) {

        List<WorkflowResponseDto> workflows = workflowService.getAllWorkflows();

        long needsAttentionCount = workflows.stream().filter(workflow -> workflow.getCurrentState().name().equals("REVIEWED")).count();

        model.addAttribute("workflows", workflows);
        model.addAttribute("needsAttentionCount", needsAttentionCount);

        return "workflow/approvals";
    }

//    Displays Workflow Status Page
    @GetMapping("/status/{workflowId}")
    public String showWorkflowStatus(@PathVariable Long workflowId, Model model) {
        List<WorkflowHistoryResponseDto> history = workflowService.getWorkflowHistory(workflowId);

        model.addAttribute("workflow", workflowService.getWorkflowById(workflowId));
        model.addAttribute("currentUser",securityContextAdapter.getCurrentUsername());
        model.addAttribute("history", history);
        model.addAttribute("userRole", securityContextAdapter.getCurrentUserRole());
        model.addAttribute("latestTransition", workflowService.getLatestWorkflowActivity(workflowId));
        model.addAttribute("lastUpdated", history.stream()
                .map(WorkflowHistoryResponseDto::getTransitionTime)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        return "workflow/status";
    }

//    Handles Workflow State Transitions
    @PostMapping("/{workflowId}/transition")
    public String updateWorkflowState(
            @PathVariable Long workflowId,
            @ModelAttribute WorkflowStateUpdateRequestDto request,
            Model model) {
        {
            try {
                request.setPerformedBy(securityContextAdapter.getCurrentUsername());

                String role = securityContextAdapter.getCurrentUserRole();

                if (request.getNewState().name().equals("REVIEWED")
                        && !role.equals("APPROVER")
                        && !role.equals("ADMIN")) {

                    throw new RuntimeException("Only Approvers can review workflows.");
                }

                if ((request.getNewState().name().equals("VERIFIED")
                        || request.getNewState().name().equals("REJECTED"))
                        && !role.equals("AUDITOR")
                        && !role.equals("ADMIN")) {

                    throw new RuntimeException("Only Auditors can verify or reject workflows.");
                }

                workflowService.updateWorkflowState(workflowId, request);
                return "redirect:/workflow/status/" + workflowId;
            } catch ( RuntimeException ex ) {
                List<WorkflowHistoryResponseDto> history = workflowService.getWorkflowHistory(workflowId);

                model.addAttribute("workflow", workflowService.getWorkflowById(workflowId));
                model.addAttribute("history", history);
                model.addAttribute("latestTransition", workflowService.getLatestWorkflowActivity(workflowId));
                model.addAttribute("lastUpdated", history.stream()
                        .map(WorkflowHistoryResponseDto::getTransitionTime)
                        .filter(java.util.Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(null));
                model.addAttribute("errorMessage", ex.getMessage());
                return "workflow/status";
            }
        }
    }
}
