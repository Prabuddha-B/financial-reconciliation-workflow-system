package com.frwss.system.service;

import com.frwss.system.model.*;
import com.frwss.system.repository.FinancialRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import org.apache.commons.csv.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private final FinancialRecordRepository financialRecordRepository;
    private final PayrollRepository payrollRepository;

    public IngestionService(FinancialRecordRepository financialRecordRepository,
                            PayrollRepository payrollRepository) {
        this.financialRecordRepository = financialRecordRepository;
        this.payrollRepository = payrollRepository;
    }

    // =========================================================
    // ENTRY POINT
    // =========================================================
    public IngestionResult processFile(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();

            if (fileName == null || file.isEmpty()) {
                return IngestionResult.receiptResult(List.of(), 0, 0);
            }

            if (fileName.toLowerCase().endsWith(".csv")) {
                if (isPayrollFile(file)) {
                    return savePayrollCsv(file);
                } else {
                    return processReceiptCsv(file);
                }
            }

            if (fileName.toLowerCase().endsWith(".xlsx")) {
                return previewWorkbook(file.getInputStream());
            }

            throw new IllegalArgumentException("Unsupported file format");

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to process uploaded file", e);
        }
    }

    // =========================================================
    // PAYROLL DB INGESTION (FIXED)
    // =========================================================
    public IngestionResult savePayrollCsv(MultipartFile file) throws Exception {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            List<Payroll> payrollList = new ArrayList<>();
            List<Receipt> preview = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            Set<String> toReplace = new HashSet<>();

            int row = 1;

            for (CSVRecord record : csvParser) {

                String payrollId = record.get("payroll_id").trim();

                Receipt uiRow = new Receipt(
                        row++,
                        payrollId,
                        record.get("employee_name"),
                        parseDouble(record.get("salary"), "salary"),
                        record.get("payment_date"),
                        record.get("entered_by")
                );

                // duplicate inside file
                if (!seen.add(payrollId)) {
                    uiRow.setValid(false);
                    uiRow.setErrorMessage("Duplicate payroll_id in file");
                    preview.add(uiRow);
                    continue;
                }

                // existing DB record
                if (payrollRepository.existsById(payrollId)) {
                    toReplace.add(payrollId);
                    uiRow.setErrorMessage("Will replace existing record");
                }

                Payroll p = new Payroll();
                p.setPayrollId(payrollId);
                p.setEmployeeId(record.get("employee_id"));
                p.setEmployeeName(record.get("employee_name"));
                p.setSalary(new BigDecimal(record.get("salary")));
                p.setPaymentDate(LocalDate.parse(record.get("payment_date")));
                p.setReferenceNo(record.get("reference_no"));
                p.setStatus(record.get("status"));

                String createdAt = record.get("created_at").replace("Z", "");
                if (createdAt.length() > 19) {
                    createdAt = createdAt.substring(0, 19);
                }
                p.setCreatedAt(LocalDateTime.parse(createdAt));

                p.setEnteredBy(record.get("entered_by"));

                payrollList.add(p);
                preview.add(uiRow);
            }

            if (!toReplace.isEmpty()) {
                payrollRepository.deleteAllByIdInBatch(toReplace);
            }

            if (!payrollList.isEmpty()) {
                payrollRepository.saveAll(payrollList);
            }

            return IngestionResult.payrollResult(
                    preview,
                    payrollList.size(),
                    0,
                    toReplace.size()
            );
        }
    }

    // =========================================================
    // RECEIPT CSV (UI ONLY)
    // =========================================================
    private IngestionResult processReceiptCsv(MultipartFile file) throws Exception {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = createCsvParser(reader)) {

            List<Receipt> records = new ArrayList<>();

            for (CSVRecord r : csvParser) {
                Receipt receipt = new Receipt(
                        parseInt(r.get(0), "receiptId"),
                        r.get(1),
                        r.get(2),
                        parseDouble(r.get(3), "amount"),
                        r.get(4),
                        r.get(5)
                );

                validate(receipt);
                records.add(receipt);
            }

            return summarize(records);
        }
    }

    // =========================================================
    // EXCEL PREVIEW (UI ONLY)
    // =========================================================
    private IngestionResult previewWorkbook(InputStream inputStream) throws Exception {

        List<Receipt> records = new ArrayList<>();
        DataFormatter df = new DataFormatter();

        try (XSSFWorkbook wb = new XSSFWorkbook(inputStream)) {
            XSSFSheet sheet = wb.getSheetAt(0);
            boolean first = true;

            for (Row row : sheet) {
                if (first) { first = false; continue; }
                if (isRowEmpty(row)) continue;

                Receipt r = new Receipt(
                        parseInt(readCell(row, 0, df), "receiptId"),
                        readCell(row, 1, df),
                        readCell(row, 2, df),
                        parseDouble(readCell(row, 3, df), "amount"),
                        readCell(row, 4, df),
                        readCell(row, 5, df)
                );

                validate(r);
                records.add(r);
            }
        }

        return summarize(records);
    }

    // =========================================================
    // PAYROLL DETECTION (FIXED - SAFE)
    // =========================================================
    private boolean isPayrollFile(MultipartFile file) throws Exception {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            Set<String> headers = parser.getHeaderMap().keySet().stream()
                    .map(h -> h.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            return headers.contains("payroll_id")
                    && headers.contains("employee_id")
                    && headers.contains("payment_date");
        }
    }

    // =========================================================
    // VALIDATION
    // =========================================================
    private void validate(Receipt r) {
        if (r.getAmount() <= 0) {
            r.setValid(false);
            r.setErrorMessage("Invalid amount");
        }

        if (r.getReferenceNo() == null || r.getReferenceNo().isEmpty()) {
            r.setValid(false);
            r.setErrorMessage("Missing reference");
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private IngestionResult summarize(List<Receipt> records) {
        int valid = 0, invalid = 0;

        for (Receipt r : records) {
            if (r.isValid()) valid++;
            else invalid++;
        }

        return IngestionResult.receiptResult(records, valid, invalid);
    }

    private CSVParser createCsvParser(BufferedReader reader) throws Exception {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build()
                .parse(reader);
    }

    private String readCell(Row row, int i, DataFormatter df) {
        Cell c = row.getCell(i);
        if (c == null) throw new IllegalArgumentException("Missing cell " + i);
        return df.formatCellValue(c).trim();
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < 6; i++) {
            Cell c = row.getCell(i);
            if (c != null && !c.toString().trim().isEmpty()) return false;
        }
        return true;
    }

    private int parseInt(String v, String f) {
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + f + ": " + v);
        }
    }

    private double parseDouble(String v, String f) {
        try {
            return Double.parseDouble(v.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + f + ": " + v);
        }
    }
}