package com.peradeniya.frwss.service;

import com.peradeniya.frwss.model.FinancialRecord;
import com.peradeniya.frwss.model.Payroll;
import com.peradeniya.frwss.repository.FinancialRecordRepository;
import com.peradeniya.frwss.repository.PayrollRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {

    @Autowired
    private FinancialRecordRepository repository;

    @Autowired
    private PayrollRepository payrollRepository;

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
}