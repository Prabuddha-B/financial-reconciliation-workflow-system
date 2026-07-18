package com.frwss.system.report.dto;

import java.time.LocalDate;

public class ReportFilterDto {
    private String status;
    private String module;
    private String reconciledBy;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String reportType; // PDF or Exce;

    // Getters and setters


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getReconciledBy() {
        return reconciledBy;
    }

    public void setReconciledBy(String reconciledBy) {
        this.reconciledBy = reconciledBy;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public boolean hasFilters(){
        return status != null || module != null || reconciledBy != null ||
                dateFrom != null || dateTo != null;
    }
}
