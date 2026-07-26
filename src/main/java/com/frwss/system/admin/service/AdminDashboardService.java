package com.frwss.system.admin.service;

import com.frwss.system.admin.dto.AdminDashboardData;
import com.frwss.system.audit.repository.AuditLogRepository;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.tax.repository.TaxConfigurationRepository;
import com.frwss.system.workflow.repository.WorkflowRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;

    private final WorkflowRecordRepository workflowRepository;

    private final AuditLogRepository auditRepository;

    private final TaxConfigurationRepository taxRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            WorkflowRecordRepository workflowRepository,
            AuditLogRepository auditRepository,
            TaxConfigurationRepository taxRepository) {

        this.userRepository = userRepository;
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
        // Workflow Statistics
        // ------------------------------------------------------------
        dashboard.setTotalWorkflows(
                workflowRepository.count()
        );

        // These will be replaced with actual workflow status counts
        // when status-based repository queries are implemented.
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

}