package com.frwss.system.reconciliation.controller;

import com.frwss.system.reconciliation.model.BulkReconciliation;
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

/**
 * Pages and form actions of the reconciliation module (everything under /reconciliation). Page
 * methods only show data. POST methods run a reconciliation step or record a reviewer's decision
 * and then redirect back with a success or error message. Changing data needs ENTRY_STAFF or ADMIN
 * (see SecurityConfig); reading is open to any logged-in user.
 */
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

    /**
     * GET /reconciliation. Shows the reconciliation results with two optional filters: status
     * (EXACT, PARTIAL, ORPHAN, ... or ALL) and limit (number of rows or ALL, default 50).
     */
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

        try {
            List<BulkReconciliation> bulkResults = reconciliationService.getAllBulkReconciliationResults();
            model.addAttribute("bulkResults", bulkResults != null ? bulkResults : java.util.Collections.emptyList());
        } catch (Exception e) {
            // If the method or table doesn't exist yet, use empty list
            model.addAttribute("bulkResults", java.util.Collections.emptyList());
        }
        return "reconciliation/dashboard";
    }

    /**
     * Keeps only the first N results for display. A limit of -1 (ALL) returns the list unchanged.
     */
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

    /**
     * Converts the limit request value to a number: blank gives 50, ALL gives -1 (no limit), and
     * text that is not a number also falls back to 50.
     */
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

    /**
     * GET /reconciliation/orphans. Lists every orphan result (no match found) so a reviewer can
     * choose one to resolve.
     */
    @GetMapping("/orphans")
    public String showOrphanResolutionView(Model model){
        model.addAttribute("orphans", reconciliationService.getOrphanRecordViews());
        return "reconciliation/orphans";
    }

    /**
     * GET /reconciliation/orphans/{resultId}/candidates. Shows one orphan with the suggested
     * source records (same amount, date within 7 days, best score first) that the reviewer can
     * choose from.
     */
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

    /**
     * GET /reconciliation/manual-review. Lists the results a reviewer has resolved manually, each
     * with a button to send it to workflow review.
     */
    @GetMapping("/manual-review")
    public String showManualReviewPage(Model model){
        model.addAttribute("reviewRecords", reconciliationService.getManualReviewRecords());

        return "reconciliation/manual-review";
    }

    /**
     * POST /reconciliation/run-exact. Runs one batch of exact matching as the logged-in user, then
     * returns to the results page with a success message.
     */
    @PostMapping("/run-exact")
    public String runExactReconciliation(RedirectAttributes redirectAttributes){
        reconciliationService.runExactReconciliation(securityContextAdapter.getCurrentUsername());

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Exact reconciliation completed successfully."
        );

        return "redirect:/reconciliation";
    }

    /**
     * POST /reconciliation/run-partial. Runs one batch of partial matching as the logged-in user,
     * with a fixed tolerance of 3 days.
     */
    @PostMapping("/run-partial")
    public String runPartialReconciliation(RedirectAttributes redirectAttributes){
        reconciliationService.runPartialReconciliation(securityContextAdapter.getCurrentUsername(), 3);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Partial reconciliation completed successfully with ±3 days tolerance."
        );

        return "redirect:/reconciliation";
    }

    /**
     * POST /reconciliation/orphans/{resultId}/resolve-manual-entry. Resolves an orphan with a source
     * record ID the reviewer typed in (used when there are no suggested candidates). The confidence
     * is not accepted from the form; the service sets it. If a check fails, the reviewer goes back
     * to the candidates page with the reason shown.
     */
    @PostMapping("/orphans/{resultId}/resolve-manual-entry")
    public String resolveOrphanWithManualEntry(
            @PathVariable Long resultId,
            @RequestParam String sourceType,
            @RequestParam String matchedRecordId,
            @RequestParam String justification,
            RedirectAttributes redirectAttributes
    ){
        try {
            reconciliationService.resolveOrphanWithManualEntry(
                    resultId,
                    sourceType,
                    matchedRecordId,
                    justification,
                    securityContextAdapter.getCurrentUsername()
            );
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/reconciliation/orphans/" + resultId + "/candidates";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Orphan record manually resolved successfully"
        );

        return "redirect:/reconciliation/orphans";
    }

    /**
     * POST /reconciliation/orphans/{resultId}/resolve. Saves the reviewer's choice for an orphan
     * (source type, matched record id and confidence) together with the required justification,
     * then redirects with a message.
     */
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
                securityContextAdapter.getCurrentUsername()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Orphan record manually resolved successfully"
        );

        return "redirect:/reconciliation/orphans";
    }

    /**
     * POST /reconciliation/manual-review/{resultId}/approve. Approves a manually resolved result
     * directly. In normal use approval happens through the workflow module (the auditor's VERIFIED
     * step), so no page button calls this.
     */
    @PostMapping("/manual-review/{resultId}/approve")
    public String approveManualResolution(
            @PathVariable Long resultId,
            RedirectAttributes redirectAttributes
    ){
        reconciliationService.approveManualResolution(resultId, securityContextAdapter.getCurrentUsername());

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Manual resolultion approved successfully."
        );

        return "redirect:/reconciliation/manual-review";
    }

    /**
     * POST /reconciliation/manual-review/{resultId}/reject. Rejects a manually resolved result
     * with a reason. As with approve, the workflow module normally does this.
     */
    @PostMapping("/manual-review/{resultId}/reject")
    public String rejectManualResolution(
            @PathVariable Long resultId,
            @RequestParam String rejectionReason,
            RedirectAttributes redirectAttributes
    ){
        reconciliationService.rejectManualResolution(
                resultId,
                rejectionReason,
                securityContextAdapter.getCurrentUsername()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Manual resolution rejected successfully."
        );

        return "redirect:/reconciliation/manual-review";
    }

    /**
     * POST /reconciliation/manual-review/{resultId}/send-to-workflow. Submits a manually resolved
     * result to the workflow module. A result can only be submitted once: if a workflow item
     * already exists for it, an error message is shown. Any failure is reported on the manual
     * review page instead of being thrown.
     */
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

    // Run One-to-Many Reconciliation
    @PostMapping("/run-one-to-many")
    public String runOneToManyReconciliation(
            @RequestParam(defaultValue = "5") int tolerancePercent,
            RedirectAttributes redirectAttributes) {

        try {
            String performedBy = securityContextAdapter.getCurrentUsername();
            reconciliationService.runOneToManyReconciliation(performedBy, tolerancePercent);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "One-to-many reconciliation completed successfully with ±" +
                            tolerancePercent + "% tolerance."
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "One-to-many reconciliation failed: " + e.getMessage()
            );
        }

        return "redirect:/reconciliation";
    }

    // View Bulk Reconciliation Results
    @GetMapping("/bulk-results")
    public String showBulkResults(Model model) {
        List<BulkReconciliation> bulkResults = reconciliationService.getAllBulkReconciliationResults();
        model.addAttribute("bulkResults", bulkResults);
        // Child records for each bulk result, shown in the detail panel when a row is clicked
        model.addAttribute("bulkChildren", reconciliationService.getChildRecordsForBulkResults(bulkResults));
        return "reconciliation/bulk-results";
    }


}
