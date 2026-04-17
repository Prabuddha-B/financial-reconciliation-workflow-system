package com.frwss.system.service;

import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {

    public List<?> processFile(MultipartFile file, String dataType) {

        switch (dataType) {

            case "RECEIPT":
                return processReceiptFile(file);

            case "PAYROLL":
                return processPayrollFile(file);

            case "STOCK":
                return processStockFile(file);

            case "ACCOUNTING":
                return processAccountingFile(file);

            default:
                throw new RuntimeException("Invalid data type selected");
        }
    }


    public List<Receipt> processReceiptFile(MultipartFile file) {

        List<Receipt> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();

            // For CSV
            assert fileName != null;
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream())
                );

                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }

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

            // for XLSX
            else if (fileName.endsWith(".xlsx")) {

                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                        new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(0);

                boolean isFirstRow = true;

                for (org.apache.poi.ss.usermodel.Row row : sheet) {

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

    public List<Payroll> processPayrollFile(MultipartFile file) {

        List<Payroll> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();

            assert fileName != null;

            // For CSV
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream())
                );

                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {

                    lineNumber++;
                    line = line.replace("\uFEFF", "").trim();

                    if (line.isEmpty()) continue;
                    if (lineNumber == 1) continue;

                    String delimiter = line.contains("\t") ? "\t" : ",";
                    String[] data = line.split(delimiter);

                    if (data.length > 0 && isPayrollHeaderValue(data[0])) continue;
                    if (data.length < 9) continue;

                    Payroll payroll = new Payroll();

                    try {
                        payroll.setPayrollId(data[0]);
                        payroll.setEmployeeId(data[1]);
                        payroll.setEmployeeName(data[2]);
                        payroll.setSalary(Double.parseDouble(data[3]));
                        payroll.setPaymentDate(data[4]);
                        payroll.setReferenceNo(data[5]);
                        payroll.setStatus(data[6]);
                        payroll.setCreatedAt(data[7]);
                        payroll.setEnteredBy(data[8]);

                        validate(payroll);
                        records.add(payroll);

                    } catch (Exception e) {
                        payroll.setValid(false);
                        payroll.setErrorMessage("Parsing error");
                        records.add(payroll);
                    }
                }
            }
            // For XLSX
            else if (fileName.endsWith(".xlsx")) {

                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                        new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(0);

                for (org.apache.poi.ss.usermodel.Row row : sheet) {

                    if (row.getRowNum() == 0) continue;
                    if (row.getCell(0) == null) continue;

                    String firstCell = row.getCell(0).toString().trim();
                    if (firstCell.isEmpty() || isPayrollHeaderValue(firstCell)) continue;

                    Payroll payroll = new Payroll();

                    try {
                        payroll.setPayrollId(row.getCell(0).toString());
                        payroll.setEmployeeId(row.getCell(1).toString());
                        payroll.setEmployeeName(row.getCell(2).toString());
                        payroll.setSalary(row.getCell(3).getNumericCellValue());
                        payroll.setPaymentDate(row.getCell(4).toString());
                        payroll.setReferenceNo(row.getCell(5).toString());
                        payroll.setStatus(row.getCell(6).toString());
                        payroll.setCreatedAt(row.getCell(7).toString());
                        payroll.setEnteredBy(row.getCell(8).toString());

                        validate(payroll);
                        records.add(payroll);

                    } catch (Exception e) {
                        payroll.setValid(false);
                        payroll.setErrorMessage("Parsing error");
                        records.add(payroll);
                    }
                }

                workbook.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }

    public List<StockPurchase> processStockFile(MultipartFile file) {

        List<StockPurchase> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();
            assert fileName != null;

            // For CSV
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream())
                );

                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {

                    lineNumber++;

                    line = line.replace("\uFEFF", "").trim();

                    if (line.isEmpty()) continue;

                    // Skip first row (header)
                    if (lineNumber == 1) continue;

                    String delimiter = line.contains("\t") ? "\t" : ",";

                    String[] data = line.split(delimiter);

                    // Skip accidental header rows
                    if (data[0].equalsIgnoreCase("purchase_id")) continue;

                    if (data.length < 7) continue;

                    StockPurchase s = new StockPurchase();

                    try {
                        s.setPurchaseId(data[0]);
                        s.setVendorName(data[1]);
                        s.setInvoiceNo(data[2]);
                        s.setAmount(Double.parseDouble(data[3]));
                        s.setPurchaseDate(data[4]);
                        s.setEnteredBy(data[5]);
                        s.setCreatedAt(data[6]);

                        // VALIDATION
                        if (s.getVendorName() == null || s.getVendorName().isEmpty()) {
                            s.setValid(false);
                            s.setErrorMessage("Missing vendor name");
                        } else if (s.getAmount() <= 0) {
                            s.setValid(false);
                            s.setErrorMessage("Invalid amount");
                        } else {
                            s.setValid(true);
                        }

                        records.add(s);

                    } catch (Exception e) {
                        s.setValid(false);
                        s.setErrorMessage("Parsing error");
                        records.add(s);
                    }
                }
            }

            // For XLSX
            else if (fileName.endsWith(".xlsx")) {

                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                        new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(0);

                for (org.apache.poi.ss.usermodel.Row row : sheet) {

                    if (row.getRowNum() == 0) continue;

                    StockPurchase s = new StockPurchase();

                    try {
                        s.setPurchaseId(row.getCell(0).toString());
                        s.setVendorName(row.getCell(1).toString());
                        s.setInvoiceNo(row.getCell(2).toString());
                        s.setAmount(row.getCell(3).getNumericCellValue());
                        s.setPurchaseDate(row.getCell(4).toString());
                        s.setEnteredBy(row.getCell(5).toString());
                        s.setCreatedAt(row.getCell(6).toString());

                        if (s.getVendorName() == null || s.getVendorName().isEmpty()) {
                            s.setValid(false);
                            s.setErrorMessage("Missing vendor name");
                        } else if (s.getAmount() <= 0) {
                            s.setValid(false);
                            s.setErrorMessage("Invalid amount");
                        } else {
                            s.setValid(true);
                        }

                        records.add(s);

                    } catch (Exception e) {
                        s.setValid(false);
                        s.setErrorMessage("Parsing error");
                        records.add(s);
                    }
                }

                workbook.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }

    public List<AccountingRecord> processAccountingFile(MultipartFile file) {

        List<AccountingRecord> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();
            assert fileName != null;

            // For CSV
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream())
                );

                String line;
                int lineNumber = 0;

                while ((line = reader.readLine()) != null) {

                    lineNumber++;

                    line = line.replace("\uFEFF", "").trim();

                    if (line.isEmpty()) continue;

                    if (lineNumber == 1) continue;

                    String delimiter = line.contains("\t") ? "\t" : ",";

                    String[] data = line.split(delimiter);

                    if (data.length < 6) continue;

                    if (data[0].equalsIgnoreCase("record_id")) continue;

                    AccountingRecord a = new AccountingRecord();

                    try {
                        a.setRecordId(data[0]);
                        a.setReferenceNo(data[1]);
                        a.setModule(data[2]);
                        a.setAmount(Double.parseDouble(data[3]));
                        a.setRecordDate(data[4]);
                        a.setCreatedAt(data[5]);

                        // VALIDATION
                        if (a.getReferenceNo() == null || a.getReferenceNo().isEmpty()) {
                            a.setValid(false);
                            a.setErrorMessage("Missing reference");
                        } else if (a.getAmount() <= 0) {
                            a.setValid(false);
                            a.setErrorMessage("Invalid amount");
                        } else {
                            a.setValid(true);
                        }

                        records.add(a);

                    } catch (Exception e) {
                        a.setValid(false);
                        a.setErrorMessage("Parsing error");
                        records.add(a);
                    }
                }
            }

            // For XLSX
            else if (fileName.endsWith(".xlsx")) {

                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                        new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(0);

                for (org.apache.poi.ss.usermodel.Row row : sheet) {

                    if (row.getRowNum() == 0) continue;

                    AccountingRecord a = new AccountingRecord();

                    try {
                        a.setRecordId(row.getCell(0).toString());
                        a.setReferenceNo(row.getCell(1).toString());
                        a.setModule(row.getCell(2).toString());
                        a.setAmount(row.getCell(3).getNumericCellValue());
                        a.setRecordDate(row.getCell(4).toString());
                        a.setCreatedAt(row.getCell(5).toString());

                        if (a.getReferenceNo() == null || a.getReferenceNo().isEmpty()) {
                            a.setValid(false);
                            a.setErrorMessage("Missing reference");
                        } else if (a.getAmount() <= 0) {
                            a.setValid(false);
                            a.setErrorMessage("Invalid amount");
                        } else {
                            a.setValid(true);
                        }

                        records.add(a);

                    } catch (Exception e) {
                        a.setValid(false);
                        a.setErrorMessage("Parsing error");
                        records.add(a);
                    }
                }

                workbook.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }

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

    private void validate(Payroll payroll) {

        if (payroll.getEmployeeName() == null || payroll.getEmployeeName().isEmpty()) {
            payroll.setValid(false);
            payroll.setErrorMessage("Missing employee name");
        } else if (payroll.getSalary() <= 0) {
            payroll.setValid(false);
            payroll.setErrorMessage("Invalid salary");
        } else {
            payroll.setValid(true);
            payroll.setErrorMessage("");
        }
    }

    private boolean isPayrollHeaderValue(String value) {
        String normalized = value == null ? "" : value.trim().replace(" ", "").replace("_", "");
        return normalized.equalsIgnoreCase("payrollid");
    }
}
