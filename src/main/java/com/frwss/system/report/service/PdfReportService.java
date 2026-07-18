package com.frwss.system.report.service;

import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.report.dto.ReportFilterDto;
import com.lowagie.text.*;

import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfReportService {
    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    public byte[] generateReconciliationReport(List<ReconciliationResult > results, ReportFilterDto filter){
        log.info("Generating PDF report for {} reconciliation results", results.size());

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Add Title
            addTitle(document, filter);

            // 2. Add Metadata
            addMetadata(document, filter);

            // 3. Add Summary
            addSummary(document, results);

            // 4. Add Results Table
            addResultsTable(document, results);

            // 5. Add Footer
            addFooter(document);

            document.close();
            log.info("PDF report generated successfully. Size: {} bytes", out.size());

            return out.toByteArray();

        } catch (Exception e){
            log.error("Error generating PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addTitle(Document document, ReportFilterDto filter) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("FRWSS - Reconciliation Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font subtitleFont = new Font(Font.HELVETICA, 12, Font.ITALIC);
        Paragraph subtitle = new Paragraph(
                "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                subtitleFont
        );
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
    }

    private void addMetadata(Document document, ReportFilterDto filter) throws DocumentException{
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10);

        Paragraph section = new Paragraph("Report Details", sectionFont);

        document.add(section);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);

        addMetaRow(metaTable, "Report Type", filter.getReportType() != null ? filter.getReportType() : "All");
        addMetaRow(metaTable, "Status Filter", filter.getStatus() != null ? filter.getStatus() : "All");
        addMetaRow(metaTable, "Module Filter", filter.getModule() != null ? filter.getModule() : "All");
        addMetaRow(metaTable, "Reconciled By", filter.getReconciledBy() != null ? filter.getReconciledBy() : "All");
        addMetaRow(metaTable, "Date From", filter.getDateFrom() != null ? filter.getDateFrom().toString() : "All");
        addMetaRow(metaTable, "Date To", filter.getDateTo() != null ? filter.getDateTo().toString() : "All");

        document.add(metaTable);
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
    }

    private void addMetaRow(PdfPTable table, String label, String value){
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", new Font(Font.HELVETICA, 10, Font.BOLD)));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 10)));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSummary(Document document, List<ReconciliationResult> results) throws DocumentException{
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);

        Paragraph section = new Paragraph("Summary Statistics", sectionFont);
        document.add(section);

        long total = results.size();
        long exact = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.EXACT).count();
        long partial = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.PARTIAL).count();
        long orphan = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.ORPHAN).count();
        long manuallyResolved = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.MANUALLY_RESOLVED).count();
        long pendingApproval = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.PENDING_APPROVAL).count();
        long approved = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.APPROVED).count();
        long rejected = results.stream().filter(r -> r.getStatus() == ReconciliationStatus.REJECTED).count();

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(60);
        summaryTable.setHorizontalAlignment(Element.ALIGN_CENTER);

        addSummaryRow(summaryTable, "Total Records", String.valueOf(total));
        addSummaryRow(summaryTable, "Exact Matches", String.valueOf(exact));
        addSummaryRow(summaryTable, "Partial Matches", String.valueOf(partial));
        addSummaryRow(summaryTable, "Orphan Records", String.valueOf(orphan));
        addSummaryRow(summaryTable, "Manually Resolved", String.valueOf(manuallyResolved));
        addSummaryRow(summaryTable, "Pending Approval", String.valueOf(pendingApproval));
        addSummaryRow(summaryTable, "Approved", String.valueOf(approved));
        addSummaryRow(summaryTable, "Rejected", String.valueOf(rejected));

        document.add(summaryTable);
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
    }

    private void addSummaryRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, new Font(Font.HELVETICA, 10)));
        labelCell.setPadding(4);
        labelCell.setBackgroundColor(new Color(240, 240, 240));

        PdfPCell valueCell = new PdfPCell(new Phrase(value, new Font(Font.HELVETICA, 10)));
        valueCell.setPadding(4);
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addResultsTable(Document document, List<ReconciliationResult> results) throws DocumentException{
        Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD);

        Paragraph section = new Paragraph("Reconciliation Details", sectionFont);
        document.add(section);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 2, 2, 2, 2, 2, 2, 3});

        // Headers
        String[] headers = {"ID", "Accounting Record", "Matched Record", "Source", "Status", "Confidence", "Reconciled By", "Remarks"};
        for (String header : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(header, new Font(Font.HELVETICA, 10, Font.BOLD)));
            headerCell.setBackgroundColor(new Color(200, 200, 200));
            headerCell.setPadding(5);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(headerCell);
        }

        int rowCount = 0;

        for(ReconciliationResult result : results){
            if(rowCount >= 50) break;

            table.addCell(new Phrase(String.valueOf(result.getId()), new Font(Font.HELVETICA, 9)));
            table.addCell(new Phrase(result.getAccountingRecordId(), new Font(Font.HELVETICA, 9)));
            table.addCell(new Phrase(result.getMatchedRecordId() != null ? result.getMatchedRecordId() : "-", new Font(Font.HELVETICA, 9)));
            table.addCell(new Phrase(result.getMatchedSourceType() != null ? result.getMatchedSourceType().toString() : "NONE", new Font(Font.HELVETICA, 9)));
            table.addCell(new Phrase(result.getStatus().toString(), new Font(Font.HELVETICA, 9)));
            table.addCell(new Phrase(result.getConfidenceScore() != null ? result.getConfidenceScore() + "%" : "0%", new Font(Font.HELVETICA, 9)));
            table.addCell(new Phrase(result.getReconciledBy() != null ? result.getReconciledBy() : "-", new Font(Font.HELVETICA, 9)));

            String remarks = result.getRemarks();
            if (remarks != null && remarks.length() > 50) {
                remarks = remarks.substring(0, 47) + "...";
            }
            table.addCell(new Phrase(remarks != null ? remarks : "-", new Font(Font.HELVETICA, 9)));

            rowCount++;
        }

        document.add(table);

        if (results.size() > 50) {
            Font noteFont = new Font(Font.HELVETICA, 10, Font.ITALIC);
            Paragraph note = new Paragraph(
                    "Note: Showing only first 50 records. Total records: " + results.size(),
                    noteFont
            );
            note.setAlignment(Element.ALIGN_RIGHT);
            document.add(note);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC);
        Paragraph footer = new Paragraph(
                "FRWSS - Financial Reconciliation & Workflow Support System v1.0 | Report generated automatically",
                footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph pageInfo = new Paragraph(
                "This report is for internal use only. Generated on: " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                footerFont
        );
        pageInfo.setAlignment(Element.ALIGN_CENTER);
        document.add(pageInfo);
    }
}
