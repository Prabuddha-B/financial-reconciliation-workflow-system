package com.frwss.system.service;

import com.frwss.system.model.FinancialRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.repository.FinancialRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
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
    private FinancialRecordRepository financialRecordRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    // =========================================================
    // 1. FINANCIAL RECORD INGESTION (DATABASE)
    // =========================================================
    public void processCSV(MultipartFile file) throws Exception {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader,
                     CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .setIgnoreHeaderCase(true)
                             .setTrim(true)
                             .build())) {

            List<FinancialRecord> recordsToSave = new ArrayList<>();

            for (CSVRecord csvRecord : csvParser) {

                String refId = csvRecord.get("Reference ID");
                Double amount = Double.parseDouble(csvRecord.get("Amount"));
                LocalDate date = LocalDate.parse(csvRecord.get("Date"));

                if (!financialRecordRepository.existsByReferenceIdAndAmountAndDate(refId, amount, date)) {

                    FinancialRecord record = new FinancialRecord();
                    record.setReferenceId(refId);
                    record.setAmount(amount);
                    record.setDate(date);

                    recordsToSave.add(record);
                }
            }

            financialRecordRepository.saveAll(recordsToSave);
        }
    }

    // =========================================================
    // 2. PAYROLL INGESTION (DATABASE)
    // =========================================================
    public void savePayrollCsv(MultipartFile file) throws Exception {

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

                if (!payrollRepository.existsById(pId)) {

                    Payroll payroll = new Payroll();

                    payroll.setPayrollId(pId);
                    payroll.setEmployeeId(record.get("employee_id"));
                    payroll.setEmployeeName(record.get("employee_name"));
                    payroll.setSalary(new BigDecimal(record.get("salary")));
                    payroll.setPaymentDate(LocalDate.parse(record.get("payment_date")));
                    payroll.setReferenceNo(record.get("reference_no"));
                    payroll.setStatus(record.get("status"));

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
            }
        }
    }

    // =========================================================
    // 3. FILE VALIDATION + PREVIEW (UI FEATURE - HIS IDEA)
    // =========================================================
    public List<Receipt> processFile(MultipartFile file) {

        List<Receipt> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();

            if (fileName == null) return records;

            // ================= CSV =================
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream())
                );

                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {

                    if (line.trim().isEmpty()) continue;

                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    String[] data = line.split(",");

                    Receipt receipt = new Receipt(
                            Integer.parseInt(data[0]),
                            data[1],
                            data[2],
                            Double.parseDouble(data[3]),
                            data[4],
                            data[5]
                    );

                    validate(receipt);
                    records.add(receipt);
                }
            }

            // ================= XLSX =================
            else if (fileName.endsWith(".xlsx")) {

                XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
                XSSFSheet sheet = workbook.getSheetAt(0);

                boolean isFirstRow = true;

                for (var row : sheet) {

                    if (isFirstRow) {
                        isFirstRow = false;
                        continue;
                    }

                    try {
                        Receipt receipt = new Receipt(
                                (int) row.getCell(0).getNumericCellValue(),
                                row.getCell(1).toString(),
                                row.getCell(2).toString(),
                                row.getCell(3).getNumericCellValue(),
                                row.getCell(4).toString(),
                                row.getCell(5).toString()
                        );

                        validate(receipt);
                        records.add(receipt);

                    } catch (Exception e) {
                        System.out.println("Row error: " + e.getMessage());
                    }
                }

                workbook.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }

    // =========================================================
    // 4. VALIDATION (UI SUPPORT)
    // =========================================================
    private void validate(Receipt receipt) {

        if (receipt.getAmount() <= 0) {
            receipt.setValid(false);
            receipt.setErrorMessage("Invalid amount");
        }

        if (receipt.getReferenceNo() == null || receipt.getReferenceNo().isEmpty()) {
            receipt.setValid(false);
            receipt.setErrorMessage("Missing reference");
        }
    }
}