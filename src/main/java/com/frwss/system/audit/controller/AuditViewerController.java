package com.frwss.system.audit.controller;

import com.frwss.system.audit.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditViewerController {
    private final AuditService auditService;

    public AuditViewerController(AuditService auditService){
        this.auditService = auditService;
    }

    @GetMapping("/audit/logs")
    public String showAuditLogs(Model model){
        model.addAttribute("logs", auditService.getAllLogs());
        model.addAttribute("chainValid", auditService.verifyAuditChain());

        return "audit/logs";
    }
}
