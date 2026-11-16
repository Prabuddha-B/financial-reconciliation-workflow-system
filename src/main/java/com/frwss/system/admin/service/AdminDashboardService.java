// -------------------------------------------------------------------------------
//       Aggregates system statistics for the Administrator Dashboard
// -------------------------------------------------------------------------------

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

    /**
     * Collects user, reconciliation, workflow, audit, and tax configuration
     * statistics and combines them into an AdminDashboardData object.
     *
     * @return aggregated dashboard statistics
     */
    public AdminDashboardData getDashboardData() {

        AdminDashboardData dashboard = new AdminDashboardData();


        // User Statistics
        dashboard.setTotalUsers(userRepository.count());
        dashboard.setActiveUsers(userRepository.countByEnabledTrue());
        dashboard.setDisabledUsers(userRepository.countByEnabledFalse());

        // Role Statistics
        dashboard.setAdministratorCount(userRepository.countUsersByRole("ROLE_ADMIN"));
        dashboard.setApproverCount(userRepository.countUsersByRole("ROLE_APPROVER"));
        dashboard.setAuditorCount(userRepository.countUsersByRole("ROLE_AUDITOR"));
        dashboard.setEntryStaffCount(userRepository.countUsersByRole("ROLE_ENTRY_STAFF"));

        // Reconciliation Statistics
        dashboard.setTotalReconciliationResults(reconciliationResultRepository.count());
        dashboard.setExactMatches(reconciliationResultRepository.findByStatus(ReconciliationStatus.EXACT).size());
        dashboard.setPartialMatches(reconciliationResultRepository.findByStatus(ReconciliationStatus.PARTIAL).size());
        dashboard.setOrphanRecords(reconciliationResultRepository.findByStatus(ReconciliationStatus.ORPHAN).size());
        dashboard.setManuallyResolved(reconciliationResultRepository.findByStatus(ReconciliationStatus.MANUALLY_RESOLVED).size());
        dashboard.setPendingApproval(reconciliationResultRepository.findByStatus(ReconciliationStatus.PENDING_APPROVAL).size());
        dashboard.setApproved(reconciliationResultRepository.findByStatus(ReconciliationStatus.APPROVED).size());
        dashboard.setRejected(reconciliationResultRepository.findByStatus(ReconciliationStatus.REJECTED).size());

        // Bulk matches
        // dashboard.setBulkMatches(bulkReconciliationRepository.count());


        // Workflow Statistics
        dashboard.setTotalWorkflows(workflowRepository.count());


        dashboard.setPendingWorkflows(0);
        dashboard.setApprovedWorkflows(0);
        dashboard.setRejectedWorkflows(0);

        // Audit Statistics
        dashboard.setTotalAuditLogs(auditRepository.count());


        // Tax Configuration Statistics
        dashboard.setTotalTaxConfigurations(taxRepository.count());

        // System Health
        dashboard.setSystemStatus("ONLINE");

        return dashboard;
    }

    /**
     * Retrieves the five most recent audit log entries in descending
     * timestamp order for display on the Administrator Dashboard.
     *
     * @return list of the latest five audit activities
     */
    public List<AuditLog> getRecentActivities() {
        return auditRepository.findTop5ByOrderByTimestampDesc();
    }
}
