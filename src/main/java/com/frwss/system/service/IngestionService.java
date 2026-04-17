package com.frwss.system.service;

import com.frwss.system.dto.*;
import com.frwss.system.model.*;
import com.frwss.system.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {

    private final ReceiptRepository receiptRepository;
    private final PayrollRepository payrollRepository;
    private final StockPurchaseRepository stockPurchaseRepository;
    private final AccountingRecordRepository accountingRecordRepository;

    public IngestionService(
            ReceiptRepository receiptRepository,
            PayrollRepository payrollRepository,
            StockPurchaseRepository stockPurchaseRepository,
            AccountingRecordRepository accountingRecordRepository
    ) {
        this.receiptRepository = receiptRepository;
        this.payrollRepository = payrollRepository;
        this.stockPurchaseRepository = stockPurchaseRepository;
        this.accountingRecordRepository = accountingRecordRepository;
    }

    // =========================================================
    // MAIN ROUTERS
    // =========================================================
    public IngestionResult processFileForPreview(MultipartFile file, String dataType) throws IOException {
        return switch (dataType.toUpperCase()) {
            case "RECEIPT" -> previewReceipts(file);
            case "PAYROLL" -> previewPayroll(file);
            case "STOCK" -> previewStock(file);
            case "ACCOUNTING" -> previewAccounting(file);
            default -> throw new IllegalArgumentException("Invalid data type: " + dataType);
        };
    }

    /**
     * This handles saving the LIST currently stored in the Session.
     */
    public void saveProcessedRecords(List<?> records, String dataType) {
        if (records == null || records.isEmpty()) return;

        switch (dataType.toUpperCase()) {
            case "RECEIPT":
                persistReceipts((List<ReceiptDto>) records);
                break;
            case "PAYROLL":
                persistPayroll((List<PayrollDto>) records);
                break;
            case "STOCK":
                persistStock((List<StockPurchase>) records);
                break;
            case "ACCOUNTING":
                persistAccounting((List<AccountingRecord>) records);
                break;
            default:
                throw new IllegalArgumentException("Unknown data type for saving: " + dataType);
        }
    }

    // =========================================================
    // PERSISTENCE LOGIC (Mapping DTO -> Entity)
    // =========================================================

    private void persistReceipts(List<ReceiptDto> dtos) {
        List<Receipt> entities = new ArrayList<>();
        for (ReceiptDto dto : dtos) {
            if (receiptRepository.existsById(dto.getReceiptId())) continue;

            Receipt entity = new Receipt();
            entity.setReceiptId(dto.getReceiptId());
            entity.setReferenceNo(dto.getReferenceNo());
            entity.setPayerName(dto.getPayerName());
            entity.setAmount(dto.getAmount());
            // If DTO doesn't have these, we use defaults or extend DTO
            entity.setReceiptDate(LocalDate.now());
            entity.setEnteredBy("SYSTEM_USER");
            entities.add(entity);
        }
        receiptRepository.saveAll(entities);
    }

    private void persistPayroll(List<PayrollDto> dtos) {
        List<Payroll> entities = new ArrayList<>();
        for (PayrollDto dto : dtos) {
            if (payrollRepository.existsById(dto.getPayrollId())) continue;

            Payroll p = new Payroll();
            p.setPayrollId(dto.getPayrollId());
            p.setEmployeeId(dto.getEmployeeId());
            p.setEmployeeName(dto.getEmployeeName());
            p.setSalary(dto.getSalary());
            p.setPaymentDate(dto.getPaymentDate());
            p.setReferenceNo(dto.getReferenceNo());
            p.setStatus(dto.getStatus());
            p.setCreatedAt(dto.getCreatedAt());
            p.setEnteredBy(dto.getEnteredBy());
            entities.add(p);
        }
        payrollRepository.saveAll(entities);
    }

    private void persistStock(List<StockPurchase> records) {
        records.removeIf(s -> stockPurchaseRepository.existsById(s.getPurchaseId()));
        stockPurchaseRepository.saveAll(records);
    }

    private void persistAccounting(List<AccountingRecord> records) {
        records.removeIf(a -> accountingRecordRepository.existsById(a.getRecordId()));
        accountingRecordRepository.saveAll(records);
    }

    // =========================================================
    // PREVIEW METHODS (Already mostly correct in your draft)
    // =========================================================

    private IngestionResult previewReceipts(MultipartFile file) throws IOException {
        List<ReceiptDto> preview = new ArrayList<>();
        int validCount = 0;
        try (BufferedReader reader = createReader(file); CSVParser parser = createParser(reader)) {
            for (CSVRecord r : parser) {
                ReceiptDto dto = new ReceiptDto(
                        r.get("receipt_id"),
                        r.get("reference_no"),
                        r.get("payer_name"),
                        new BigDecimal(r.get("amount")),
                        r.get("receipt_date"),
                        r.get("entered_by")
                );
                validateReceipt(dto);
                if (dto.isValid()) validCount++;
                preview.add(dto);
            }
        }
        return new IngestionResult(preview, preview.size(), validCount, preview.size() - validCount);
    }

    private IngestionResult previewPayroll(MultipartFile file) throws IOException {
        List<PayrollDto> preview = new ArrayList<>();
        int validCount = 0;
        try (BufferedReader reader = createReader(file); CSVParser parser = createParser(reader)) {
            for (CSVRecord r : parser) {
                PayrollDto dto = new PayrollDto(
                        r.get("payroll_id"),
                        r.get("employee_id"),
                        r.get("employee_name"),
                        new BigDecimal(r.get("salary")),
                        LocalDate.parse(r.get("payment_date")),
                        r.get("reference_no"),
                        r.get("status"),
                        parseDateTime(r.get("created_at")),
                        r.get("entered_by")
                );
                validatePayroll(dto);
                if (dto.isValid()) validCount++;
                preview.add(dto);
            }
        }
        return new IngestionResult(preview, preview.size(), validCount, preview.size() - validCount);
    }

    // (Stock and Accounting previews remain as they were in your code)
    private IngestionResult previewStock(MultipartFile file) throws IOException {
        List<StockPurchase> preview = new ArrayList<>();
        try (BufferedReader reader = createReader(file); CSVParser parser = createParser(reader)) {
            for (CSVRecord r : parser) {
                StockPurchase s = new StockPurchase();
                s.setPurchaseId(r.get("purchase_id"));
                s.setVendorName(r.get("vendor_name"));
                s.setInvoiceNo(r.get("invoice_no"));
                s.setAmount(new BigDecimal(r.get("amount")));
                s.setPurchaseDate(LocalDate.parse(r.get("purchase_date")));
                s.setEnteredBy(r.get("entered_by"));
                preview.add(s);
            }
        }
        return new IngestionResult(preview, preview.size(), preview.size(), 0);
    }

    private IngestionResult previewAccounting(MultipartFile file) throws IOException {
        List<AccountingRecord> preview = new ArrayList<>();
        try (BufferedReader reader = createReader(file); CSVParser parser = createParser(reader)) {
            for (CSVRecord r : parser) {
                AccountingRecord a = new AccountingRecord();
                a.setRecordId(r.get("record_id"));
                a.setReferenceNo(r.get("reference_no"));
                a.setModule(r.get("module"));
                a.setAmount(new BigDecimal(r.get("amount")));
                a.setRecordDate(LocalDate.parse(r.get("record_date")));
                a.setEnteredBy(r.get("entered_by"));
                preview.add(a);
            }
        }
        return new IngestionResult(preview, preview.size(), preview.size(), 0);
    }

    // =========================================================
    // HELPERS & VALIDATION
    // =========================================================

    private BufferedReader createReader(MultipartFile file) throws IOException {
        return new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
    }

    private CSVParser createParser(BufferedReader reader) throws IOException {
        return new CSVParser(reader, CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build());
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isEmpty()) return LocalDateTime.now();
        String cleaned = raw.replace("Z", "").trim();
        return LocalDateTime.parse(cleaned.substring(0, 19));
    }

    private void validateReceipt(ReceiptDto dto) {
        dto.setValid(dto.getAmount().compareTo(BigDecimal.ZERO) > 0
                && dto.getReferenceNo() != null && !dto.getReferenceNo().isEmpty());
    }

    private void validatePayroll(PayrollDto dto) {
        dto.setValid(dto.getSalary().compareTo(BigDecimal.ZERO) > 0
                && dto.getEmployeeName() != null && !dto.getEmployeeName().isEmpty());
    }
}