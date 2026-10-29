package com.frwss.system.admin.dto;

/**
 * ------------------------------------------------------------
 * Admin Dashboard Data DTO
 * ------------------------------------------------------------
 * Holds all statistics displayed on the Admin Dashboard.
 * This object is populated by the AdminDashboardService
 * and passed to the dashboard view.
 * ------------------------------------------------------------
 */
public class AdminDashboardData {

    // ------------------------------------------------------------
    // User Statistics
    // ------------------------------------------------------------
    private long totalUsers;
    private long activeUsers;
    private long disabledUsers;

    // ------------------------------------------------------------
    // Role Statistics
    // ------------------------------------------------------------
    private long administratorCount;
    private long approverCount;
    private long auditorCount;
    private long entryStaffCount;

    // ------------------------------------------------------------
    // Reconciliation Statistics
    // ------------------------------------------------------------
    private long totalReconciliationResults;
    private long exactMatches;
    private long partialMatches;
    private long orphanRecords;
    private long manuallyResolved;
    private long pendingApproval;
    private long approved;
    private long rejected;
    private long bulkMatches;

    // ------------------------------------------------------------
    // Workflow Statistics
    // ------------------------------------------------------------
    private long totalWorkflows;
    private long pendingWorkflows;
    private long approvedWorkflows;
    private long rejectedWorkflows;

    // ------------------------------------------------------------
    // Audit Statistics
    // ------------------------------------------------------------
    private long totalAuditLogs;

    // ------------------------------------------------------------
    // Tax Configuration Statistics
    // ------------------------------------------------------------
    private long totalTaxConfigurations;

    // ------------------------------------------------------------
    // System Health
    // ------------------------------------------------------------
    private String systemStatus;

    // ============================================================
    // Getters & Setters
    // ============================================================

    // User Statistics
    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getDisabledUsers() {
        return disabledUsers;
    }

    public void setDisabledUsers(long disabledUsers) {
        this.disabledUsers = disabledUsers;
    }

    // Role Statistics
    public long getAdministratorCount() {
        return administratorCount;
    }

    public void setAdministratorCount(long administratorCount) {
        this.administratorCount = administratorCount;
    }

    public long getApproverCount() {
        return approverCount;
    }

    public void setApproverCount(long approverCount) {
        this.approverCount = approverCount;
    }

    public long getAuditorCount() {
        return auditorCount;
    }

    public void setAuditorCount(long auditorCount) {
        this.auditorCount = auditorCount;
    }

    public long getEntryStaffCount() {
        return entryStaffCount;
    }

    public void setEntryStaffCount(long entryStaffCount) {
        this.entryStaffCount = entryStaffCount;
    }

    // Reconciliation Statistics
    public long getTotalReconciliationResults() {
        return totalReconciliationResults;
    }

    public void setTotalReconciliationResults(long totalReconciliationResults) {
        this.totalReconciliationResults = totalReconciliationResults;
    }

    public long getExactMatches() {
        return exactMatches;
    }

    public void setExactMatches(long exactMatches) {
        this.exactMatches = exactMatches;
    }

    public long getPartialMatches() {
        return partialMatches;
    }

    public void setPartialMatches(long partialMatches) {
        this.partialMatches = partialMatches;
    }

    public long getOrphanRecords() {
        return orphanRecords;
    }

    public void setOrphanRecords(long orphanRecords) {
        this.orphanRecords = orphanRecords;
    }

    public long getManuallyResolved() {
        return manuallyResolved;
    }

    public void setManuallyResolved(long manuallyResolved) {
        this.manuallyResolved = manuallyResolved;
    }

    public long getPendingApproval() {
        return pendingApproval;
    }

    public void setPendingApproval(long pendingApproval) {
        this.pendingApproval = pendingApproval;
    }

    public long getApproved() {
        return approved;
    }

    public void setApproved(long approved) {
        this.approved = approved;
    }

    public long getRejected() {
        return rejected;
    }

    public void setRejected(long rejected) {
        this.rejected = rejected;
    }

    public long getBulkMatches() {
        return bulkMatches;
    }

    public void setBulkMatches(long bulkMatches) {
        this.bulkMatches = bulkMatches;
    }

    // Workflow Statistics
    public long getTotalWorkflows() {
        return totalWorkflows;
    }

    public void setTotalWorkflows(long totalWorkflows) {
        this.totalWorkflows = totalWorkflows;
    }

    public long getPendingWorkflows() {
        return pendingWorkflows;
    }

    public void setPendingWorkflows(long pendingWorkflows) {
        this.pendingWorkflows = pendingWorkflows;
    }

    public long getApprovedWorkflows() {
        return approvedWorkflows;
    }

    public void setApprovedWorkflows(long approvedWorkflows) {
        this.approvedWorkflows = approvedWorkflows;
    }

    public long getRejectedWorkflows() {
        return rejectedWorkflows;
    }

    public void setRejectedWorkflows(long rejectedWorkflows) {
        this.rejectedWorkflows = rejectedWorkflows;
    }

    // Audit Statistics
    public long getTotalAuditLogs() {
        return totalAuditLogs;
    }

    public void setTotalAuditLogs(long totalAuditLogs) {
        this.totalAuditLogs = totalAuditLogs;
    }

    // Tax Statistics
    public long getTotalTaxConfigurations() {
        return totalTaxConfigurations;
    }

    public void setTotalTaxConfigurations(long totalTaxConfigurations) {
        this.totalTaxConfigurations = totalTaxConfigurations;
    }

    // System Health
    public String getSystemStatus() {
        return systemStatus;
    }

    public void setSystemStatus(String systemStatus) {
        this.systemStatus = systemStatus;
    }
}