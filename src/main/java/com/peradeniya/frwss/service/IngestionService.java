package com.peradeniya.frwss.service;

import com.peradeniya.frwss.model.FinancialRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {

    @Autowired
    private FinancialRecordRepository repository;

    // Date formatter for your MOCK_DATA (M/d/yyyy)
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");

    public void processCSV(MultipartFile file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            List<FinancialRecord> recordsToSave = new ArrayList<>();

            for (CSVRecord csvRecord : csvParser) {
                // Updated to match your CSV headers exactly
                String refId = csvRecord.get("Reference ID");
                Double amount = Double.parseDouble(csvRecord.get("Amount"));

                // Standard CSV date format is YYYY-MM-DD, so we don't need a custom formatter here
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
}