package com.frwss.system.controller;

import com.frwss.system.audit.model.AuditLog;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.audit.service.AuditChainResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.service.ReconciliationService;
import com.frwss.system.repository.AccountingRecordRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final AccountingRecordRepository accountingRecordRepository;
    private final ReconciliationService reconciliationService;
    private final AuditService auditService;

    public HomeController(
            AccountingRecordRepository accountingRecordRepository,
            ReconciliationService reconciliationService,
            AuditService auditService) {
        this.accountingRecordRepository = accountingRecordRepository;
        this.reconciliationService = reconciliationService;
        this.auditService = auditService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Total records
        long totalRecords = accountingRecordRepository.count();
        model.addAttribute("totalRecords", totalRecords);

        // Reconciliation counts
        long reconciledCount = reconciliationService.getAllResults().stream()
                .filter(r -> r.getStatus() != ReconciliationStatus.ORPHAN)
                .count();
        model.addAttribute("reconciledCount", reconciledCount);

        // Pending approval count
        long pendingApprovalCount = reconciliationService.getResultsByStatus(ReconciliationStatus.PENDING_APPROVAL).size();
        model.addAttribute("pendingApprovalCount", pendingApprovalCount);

        // Orphan count for quick link badge
        long orphanCount = reconciliationService.getResultsByStatus(ReconciliationStatus.ORPHAN).size();
        model.addAttribute("orphanCount", orphanCount);

        // Audit count
        long auditCount = auditService.getTotalLogCount();
        model.addAttribute("auditCount", auditCount);

        // Audit chain status
        AuditChainResult chainResult = auditService.verifyAuditChainDetailed();
        model.addAttribute("chainValid", chainResult.isValid());

        // Recent audit logs (last 5)
        List<AuditLog> recentLogs = auditService.getFilteredLogs(null, null, null, "5");
        model.addAttribute("recentLogs", recentLogs);

        return "home";
    }
}