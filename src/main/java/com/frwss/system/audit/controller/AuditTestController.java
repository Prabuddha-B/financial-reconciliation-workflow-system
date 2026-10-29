package com.frwss.system.audit.controller;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.model.AuditLog;
import com.frwss.system.audit.service.AuditChainResult;
import com.frwss.system.audit.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditTestController {
    private final AuditService auditService;

    public AuditTestController(AuditService auditService){
        this.auditService = auditService;
    }

    // used for testing purposes only (that is why commented after testing)

//    @GetMapping("/audit/test-log")
//    public String createTestLog(){
//        AuditLog log = auditService.log(
//                AuditActionType.RECONCILIATION_STARTED,
//                "Reconciliation",
//                "RUN-001",
//                "admin",
//                "Test audit log created for hash-chain verification"
//        );
//
//        return "Audit log created with ID: " + log.getId();
//    }

    @GetMapping("/audit/verify-detailed")
    public String verifyAuditChainDetailed(){
        AuditChainResult result = auditService.verifyAuditChainDetailed();

        if (result.isValid()) {
            if (result.hasAnyBreaks()) {
                return "Audit chain is valid (" + result.getTotalBreakCount()
                        + " break(s) found, all acknowledged)";
            }
            return "Audit chain is valid";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Audit chain has ").append(result.getUnresolvedCount())
                .append(" unresolved break(s):\n");

        for (AuditChainResult.ChainBreak brk : result.getUnresolvedBreaks()) {
            sb.append("- Broken at log ID: ").append(brk.getBrokenAtId())
                    .append(" (").append(brk.getReason())
                    .append(" in field: ").append(brk.getFieldName())
                    .append(")\n");
        }

        return sb.toString();
    }
}