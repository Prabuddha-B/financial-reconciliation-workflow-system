// --------------------------------- Handles Workflow UI Pages ---------------------------------------

package com.frwss.system.workflow.controller;


import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.service.WorkflowService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;

@Controller
@RequestMapping("/workflow")
public class WorkflowViewController {
    private final WorkflowService workflowService;

    public WorkflowViewController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

//    Displays Workflow Approval Page
    @GetMapping("/approvals")
    public String showWorkflowApprovals(Model model) {
        model.addAttribute("workflows", workflowService.getAllWorkflows());
        return "workflow/approvals";
    }

//    Displays Workflow Status Page
    @GetMapping("/status/{workflowId}")
    public String showWorkflowStatus(@PathVariable Long workflowId, Model model) {
        model.addAttribute("workflow", workflowService.getWorkflowById(workflowId));
        model.addAttribute("history", workflowService.getWorkflowHistory(workflowId));
        return "workflow/status";
    }

//    Handles Workflow State Transitions
    @PostMapping("/{workflowId}/transition")
    public String updateWorkflowState(@PathVariable Long workflowId, @ModelAttribute WorkflowStateUpdateRequestDto request){
        workflowService.updateWorkflowState(workflowId, request);
        return "redirect:/workflow/status/" + workflowId;
    }
}

