package com.frwss.system.reporting.controller;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.service.ReconciliationService;
import com.frwss.system.reporting.dto.ReportFilterDto;
import com.frwss.system.reporting.service.ExcelReportService;
import com.frwss.system.reporting.service.PdfReportService;
import com.frwss.system.security.SecurityContextAdapter;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReconciliationService reconciliationService;
    private final PdfReportService pdfReportService;
    private final ExcelReportService excelReportService;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    // Key to store report data in session
    private static final String SESSION_REPORT_DATA = "REPORT_DATA";
    private static final String SESSION_REPORT_NAME = "REPORT_NAME";
    private static final String SESSION_REPORT_TYPE = "REPORT_TYPE";

    public ReportController(
            ReconciliationService reconciliationService,
            PdfReportService pdfReportService,
            ExcelReportService excelReportService,
            AuditService auditService,
            SecurityContextAdapter securityContextAdapter) {
        this.reconciliationService = reconciliationService;
        this.pdfReportService = pdfReportService;
        this.excelReportService = excelReportService;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

    @GetMapping
    public String showReportsPage(Model model) {
        model.addAttribute("filter", new ReportFilterDto());
        model.addAttribute("statuses", ReconciliationStatus.values());
        model.addAttribute("modules", getAvailableModules());
        model.addAttribute("users", getAvailableUsers());

        // Get all results and calculate stats
        List<ReconciliationResult> allResults = reconciliationService.getAllResults();
        model.addAttribute("totalResults", allResults.size());

        // Calculate exact matches for display
        long exactCount = allResults.stream()
                .filter(r -> r.getStatus() == ReconciliationStatus.EXACT)
                .count();
        model.addAttribute("exactCount", exactCount);

        return "report/reports";
    }

    @PostMapping("/generate")
    public String generateReport(
            @ModelAttribute ReportFilterDto filter,
            Model model,
            HttpSession session) {

        try {
            log.info("Generating report with filters: {}", filter);

            List<ReconciliationResult> results = getFilteredResults(filter);
            model.addAttribute("resultCount", results.size());
            model.addAttribute("filter", filter);
            model.addAttribute("totalResults", reconciliationService.getAllResults().size());

            String currentUser = securityContextAdapter.getCurrentUsername();

            if ("PDF".equalsIgnoreCase(filter.getReportType())) {
                byte[] pdfBytes = pdfReportService.generateReconciliationReport(results, filter);

                // Store in session for download
                session.setAttribute(SESSION_REPORT_DATA, pdfBytes);
                session.setAttribute(SESSION_REPORT_NAME, generateFileName("pdf", filter));
                session.setAttribute(SESSION_REPORT_TYPE, "PDF");

                // Store in session for download
                model.addAttribute("reportData", true); // just a flag
                model.addAttribute("reportType", "PDF");
                model.addAttribute("fileName", generateFileName("pdf", filter));
                //model.addAttribute("reportGenerated", true);

                auditService.log(
                        AuditActionType.REPORT_EXPORTED,
                        "Report",
                        "PDF",
                        currentUser,
                        "PDF report generated with " + results.size() + " records"
                );

            } else if ("EXCEL".equalsIgnoreCase(filter.getReportType())) {
                byte[] excelBytes = excelReportService.generateReconciliationReport(results, filter);

                // Store in session for download
                session.setAttribute(SESSION_REPORT_DATA, excelBytes);
                session.setAttribute(SESSION_REPORT_NAME, generateFileName("xlsx", filter));
                session.setAttribute(SESSION_REPORT_TYPE, "EXCEL");

                model.addAttribute("reportData", true); // just a flag
                model.addAttribute("reportType", "EXCEL");
                model.addAttribute("fileName", generateFileName("xlsx", filter));
//                model.addAttribute("reportGenerated", true);

                auditService.log(
                        AuditActionType.REPORT_EXPORTED,
                        "Report",
                        "EXCEL",
                        currentUser,
                        "Excel report generated with " + results.size() + " records"
                );

            } else {
                model.addAttribute("errorMessage", "Please select a report type (PDF or Excel)");
                model.addAttribute("statuses", ReconciliationStatus.values());
                model.addAttribute("modules", getAvailableModules());
                model.addAttribute("users", getAvailableUsers());
                return "report/reports";
            }

            model.addAttribute("successMessage", "Report generated successfully with " + results.size() + " records!");
            model.addAttribute("statuses", ReconciliationStatus.values());
            model.addAttribute("modules", getAvailableModules());
            model.addAttribute("users", getAvailableUsers());
            model.addAttribute("reportGenerated", true);

            return "report/reports";

        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to generate report: " + e.getMessage());
            model.addAttribute("filter", filter);
            model.addAttribute("statuses", ReconciliationStatus.values());
            model.addAttribute("modules", getAvailableModules());
            model.addAttribute("users", getAvailableUsers());
            model.addAttribute("totalResults", reconciliationService.getAllResults().size());
            return "report/reports";
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReport(
            HttpSession session) {

        try {
            // Retrieve report data from session

            byte[] reportData = (byte[]) session.getAttribute(SESSION_REPORT_DATA);
            String fileName = (String) session.getAttribute(SESSION_REPORT_NAME);
            String reportType = (String) session.getAttribute(SESSION_REPORT_TYPE);

            if (reportData == null || reportData.length == 0) {
                log.error("No report data found in session");
                return ResponseEntity.badRequest().body("No report data available".getBytes());
            }

            // Clear session attributes after download
            session.removeAttribute(SESSION_REPORT_DATA);
            session.removeAttribute(SESSION_REPORT_NAME);
            session.removeAttribute(SESSION_REPORT_TYPE);


            log.info("Downloading report: {} type: {}, size: {} bytes", fileName, reportType, reportData.length);

            MediaType mediaType = "PDF".equalsIgnoreCase(reportType)
                    ? MediaType.APPLICATION_PDF
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(mediaType)
                    .body(reportData);

        } catch (Exception e) {
            log.error("Error downloading report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error downloading report".getBytes());
        }
    }

    private List<ReconciliationResult> getFilteredResults(ReportFilterDto filter) {
        List<ReconciliationResult> results = reconciliationService.getAllResults();

        // Status filter
        if (filter.getStatus() != null && !filter.getStatus().isEmpty() && !"ALL".equalsIgnoreCase(filter.getStatus())) {
            try {
                ReconciliationStatus status = ReconciliationStatus.valueOf(filter.getStatus());
                results = results.stream()
                        .filter(r -> r.getStatus() == status)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException ignored) {}
        }

        // User filter
        if (filter.getReconciledBy() != null && !filter.getReconciledBy().isEmpty() && !"ALL".equalsIgnoreCase(filter.getReconciledBy())) {
            results = results.stream()
                    .filter(r -> filter.getReconciledBy().equals(r.getReconciledBy()))
                    .collect(Collectors.toList());
        }

        if (filter.getDateFrom() != null) {
            results = results.stream()
                    .filter(r -> r.getReconciledAt() != null &&
                            r.getReconciledAt().toLocalDate().isAfter(filter.getDateFrom().minusDays(1)))
                    .collect(Collectors.toList());
        }

        if (filter.getDateTo() != null) {
            results = results.stream()
                    .filter(r -> r.getReconciledAt() != null &&
                            r.getReconciledAt().toLocalDate().isBefore(filter.getDateTo().plusDays(1)))
                    .collect(Collectors.toList());
        }

        return results;
    }

    private String generateFileName(String extension, ReportFilterDto filter) {
        String prefix = "reconciliation-report";
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return prefix + "-" + date + "." + extension;
    }

    private List<String> getAvailableModules() {
        List<String> modules = new ArrayList<>();
        modules.add("ALL");
        modules.add("PAYROLL");
        modules.add("RECEIPT");
        modules.add("PURCHASE");
        modules.add("ACCOUNTING");
        return modules;
    }

    private List<String> getAvailableUsers() {
        List<String> users = new ArrayList<>();
        users.add("ALL");
        users.add("admin");
        users.add("reviewer");
        users.add("supervisor");
        return users;
    }
}