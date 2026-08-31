package com.frwss.system.admin.service;

import com.frwss.system.admin.dto.AdminDashboardData;
import com.frwss.system.audit.model.AuditLog;
import com.frwss.system.audit.repository.AuditLogRepository;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.repository.ReconciliationResultRepository;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.tax.repository.TaxConfigurationRepository;
import com.frwss.system.workflow.repository.WorkflowRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ReconciliationResultRepository reconciliationResultRepository;
    private final WorkflowRecordRepository workflowRepository;
    private final AuditLogRepository auditRepository;
    private final TaxConfigurationRepository taxRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            ReconciliationResultRepository reconciliationResultRepository,
            WorkflowRecordRepository workflowRepository,
            AuditLogRepository auditRepository,
            TaxConfigurationRepository taxRepository) {

        this.userRepository = userRepository;
        this.reconciliationResultRepository = reconciliationResultRepository;
        this.workflowRepository = workflowRepository;
        this.auditRepository = auditRepository;
        this.taxRepository = taxRepository;
    }

    // ------------------------------------------------------------
    // Builds Admin Dashboard Data
    // ------------------------------------------------------------
    public AdminDashboardData getDashboardData() {

        AdminDashboardData dashboard = new AdminDashboardData();

        // ------------------------------------------------------------
        // User Statistics
        // ------------------------------------------------------------
        dashboard.setTotalUsers(
                userRepository.count()
        );

        dashboard.setActiveUsers(
                userRepository.countByEnabledTrue()
        );

        dashboard.setDisabledUsers(
                userRepository.countByEnabledFalse()
        );

        // ------------------------------------------------------------
        // Role Statistics
        // ------------------------------------------------------------
        dashboard.setAdministratorCount(
                userRepository.countUsersByRole("ROLE_ADMIN")
        );

        dashboard.setApproverCount(
                userRepository.countUsersByRole("ROLE_APPROVER")
        );

        dashboard.setAuditorCount(
                userRepository.countUsersByRole("ROLE_AUDITOR")
        );

        dashboard.setEntryStaffCount(
                userRepository.countUsersByRole("ROLE_ENTRY_STAFF")
        );

        // ------------------------------------------------------------
        // Reconciliation Statistics
        // ------------------------------------------------------------
        dashboard.setTotalReconciliationResults(
                reconciliationResultRepository.count()
        );

        dashboard.setExactMatches(
                reconciliationResultRepository.findByStatus(ReconciliationStatus.EXACT).size()
        );

        dashboard.setPartialMatches(
                reconciliationResultRepository.findByStatus(ReconciliationStatus.PARTIAL).size()
        );

        dashboard.setOrphanRecords(
                reconciliationResultRepository.findByStatus(ReconciliationStatus.ORPHAN).size()
        );

        dashboard.setManuallyResolved(
                reconciliationResultRepository.findByStatus(ReconciliationStatus.MANUALLY_RESOLVED).size()
        );

        dashboard.setPendingApproval(
                reconciliationResultRepository.findByStatus(ReconciliationStatus.PENDING_APPROVAL).size()
        );

        dashboard.setApproved(
                reconciliationResultRepository.findByStatus(ReconciliationStatus.APPROVED).size()
        );

        dashboard.setRejected(
                reconciliationResultRepository.findByStatus(ReconciliationStatus.REJECTED).size()
        );

        // Bulk matches - you'll need to add this to BulkReconciliationRepository
        // dashboard.setBulkMatches(bulkReconciliationRepository.count());

        // ------------------------------------------------------------
        // Workflow Statistics
        // ------------------------------------------------------------
        dashboard.setTotalWorkflows(
                workflowRepository.count()
        );

        // TODO: Replace with actual workflow status counts
        dashboard.setPendingWorkflows(0);
        dashboard.setApprovedWorkflows(0);
        dashboard.setRejectedWorkflows(0);

        // ------------------------------------------------------------
        // Audit Statistics
        // ------------------------------------------------------------
        dashboard.setTotalAuditLogs(
                auditRepository.count()
        );

        // ------------------------------------------------------------
        // Tax Configuration Statistics
        // ------------------------------------------------------------
        dashboard.setTotalTaxConfigurations(
                taxRepository.count()
        );

        // ------------------------------------------------------------
        // System Health
        // ------------------------------------------------------------
        dashboard.setSystemStatus("ONLINE");

        return dashboard;
    }

    public List<AuditLog> getRecentActivities() {
        return auditRepository.findTop5ByOrderByTimestampDesc();
    }
}
