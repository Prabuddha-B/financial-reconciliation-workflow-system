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

        if(result.isValid()){
            return "Audit chain is valid";
        } else {
            return "Audit chain is broken at log ID: "
                    + result.getBrokenAtId()
                    + " (" + result.getReason() + ")";
        }
    }
}
