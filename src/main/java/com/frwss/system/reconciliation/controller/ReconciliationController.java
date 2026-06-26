package com.frwss.system.reconciliation.controller;

import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.service.ReconciliationService;
import com.frwss.system.security.SecurityContextAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.frwss.system.workflow.service.WorkflowService;
import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.repository.WorkflowRecordRepository;

import java.util.List;

@Controller
@RequestMapping("/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private final WorkflowService workflowService;
    private final WorkflowRecordRepository workflowRecordRepository;
    private final SecurityContextAdapter securityContextAdapter;

    public ReconciliationController(ReconciliationService reconciliationService,
                                    WorkflowService workflowService,
                                    WorkflowRecordRepository workflowRecordRepository, SecurityContextAdapter securityContextAdapter) {
        this.reconciliationService = reconciliationService;
        this.workflowService = workflowService;
        this.workflowRecordRepository = workflowRecordRepository;
        this.securityContextAdapter = securityContextAdapter;
    }

    @GetMapping
    public String showReconciliationDashboard(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String limit,
            Model model
    ){
        List<ReconciliationResult> results;

        if(status != null && !status.isBlank() && !status.equals("ALL")){
            ReconciliationStatus selectedStatus = ReconciliationStatus.valueOf(status);
            results = reconciliationService.getResultsByStatus(selectedStatus);
        } else {
            results = reconciliationService.getAllResults();
        }

        results = applyDisplayLimit(results, limit);

        model.addAttribute("results", results);
        model.addAttribute("selectedStatus", status == null ? "ALL" : status);
        model.addAttribute("selectedLimit", limit == null || limit.isBlank() ? "50" : limit);
        model.addAttribute("statuses", ReconciliationStatus.values());


        model.addAttribute("allResults", reconciliationService.getAllResults());
        model.addAttribute("exactResults", reconciliationService.getResultsByStatus(ReconciliationStatus.EXACT));
        model.addAttribute("orphanResults", reconciliationService.getResultsByStatus(ReconciliationStatus.ORPHAN));
        model.addAttribute("partialResults", reconciliationService.getResultsByStatus(ReconciliationStatus.PARTIAL));
        model.addAttribute("pendingApprovalResults", reconciliationService.getResultsByStatus(ReconciliationStatus.PENDING_APPROVAL));
        model.addAttribute("manualResults", reconciliationService.getResultsByStatus(ReconciliationStatus.MANUALLY_RESOLVED));
        model.addAttribute("approvedResults", reconciliationService.getResultsByStatus(ReconciliationStatus.APPROVED));
        model.addAttribute("rejectedResults", reconciliationService.getResultsByStatus(ReconciliationStatus.REJECTED));
        return "reconciliation/dashboard";
    }

    private List<ReconciliationResult> applyDisplayLimit(
            List<ReconciliationResult> results,
            String limit
    ) {
        int selectedLimit = parseLimit(limit);

        if (selectedLimit == -1) {
            return results;
        }

        return results.stream()
                .limit(selectedLimit)
                .toList();
    }

    private int parseLimit(String limit) {
        if (limit == null || limit.isBlank()) {
            return 50;
        }

        if (limit.equalsIgnoreCase("ALL")) {
            return -1;
        }

        try {
            return Integer.parseInt(limit);
        } catch (NumberFormatException e) {
            return 50;
        }
    }

    @GetMapping("/orphans")
    public String showOrphanResolutionView(Model model){
        model.addAttribute("orphans", reconciliationService.getOrphanRecordViews());
        return "reconciliation/orphans";
    }

    @GetMapping("/orphans/{resultId}/candidates")
    public String showCandidateMatches(
            @PathVariable Long resultId,
            Model model
    ){
        model.addAttribute("candidates", reconciliationService.getCandidateMatchesForOrphan(resultId));
        model.addAttribute("orphanRecord",reconciliationService.getOrphanRecordView(resultId));
        model.addAttribute("resultId", resultId);

        return "reconciliation/candidates";
    }

    @GetMapping("/manual-review")
    public String showManualReviewPage(Model model){
        model.addAttribute("reviewRecords", reconciliationService.getManualReviewRecords());

        return "reconciliation/manual-review";
    }

    @PostMapping("/run-exact")
    public String runExactReconciliation(RedirectAttributes redirectAttributes){
        // temporary user until Spring Security is fully integrated
        reconciliationService.runExactReconciliation("admin");

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Exact reconciliation completed successfully."
        );

        return "redirect:/reconciliation";
    }

    @PostMapping("/run-partial")
    public String runPartialReconciliation(RedirectAttributes redirectAttributes){
        reconciliationService.runPartialReconciliation("admin", 3);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Partial reconciliation completed successfully with ±3 days tolerance."
        );

        return "redirect:/reconciliation";
    }

    @PostMapping("/orphans/{resultId}/resolve")
    public String resolveOrphanManually(
            @PathVariable Long resultId,
            @RequestParam String sourceType,
            @RequestParam String matchedRecordId,
            @RequestParam Double confidenceScore,
            @RequestParam String justification,
            RedirectAttributes redirectAttributes
    ){
        reconciliationService.resolveOrphanManually(
                resultId,
                sourceType,
                matchedRecordId,
                confidenceScore,
                justification,
                "admin"
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Orphan record manually resolved successfully"
        );

        return "redirect:/reconciliation/orphans";
    }

    @PostMapping("/manual-review/{resultId}/approve")
    public String approveManualResolution(
            @PathVariable Long resultId,
            RedirectAttributes redirectAttributes
    ){
        reconciliationService.approveManualResolution(resultId, "admin");

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Manual resolultion approved successfully."
        );

        return "redirect:/reconciliation/manual-review";
    }

    @PostMapping("/manual-review/{resultId}/reject")
    public String rejectManualResolution(
            @PathVariable Long resultId,
            @RequestParam String rejectionReason,
            RedirectAttributes redirectAttributes
    ){
        reconciliationService.rejectManualResolution(
                resultId,
                rejectionReason,
                "admin"
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Manual resolution rejected successfully."
        );

        return "redirect:/reconciliation/manual-review";
    }

    @PostMapping("/manual-review/{resultId}/send-to-workflow")
    public String sendToWorkflowReview(
            @PathVariable Long resultId,
            @RequestParam(required = false) String comments,
            RedirectAttributes redirectAttributes
    ){

        try{
            // Get current user from security adapter
            String performedBy = securityContextAdapter.getCurrentUsername();

            // Check if already in workflow
            // Prevent duplicate workflow creation for the same reconciliation record
            if (workflowRecordRepository
                    .findBySourceTypeAndSourceRecordId(
                            "RECONCILIATION",
                            resultId.toString()
                    )
                    .isPresent()) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "This record has already been submitted for workflow review."
                );

                return "redirect:/reconciliation/manual-review";
            }

            // Submit to workflow
            reconciliationService.sendToWorkflowReview(resultId, performedBy,comments);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Record submitted to workflow review successfully"
            );
        } catch (Exception e){
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to submit to workflow: " + e.getMessage()
            );
        }

        return "redirect:/reconciliation/manual-review";
    }
}
