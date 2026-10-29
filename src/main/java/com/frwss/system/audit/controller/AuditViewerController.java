package com.frwss.system.audit.controller;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditChainResult;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.security.SecurityContextAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuditViewerController {
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    public AuditViewerController(AuditService auditService, SecurityContextAdapter securityContextAdapter){
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }


    @GetMapping("/audit/logs")
    public String showAuditLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String limit,
            Model model
    ){
        // Load filtered logs
        model.addAttribute(
                "logs",
                auditService.getFilteredLogs(actionType, entityType, performedBy, limit)
        );

        // Run detailed chain verification (walks the full chain, collects every break)
        AuditChainResult chainResult = auditService.verifyAuditChainDetailed();

        // Pass full result object to the view
        model.addAttribute("chainResult", chainResult);
        model.addAttribute("chainValid", chainResult.isValid());
        model.addAttribute("chainBreaks", chainResult.getBreaks());
        model.addAttribute("unresolvedBreaks", chainResult.getUnresolvedBreaks());
        model.addAttribute("totalBreakCount", chainResult.getTotalBreakCount());
        model.addAttribute("unresolvedBreakCount", chainResult.getUnresolvedCount());
        model.addAttribute("canAcknowledgeBreaks",
                securityContextAdapter.hasAnyRole("AUDITOR", "ADMIN"));

        // Fast per-row lookups for the table (avoids scanning the breaks list per row in Thymeleaf)
        java.util.Set<Long> unresolvedIds = new java.util.HashSet<>();
        java.util.Set<Long> acknowledgedIds = new java.util.HashSet<>();
        for (AuditChainResult.ChainBreak brk : chainResult.getBreaks()) {
            if (brk.isAcknowledged()) {
                acknowledgedIds.add(brk.getBrokenAtId());
            } else {
                unresolvedIds.add(brk.getBrokenAtId());
            }
        }
        model.addAttribute("unresolvedBrokenIds", unresolvedIds);
        model.addAttribute("acknowledgedBrokenIds", acknowledgedIds);

        java.util.Map<Long, String> brokenFieldByLogId = new java.util.HashMap<>();
        for (AuditChainResult.ChainBreak brk : chainResult.getBreaks()) {
            brokenFieldByLogId.put(brk.getBrokenAtId(), brk.getFieldName());
        }
        model.addAttribute("brokenFieldByLogId", brokenFieldByLogId);

        // Other UI attributes
        model.addAttribute("totalLogCount", auditService.getTotalLogCount());

        model.addAttribute("actionTypes", AuditActionType.values());

        model.addAttribute("selectedActionType",
                actionType == null || actionType.isBlank() ? "ALL" : actionType);

        model.addAttribute("selectedEntityType",
                entityType == null ? "" : entityType);

        model.addAttribute("selectedPerformedBy",
                performedBy == null ? "" : performedBy);

        model.addAttribute("selectedLimit",
                limit == null || limit.isBlank() ? "50" : limit);

        return "audit/logs";
    }

    /**
     * Acknowledges an investigated chain break by writing a new, normally
     * hash-chained AUDIT_CHAIN_CORRECTED entry. The original broken entry is
     * never modified - this only lets the chain be reported as valid again
     * from this point forward. Restricted to AUDITOR/ADMIN by SecurityConfig's
     * URL rule for /audit/**.
     */
    @PostMapping("/audit/acknowledge-break")
    public String acknowledgeChainBreak(
            @RequestParam Long brokenLogId,
            @RequestParam String justification,
            RedirectAttributes redirectAttributes
    ) {
        if (!securityContextAdapter.hasAnyRole("AUDITOR", "ADMIN")) {
            redirectAttributes.addFlashAttribute("acknowledgeError",
                    "You do not have permission to acknowledge chain breaks.");
            return "redirect:/audit/logs";
        }

        try {
            auditService.acknowledgeChainBreak(
                    brokenLogId,
                    securityContextAdapter.getCurrentUsername(),
                    justification
            );
            redirectAttributes.addFlashAttribute("acknowledgeSuccess",
                    "Break at entry #" + brokenLogId + " acknowledged. The chain is now valid from this point forward.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("acknowledgeError", e.getMessage());
        }

        return "redirect:/audit/logs";
    }
}