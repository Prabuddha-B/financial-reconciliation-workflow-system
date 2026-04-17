package com.frwss.system.service;

import com.frwss.system.model.FinancialRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private FinancialRecordRepository repository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private StockPurchaseRepository stockPurchaseRepository;

    @Autowired
    private AccountingRecordRepository accountingRecordRepository;

    // Financial Record Logic (Existing & Working)
    public void processCSV(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            List<FinancialRecord> recordsToSave = new ArrayList<>();

            for (CSVRecord csvRecord : csvParser) {
                String refId = csvRecord.get("Reference ID");
                Double amount = Double.parseDouble(csvRecord.get("Amount"));
                LocalDate date = LocalDate.parse(csvRecord.get("Date"));

                if (!repository.existsByReferenceIdAndAmountAndDate(refId, amount, date)) {
                    FinancialRecord record = new FinancialRecord();
                    record.setReferenceId(refId);
                    record.setAmount(amount);
                    record.setDate(date);
                    recordsToSave.add(record);
                }
            }
            repository.saveAll(recordsToSave);
        }
    }

    public void savePayrollCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader,
                     CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .setIgnoreHeaderCase(true)
                             .setTrim(true)
                             .build())) {

            List<Payroll> payrollList = new ArrayList<>();

            for (CSVRecord record : csvParser) {

                String pId = record.get("payroll_id");

                // Avoid duplicates (IMPORTANT since ID is manual)
                if (!payrollRepository.existsById(pId)) {

                    Payroll payroll = new Payroll();

                    payroll.setPayrollId(pId); // Now valid

                    payroll.setEmployeeId(record.get("employee_id"));
                    payroll.setEmployeeName(record.get("employee_name"));
                    payroll.setSalary(new BigDecimal(record.get("salary")));

                    //  Payment Date
                    payroll.setPaymentDate(LocalDate.parse(record.get("payment_date")));

                    payroll.setReferenceNo(record.get("reference_no"));
                    payroll.setStatus(record.get("status"));

                    // Handle created_at safely
                    String createdAtRaw = record.get("created_at").replace("Z", "");
                    if (createdAtRaw.length() > 19) {
                        createdAtRaw = createdAtRaw.substring(0, 19);
                    }
                    payroll.setCreatedAt(LocalDateTime.parse(createdAtRaw));

                    payroll.setEnteredBy(record.get("entered_by"));

                    payrollList.add(payroll);
                }
            }

            if (!payrollList.isEmpty()) {
                payrollRepository.saveAll(payrollList);
                System.out.println("Saved " + payrollList.size() + " payroll records!");
            } else {
                System.out.println("No new payroll records (duplicates skipped).");
            }
        }
    }

    public void saveReceiptCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            List<Receipt> receiptsToSave = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                String rId = record.get("receipt_id");

                // Only process if the ID is unique to the database
                if (!receiptRepository.existsById(rId)) {
                    Receipt receipt = new Receipt();
                    receipt.setReceiptId(rId);
                    receipt.setReferenceNo(record.get("reference_no"));
                    receipt.setPayerName(record.get("payer_name"));
                    receipt.setAmount(new BigDecimal(record.get("amount")));
                    receipt.setReceiptDate(LocalDate.parse(record.get("receipt_date")));
                    receipt.setEnteredBy(record.get("entered_by"));

                    // Handling ISO 8601 formatting from Mockaroo
                    String rawCreated = record.get("created_at").replace("Z", "");
                    receipt.setCreatedAt(LocalDateTime.parse(rawCreated.substring(0, 19)));

                    receiptsToSave.add(receipt);
                }
            }

            if (!receiptsToSave.isEmpty()) {
                receiptRepository.saveAll(receiptsToSave);
                System.out.println("Successfully saved " + receiptsToSave.size() + " receipts!");
            }
        }
    }

    public void saveStockPurchaseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            List<StockPurchase> purchasesToSave = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                String pId = record.get("purchase_id");

                if (!stockPurchaseRepository.existsById(pId)) {
                    StockPurchase purchase = new StockPurchase();
                    purchase.setPurchaseId(pId);
                    purchase.setVendorName(record.get("vendor_name"));
                    purchase.setInvoiceNo(record.get("invoice_no"));
                    purchase.setAmount(new BigDecimal(record.get("amount")));
                    purchase.setPurchaseDate(LocalDate.parse(record.get("purchase_date")));
                    purchase.setEnteredBy(record.get("entered_by"));

                    String rawCreated = record.get("created_at").replace("Z", "");
                    purchase.setCreatedAt(LocalDateTime.parse(rawCreated.substring(0, 19)));

                    purchasesToSave.add(purchase);
                }
            }
            stockPurchaseRepository.saveAll(purchasesToSave);
        }
    }



    public void saveAccountingRecordCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            List<AccountingRecord> recordsToSave = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                String rId = record.get("record_id");

                if (!accountingRecordRepository.existsById(rId)) {
                    AccountingRecord accRecord = new AccountingRecord();
                    accRecord.setRecordId(rId);
                    accRecord.setReferenceNo(record.get("reference_no"));
                    accRecord.setCategory(record.get("category"));
                    accRecord.setAmount(new BigDecimal(record.get("amount")));
                    accRecord.setRecordDate(LocalDate.parse(record.get("record_date")));
                    accRecord.setEnteredBy(record.get("entered_by"));

                    // Parsing ISO timestamp
                    String rawCreated = record.get("created_at").replace("Z", "");
                    accRecord.setCreatedAt(LocalDateTime.parse(rawCreated.substring(0, 19)));

                    recordsToSave.add(accRecord);
                }
            }
            accountingRecordRepository.saveAll(recordsToSave);
        }
    }
}