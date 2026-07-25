package com.frwss.system.reporting.service;

import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reporting.dto.ReportFilterDto;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating Excel reports using Apache POI.
 * Creates professional Excel reports with multiple sheets:
 * - Summary: Overview and metadata
 * - Details: All reconciliation records
 * - Statistics: Status distribution with percentages
 */
@Service
public class ExcelReportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelReportService.class);

    /**
     * Generates an Excel report with three sheets:
     * 1. Summary - Report metadata and summary statistics
     * 2. Details - All reconciliation records in a table
     * 3. Statistics - Status distribution with counts and percentages
     *
     * @param results List of reconciliation results to include
     * @param filter  The filter criteria used for the report
     * @return byte array of the Excel file
     */
    public byte[] generateReconciliationReport(List<ReconciliationResult> results, ReportFilterDto filter) {
        log.info("Generating Excel report for {} reconciliation results", results.size());

        // Using try-with-resources to automatically close the workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {

            // ==========================================
            // Create three separate sheets
            // ==========================================

            // 1. Summary Sheet - Overview and metadata
            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(workbook, summarySheet, results, filter);

            // 2. Details Sheet - All records in a table
            Sheet detailsSheet = workbook.createSheet("Details");
            createDetailsSheet(workbook, detailsSheet, results);

            // 3. Statistics Sheet - Status distribution
            Sheet statsSheet = workbook.createSheet("Statistics");
            createStatisticsSheet(workbook, statsSheet, results);

            // Write workbook to byte array for download
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Excel report generated successfully. Size: {} bytes", out.size());

            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generating Excel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    /**
     * Creates the Summary sheet with:
     * - Report title
     * - Filter metadata (report type, status, module, etc.)
     * - Summary statistics (totals, counts per status)
     */
    private void createSummarySheet(XSSFWorkbook workbook, Sheet sheet,
                                    List<ReconciliationResult> results, ReportFilterDto filter) {
        int rowNum = 0;

        // ==========================================
        // TITLE
        // ==========================================
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("FRWSS - Reconciliation Report");

        // Style the title: Bold, larger font
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // Merge title across 4 columns
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));

        rowNum += 2; // Add some spacing

        // ==========================================
        // REPORT METADATA (Filters applied)
        // ==========================================
        addReportMetadata(sheet, rowNum, filter);
        rowNum += 8; // Space for metadata rows

        // ==========================================
        // SUMMARY STATISTICS
        // ==========================================
        addSummaryStatistics(workbook, sheet, rowNum, results);
        rowNum += 12; // Space for statistics

        // ==========================================
        // FOOTER - Generation timestamp
        // ==========================================
        Row footerRow = sheet.createRow(rowNum);
        Cell footerCell = footerRow.createCell(0);
        footerCell.setCellValue("Generated on: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        CellStyle footerStyle = workbook.createCellStyle();
        Font footerFont = workbook.createFont();
        footerFont.setItalic(true);
        footerFont.setFontHeightInPoints((short) 10);
        footerStyle.setFont(footerFont);
        footerCell.setCellStyle(footerStyle);

        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum, rowNum, 0, 3));

        // Auto-size columns for better readability
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Helper method to add metadata rows to the summary sheet
     */
    private void addReportMetadata(Sheet sheet, int startRow, ReportFilterDto filter) {
        int rowNum = startRow;

        // Create a bold style for labels
        CellStyle labelStyle = sheet.getWorkbook().createCellStyle();
        Font labelFont = sheet.getWorkbook().createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);

        // Add each metadata field as a row
        addMetaRow(sheet, rowNum++, "Report Type:", filter.getReportType() != null ? filter.getReportType() : "All", labelStyle);
        addMetaRow(sheet, rowNum++, "Status Filter:", filter.getStatus() != null ? filter.getStatus() : "All", labelStyle);
        addMetaRow(sheet, rowNum++, "Module Filter:", filter.getModule() != null ? filter.getModule() : "All", labelStyle);
        addMetaRow(sheet, rowNum++, "Reconciled By:", filter.getReconciledBy() != null ? filter.getReconciledBy() : "All", labelStyle);
        addMetaRow(sheet, rowNum++, "Date From:", filter.getDateFrom() != null ? filter.getDateFrom().toString() : "All", labelStyle);
        addMetaRow(sheet, rowNum++, "Date To:", filter.getDateTo() != null ? filter.getDateTo().toString() : "All", labelStyle);
    }

    /**
     * Helper method to add a single metadata row
     */
    private void addMetaRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
    }

    /**
     * Adds summary statistics to the summary sheet
     */
    private void addSummaryStatistics(XSSFWorkbook workbook, Sheet sheet,
                                      int startRow, List<ReconciliationResult> results) {
        int rowNum = startRow;

        // ==========================================
        // STATISTICS HEADER
        // ==========================================
        Row headerRow = sheet.createRow(rowNum++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Summary Statistics");

        // Style the header
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerCell.setCellStyle(headerStyle);

        // Merge header across 2 columns
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRow, startRow, 0, 1));

        // ==========================================
        // STATISTICS DATA
        // ==========================================
        // Calculate all statistics
        long total = results.size();
        long exact = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.EXACT).count();
        long partial = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.PARTIAL).count();
        long orphan = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.ORPHAN).count();
        long manuallyResolved = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.MANUALLY_RESOLVED).count();
        long pendingApproval = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.PENDING_APPROVAL).count();
        long approved = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.APPROVED).count();
        long rejected = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.REJECTED).count();

        // Create bold style for labels
        CellStyle labelStyle = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);

        // Add each statistic as a row
        addStatRow(sheet, rowNum++, "Total Records", String.valueOf(total), labelStyle);
        addStatRow(sheet, rowNum++, "Exact Matches", String.valueOf(exact), labelStyle);
        addStatRow(sheet, rowNum++, "Partial Matches", String.valueOf(partial), labelStyle);
        addStatRow(sheet, rowNum++, "Orphan Records", String.valueOf(orphan), labelStyle);
        addStatRow(sheet, rowNum++, "Manually Resolved", String.valueOf(manuallyResolved), labelStyle);
        addStatRow(sheet, rowNum++, "Pending Approval", String.valueOf(pendingApproval), labelStyle);
        addStatRow(sheet, rowNum++, "Approved", String.valueOf(approved), labelStyle);
        addStatRow(sheet, rowNum++, "Rejected", String.valueOf(rejected), labelStyle);
    }

    /**
     * Helper method to add a statistics row
     */
    private void addStatRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
    }

    /**
     * Creates the Details sheet with all reconciliation records
     * This is a comprehensive table with all fields
     */
    private void createDetailsSheet(XSSFWorkbook workbook, Sheet sheet,
                                    List<ReconciliationResult> results) {
        int rowNum = 0;

        // ==========================================
        // HEADERS
        // ==========================================
        String[] headers = {"ID", "Accounting Record", "Matched Record",
                "Source", "Status", "Confidence", "Reconciled By",
                "Reconciled At", "Remarks"};

        // Create header row with styling
        Row headerRow = sheet.createRow(rowNum++);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Apply grey background to headers
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create all header cells
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // ==========================================
        // DATA ROWS
        // ==========================================
        for (ReconciliationResult result : results) {
            Row row = sheet.createRow(rowNum++);

            // Fill each cell with data
            row.createCell(0).setCellValue(result.getId());
            row.createCell(1).setCellValue(result.getAccountingRecordId());
            row.createCell(2).setCellValue(result.getMatchedRecordId() != null ? result.getMatchedRecordId() : "-");
            row.createCell(3).setCellValue(result.getMatchedSourceType() != null ? result.getMatchedSourceType().toString() : "NONE");
            row.createCell(4).setCellValue(result.getStatus().toString());
            row.createCell(5).setCellValue(result.getConfidenceScore() != null ? result.getConfidenceScore() + "%" : "0%");
            row.createCell(6).setCellValue(result.getReconciledBy() != null ? result.getReconciledBy() : "-");
            row.createCell(7).setCellValue(result.getReconciledAt() != null ? result.getReconciledAt().toString() : "-");
            row.createCell(8).setCellValue(result.getRemarks() != null ? result.getRemarks() : "-");
        }

        // ==========================================
        // AUTO-SIZE COLUMNS
        // ==========================================
        // Makes columns wide enough to fit content
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Creates the Statistics sheet with status distribution
     * Includes counts and percentages for each status type
     */
    private void createStatisticsSheet(XSSFWorkbook workbook, Sheet sheet,
                                       List<ReconciliationResult> results) {
        int rowNum = 0;

        // ==========================================
        // HEADERS
        // ==========================================
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.createCell(0).setCellValue("Status");
        headerRow.createCell(1).setCellValue("Count");
        headerRow.createCell(2).setCellValue("Percentage");

        // Style the headers
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < 3; i++) {
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        // ==========================================
        // DATA - One row per status
        // ==========================================
        long total = results.size();

        // Loop through all possible statuses
        ReconciliationStatus[] statuses = ReconciliationStatus.values();

        for (ReconciliationStatus status : statuses) {
            long count = results.stream()
                    .filter(r -> r.getStatus() == status)
                    .count();

            // Calculate percentage (avoid division by zero)
            double percentage = total > 0 ? (count * 100.0 / total) : 0;

            // Create a row for this status
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(status.toString());
            row.createCell(1).setCellValue(count);
            row.createCell(2).setCellValue(String.format("%.1f%%", percentage));
        }

        // ==========================================
        // AUTO-SIZE COLUMNS
        // ==========================================
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}