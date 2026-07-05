package com.frwss.system.audit.controller;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditChainResult;
import com.frwss.system.audit.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditViewerController {
    private final AuditService auditService;

    public AuditViewerController(AuditService auditService){
        this.auditService = auditService;
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

        // Run detailed chain verification
        AuditChainResult chainResult = auditService.verifyAuditChainDetailed();

        // Pass full result object to the view
        model.addAttribute("chainResult", chainResult);
        model.addAttribute("chainValid", chainResult.isValid());
        model.addAttribute("brokenLogId", chainResult.getBrokenAtId());
        model.addAttribute("mismatchReason", chainResult.getReason());
        model.addAttribute("mismatchField", chainResult.getFieldName());

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
}
