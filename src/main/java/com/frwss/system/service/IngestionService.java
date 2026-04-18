package com.frwss.system.service;

import com.frwss.system.dto.AccountingRecordDto;
import com.frwss.system.dto.IngestionResult;
import com.frwss.system.dto.PayrollDto;
import com.frwss.system.dto.ReceiptDto;
import com.frwss.system.dto.StockPurchaseDto;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class IngestionService {

//    Represents the results of saving records
    public record SaveSummary(int savedCount, int skippedDuplicateCount, int skippedInvalidCount) {
    }

//    Dashboard summary stat from DB
    public record DashboardSummary(long totalCount, long validCount, long invalidCount) {
    }

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/uuuu"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    };

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm"),
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss"),
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm a"),
            DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    };

    private final ReceiptRepository receiptRepository;
    private final PayrollRepository payrollRepository;
    private final StockPurchaseRepository stockPurchaseRepository;
    private final AccountingRecordRepository accountingRecordRepository;

    public IngestionService(
            ReceiptRepository receiptRepository,
            PayrollRepository payrollRepository,
            StockPurchaseRepository stockPurchaseRepository,
            AccountingRecordRepository accountingRecordRepository) {
        this.receiptRepository = receiptRepository;
        this.payrollRepository = payrollRepository;
        this.stockPurchaseRepository = stockPurchaseRepository;
        this.accountingRecordRepository = accountingRecordRepository;
    }

//    Process file and returns only records
    public List<?> processFile(MultipartFile file, String dataType) {
        try {
            return processFileForPreview(file, dataType).getRecords();
        } catch (IOException e) {
            throw new RuntimeException("Failed to process uploaded file", e);
        }
    }

//    Main Preview
    public IngestionResult processFileForPreview(MultipartFile file, String dataType) throws IOException {
        return switch (dataType.toUpperCase(Locale.ROOT)) {
            case "RECEIPT" -> previewReceipts(file);
            case "PAYROLL" -> previewPayroll(file);
            case "STOCK" -> previewStock(file);
            case "ACCOUNTING" -> previewAccounting(file);
            default -> throw new IllegalArgumentException("Invalid data type selected");
        };
    }

//    Counts for Summary
    public DashboardSummary getDashboardSummary() {
        long receiptCount = receiptRepository.count();
        long payrollCount = payrollRepository.count();
        long stockCount = stockPurchaseRepository.count();
        long accountingCount = accountingRecordRepository.count();
        long total = receiptCount + payrollCount + stockCount + accountingCount;

        // Invalid rows are not persisted, so DB-backed invalid count remains zero.
        return new DashboardSummary(total, total, 0);
    }

    public SaveSummary saveProcessedRecords(List<?> records, String dataType) {
        if (records == null || records.isEmpty()) {
            return new SaveSummary(0, 0, 0);
        }

        return switch (dataType.toUpperCase(Locale.ROOT)) {
            case "RECEIPT" -> persistReceipts((List<ReceiptDto>) records);
            case "PAYROLL" -> persistPayroll((List<PayrollDto>) records);
            case "STOCK" -> persistStock((List<StockPurchaseDto>) records);
            case "ACCOUNTING" -> persistAccounting((List<AccountingRecordDto>) records);
            default -> throw new IllegalArgumentException("Unknown data type for saving: " + dataType);
        };
    }

//    Previewing Methods
    private IngestionResult previewReceipts(MultipartFile file) throws IOException {
        List<ReceiptDto> records = new ArrayList<>();
        if (isXlsx(file)) {
            readReceiptWorkbook(file, records);
        } else {
            readReceiptCsv(file, records);
        }
        return buildResult(records);
    }

    private IngestionResult previewPayroll(MultipartFile file) throws IOException {
        List<PayrollDto> records = new ArrayList<>();
        if (isXlsx(file)) {
            readPayrollWorkbook(file, records);
        } else {
            readPayrollCsv(file, records);
        }
        return buildResult(records);
    }

    private IngestionResult previewStock(MultipartFile file) throws IOException {
        List<StockPurchaseDto> records = new ArrayList<>();
        if (isXlsx(file)) {
            readStockWorkbook(file, records);
        } else {
            readStockCsv(file, records);
        }
        return buildResult(records);
    }

    private IngestionResult previewAccounting(MultipartFile file) throws IOException {
        List<AccountingRecordDto> records = new ArrayList<>();
        if (isXlsx(file)) {
            readAccountingWorkbook(file, records);
        } else {
            readAccountingCsv(file, records);
        }
        return buildResult(records);
    }

//    Saving to DB skip duplicates
    private SaveSummary persistReceipts(List<ReceiptDto> dtos) {
        List<Receipt> entities = new ArrayList<>();
        int skippedDuplicates = 0;
        int skippedInvalid = 0;
        Set<String> seenReceiptIds = new HashSet<>();
        Set<String> seenReferenceNos = new HashSet<>();
        for (ReceiptDto dto : dtos) {
            if (!dto.isValid()) {
                skippedInvalid++;
                continue;
            }
            String receiptId = dto.getReceiptId();
            String referenceNo = dto.getReferenceNo();
            if (!seenReceiptIds.add(receiptId)
                    || !seenReferenceNos.add(referenceNo)
                    || receiptRepository.existsById(receiptId)
                    || receiptRepository.existsByReferenceNo(referenceNo)) {
                skippedDuplicates++;
                continue;
            }

            Receipt entity = new Receipt();
            entity.setReceiptId(receiptId);
            entity.setReferenceNo(referenceNo);
            entity.setPayerName(dto.getPayerName());
            entity.setAmount(dto.getAmount());
            entity.setReceiptDate(parseDate(dto.getDate()));
            entity.setEnteredBy(dto.getEnteredBy());
            entity.setCreatedAt(LocalDateTime.now());
            entities.add(entity);
        }
        receiptRepository.saveAll(entities);
        return new SaveSummary(entities.size(), skippedDuplicates, skippedInvalid);
    }

    private SaveSummary persistPayroll(List<PayrollDto> dtos) {
        List<Payroll> entities = new ArrayList<>();
        int skippedDuplicates = 0;
        int skippedInvalid = 0;
        for (PayrollDto dto : dtos) {
            if (!dto.isValid()) {
                skippedInvalid++;
                continue;
            }
            if (payrollRepository.existsById(dto.getPayrollId())) {
                skippedDuplicates++;
                continue;
            }

            Payroll entity = new Payroll();
            entity.setPayrollId(dto.getPayrollId());
            entity.setEmployeeId(dto.getEmployeeId());
            entity.setEmployeeName(dto.getEmployeeName());
            entity.setSalary(dto.getSalary());
            entity.setPaymentDate(dto.getPaymentDate());
            entity.setReferenceNo(dto.getReferenceNo());
            entity.setStatus(dto.getStatus());
            entity.setCreatedAt(dto.getCreatedAt());
            entity.setEnteredBy(dto.getEnteredBy());
            entities.add(entity);
        }
        payrollRepository.saveAll(entities);
        return new SaveSummary(entities.size(), skippedDuplicates, skippedInvalid);
    }

    private SaveSummary persistStock(List<StockPurchaseDto> dtos) {
        List<StockPurchase> entities = new ArrayList<>();
        int skippedDuplicates = 0;
        int skippedInvalid = 0;
        Set<String> seenPurchaseIds = new HashSet<>();
        Set<String> seenInvoiceNos = new HashSet<>();
        for (StockPurchaseDto dto : dtos) {
            if (!dto.isValid()) {
                skippedInvalid++;
                continue;
            }
            String purchaseId = dto.getPurchaseId();
            String invoiceNo = dto.getInvoiceNo();
            if (!seenPurchaseIds.add(purchaseId)
                    || !seenInvoiceNos.add(invoiceNo)
                    || stockPurchaseRepository.existsById(purchaseId)
                    || stockPurchaseRepository.existsByInvoiceNo(invoiceNo)) {
                skippedDuplicates++;
                continue;
            }

            StockPurchase entity = new StockPurchase();
            entity.setPurchaseId(purchaseId);
            entity.setVendorName(dto.getVendorName());
            entity.setInvoiceNo(invoiceNo);
            entity.setAmount(BigDecimal.valueOf(dto.getAmount()));
            entity.setPurchaseDate(parseDate(dto.getPurchaseDate()));
            entity.setEnteredBy(dto.getEnteredBy());
            entity.setCreatedAt(parseDateTime(dto.getCreatedAt()));
            entities.add(entity);
        }
        stockPurchaseRepository.saveAll(entities);
        return new SaveSummary(entities.size(), skippedDuplicates, skippedInvalid);
    }

    private SaveSummary persistAccounting(List<AccountingRecordDto> dtos) {
        List<AccountingRecord> entities = new ArrayList<>();
        int skippedDuplicates = 0;
        int skippedInvalid = 0;
        for (AccountingRecordDto dto : dtos) {
            if (!dto.isValid()) {
                skippedInvalid++;
                continue;
            }
            if (accountingRecordRepository.existsById(dto.getRecordId())) {
                skippedDuplicates++;
                continue;
            }

            AccountingRecord entity = new AccountingRecord();
            entity.setRecordId(dto.getRecordId());
            entity.setReferenceNo(dto.getReferenceNo());
            entity.setModule(dto.getModule());
            entity.setAmount(BigDecimal.valueOf(dto.getAmount()));
            entity.setRecordDate(parseDate(dto.getRecordDate()));
            entity.setCreatedAt(LocalDateTime.now());
            entity.setEnteredBy(dto.getEnteredBy());
            entities.add(entity);
        }
        accountingRecordRepository.saveAll(entities);
        return new SaveSummary(entities.size(), skippedDuplicates, skippedInvalid);
    }

//    Read CSV and XLSX
    private void readReceiptCsv(MultipartFile file, List<ReceiptDto> records) throws IOException {
        try (BufferedReader reader = createReader(file)) {
            String line;
            boolean firstLine = true;
            Map<String, Integer> headerMap = null;
            while ((line = reader.readLine()) != null) {
                line = cleanLine(line);
                if (line.isEmpty()) {
                    continue;
                }
                String[] data = splitLine(line);
                if (firstLine) {
                    firstLine = false;
                    if (looksLikeHeader(data)) {
                        headerMap = buildHeaderMap(data);
                        continue;
                    }
                }
                String[] canonical = extractReceiptRow(data, headerMap);
                if (canonical.length < 6 || isHeaderValue(canonical[0], "receipt_id")) {
                    continue;
                }
                records.add(buildReceiptDto(canonical));
            }
        }
    }

    private void readReceiptWorkbook(MultipartFile file, List<ReceiptDto> records) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            boolean firstRow = true;
            Map<String, Integer> headerMap = null;
            for (Row row : sheet) {
                String[] values = rowValues(row, formatter, Math.max(6, row.getLastCellNum()));
                if (firstRow) {
                    firstRow = false;
                    if (looksLikeHeader(values)) {
                        headerMap = buildHeaderMap(values);
                        continue;
                    }
                }
                String[] canonical = extractReceiptRow(values, headerMap);
                String firstCell = canonical[0];
                if (firstCell.isBlank() || isHeaderValue(firstCell, "receipt_id")) {
                    continue;
                }
                records.add(buildReceiptDto(canonical));
            }
        }
    }

    private void readPayrollCsv(MultipartFile file, List<PayrollDto> records) throws IOException {
        try (BufferedReader reader = createReader(file)) {
            String line;
            int lineNumber = 0;
            Map<String, Integer> headerMap = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = cleanLine(line);
                if (line.isEmpty()) {
                    continue;
                }
                String[] data = splitLine(line);
                if (lineNumber == 1 && looksLikeHeader(data)) {
                    headerMap = buildHeaderMap(data);
                    continue;
                }
                String[] canonical = extractPayrollRow(data, headerMap);
                if (canonical.length < 9 || isHeaderValue(canonical[0], "payroll_id")) {
                    continue;
                }
                records.add(buildPayrollDto(canonical));
            }
        }
    }

    private void readPayrollWorkbook(MultipartFile file, List<PayrollDto> records) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> headerMap = null;
            for (Row row : sheet) {
                String[] values = rowValues(row, formatter, Math.max(9, row.getLastCellNum()));
                if (row.getRowNum() == 0 && looksLikeHeader(values)) {
                    headerMap = buildHeaderMap(values);
                    continue;
                }
                String[] canonical = extractPayrollRow(values, headerMap);
                String firstCell = canonical[0];
                if (firstCell.isBlank() || isHeaderValue(firstCell, "payroll_id")) {
                    continue;
                }
                records.add(buildPayrollDto(canonical));
            }
        }
    }

    private void readStockCsv(MultipartFile file, List<StockPurchaseDto> records) throws IOException {
        try (BufferedReader reader = createReader(file)) {
            String line;
            int lineNumber = 0;
            Map<String, Integer> headerMap = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = cleanLine(line);
                if (line.isEmpty()) {
                    continue;
                }
                String[] data = splitLine(line);
                if (lineNumber == 1 && looksLikeHeader(data)) {
                    headerMap = buildHeaderMap(data);
                    continue;
                }
                String[] canonical = extractStockRow(data, headerMap);
                if (canonical.length < 7 || isHeaderValue(canonical[0], "purchase_id")) {
                    continue;
                }
                records.add(buildStockDto(canonical));
            }
        }
    }

    private void readStockWorkbook(MultipartFile file, List<StockPurchaseDto> records) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> headerMap = null;
            for (Row row : sheet) {
                String[] values = rowValues(row, formatter, Math.max(7, row.getLastCellNum()));
                if (row.getRowNum() == 0 && looksLikeHeader(values)) {
                    headerMap = buildHeaderMap(values);
                    continue;
                }
                String[] canonical = extractStockRow(values, headerMap);
                String firstCell = canonical[0];
                if (firstCell.isBlank() || isHeaderValue(firstCell, "purchase_id")) {
                    continue;
                }
                records.add(buildStockDto(canonical));
            }
        }
    }

    private void readAccountingCsv(MultipartFile file, List<AccountingRecordDto> records) throws IOException {
        try (BufferedReader reader = createReader(file)) {
            String line;
            int lineNumber = 0;
            Map<String, Integer> headerMap = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = cleanLine(line);
                if (line.isEmpty()) {
                    continue;
                }
                String[] data = splitLine(line);
                if (lineNumber == 1 && looksLikeHeader(data)) {
                    headerMap = buildHeaderMap(data);
                    continue;
                }
                String[] canonical = extractAccountingRow(data, headerMap);
                if (canonical.length < 6 || isHeaderValue(canonical[0], "record_id")) {
                    continue;
                }
                records.add(buildAccountingDto(canonical));
            }
        }
    }

    private void readAccountingWorkbook(MultipartFile file, List<AccountingRecordDto> records) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> headerMap = null;
            for (Row row : sheet) {
                String[] values = rowValues(row, formatter, Math.max(6, row.getLastCellNum()));
                if (row.getRowNum() == 0 && looksLikeHeader(values)) {
                    headerMap = buildHeaderMap(values);
                    continue;
                }
                String[] canonical = extractAccountingRow(values, headerMap);
                String firstCell = canonical[0];
                if (firstCell.isBlank() || isHeaderValue(firstCell, "record_id")) {
                    continue;
                }
                records.add(buildAccountingDto(canonical));
            }
        }
    }

//    Builders for DTO
    private ReceiptDto buildReceiptDto(String[] data) {
        try {
            ReceiptDto dto = new ReceiptDto(
                    safeValue(data, 0),
                    safeValue(data, 1),
                    safeValue(data, 2),
                    parseBigDecimal(safeValue(data, 3)),
                    safeValue(data, 4),
                    safeValue(data, 5)
            );
            validateReceipt(dto);
            return dto;
        } catch (Exception e) {
            ReceiptDto dto = new ReceiptDto(
                    safeValue(data, 0),
                    safeValue(data, 1),
                    safeValue(data, 2),
                    BigDecimal.ZERO,
                    safeValue(data, 4),
                    safeValue(data, 5)
            );
            dto.setValid(false);
            dto.setErrorMessage("Parsing error");
            return dto;
        }
    }

    private PayrollDto buildPayrollDto(String[] data) {
        try {
            PayrollDto dto = new PayrollDto(
                    safeValue(data, 0),
                    safeValue(data, 1),
                    safeValue(data, 2),
                    parseBigDecimal(safeValue(data, 3)),
                    parseDate(safeValue(data, 4)),
                    safeValue(data, 5),
                    safeValue(data, 6),
                    parseDateTime(safeValue(data, 7)),
                    safeValue(data, 8)
            );
            validatePayroll(dto);
            return dto;
        } catch (Exception e) {
            PayrollDto dto = new PayrollDto(
                    safeValue(data, 0),
                    safeValue(data, 1),
                    safeValue(data, 2),
                    BigDecimal.ZERO,
                    LocalDate.now(),
                    safeValue(data, 5),
                    safeValue(data, 6),
                    LocalDateTime.now(),
                    safeValue(data, 8)
            );
            dto.setValid(false);
            dto.setErrorMessage("Parsing error");
            return dto;
        }
    }

    private StockPurchaseDto buildStockDto(String[] data) {
        StockPurchaseDto dto = new StockPurchaseDto();
        try {
            dto.setPurchaseId(safeValue(data, 0));
            dto.setVendorName(safeValue(data, 1));
            dto.setInvoiceNo(safeValue(data, 2));
            dto.setAmount(parseBigDecimal(safeValue(data, 3)).doubleValue());
            dto.setPurchaseDate(safeValue(data, 4));
            dto.setEnteredBy(safeValue(data, 5));
            dto.setCreatedAt(safeValue(data, 6));
            validateStock(dto);
        } catch (Exception e) {
            dto.setValid(false);
            dto.setErrorMessage("Parsing error");
        }
        return dto;
    }

    private AccountingRecordDto buildAccountingDto(String[] data) {
        AccountingRecordDto dto = new AccountingRecordDto(
                safeValue(data, 0),
                safeValue(data, 1),
                safeValue(data, 2),
                0,
                safeValue(data, 4),
                safeValue(data, 5)
        );
        try {
            dto.setAmount(parseBigDecimal(safeValue(data, 3)).doubleValue());
            validateAccounting(dto);
        } catch (Exception e) {
            dto.setValid(false);
            dto.setErrorMessage("Parsing error");
        }
        return dto;
    }

//    Builders for Results
    private IngestionResult buildResult(List<?> records) {
        int validCount = 0;
        for (Object record : records) {
            if (isValidRecord(record)) {
                validCount++;
            }
        }
        return new IngestionResult(records, records.size(), validCount, records.size() - validCount);
    }

//    Validation
    private boolean isValidRecord(Object record) {
        if (record instanceof ReceiptDto dto) {
            return dto.isValid();
        }
        if (record instanceof PayrollDto dto) {
            return dto.isValid();
        }
        if (record instanceof StockPurchaseDto dto) {
            return dto.isValid();
        }
        if (record instanceof AccountingRecordDto dto) {
            return dto.isValid();
        }
        return false;
    }

//    File Utilities
    private BufferedReader createReader(MultipartFile file) throws IOException {
        return new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
    }

    private boolean isXlsx(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    private String cleanLine(String line) {
        return line.replace("\uFEFF", "").trim();
    }

    private String[] splitLine(String line) {
        return line.split(line.contains("\t") ? "\t" : ",", -1);
    }

    private boolean isHeaderValue(String value, String expectedHeader) {
        String normalized = normalizeHeader(value);
        String expected = normalizeHeader(expectedHeader);
        return normalized.equalsIgnoreCase(expected);
    }

//    Handling the Header
    private boolean looksLikeHeader(String[] values) {
        for (String value : values) {
            String normalized = normalizeHeader(value);
            if (normalized.equals("receiptid") || normalized.equals("referenceno")
                    || normalized.equals("payername") || normalized.equals("receiptdate")
                    || normalized.equals("payrollid") || normalized.equals("employeeid")
                    || normalized.equals("employeename") || normalized.equals("paymentdate")
                    || normalized.equals("purchaseid") || normalized.equals("vendorname")
                    || normalized.equals("invoiceno") || normalized.equals("purchasedate")
                    || normalized.equals("recordid") || normalized.equals("module")
                    || normalized.equals("recorddate") || normalized.equals("enteredby")) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Integer> buildHeaderMap(String[] headers) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            headerMap.put(normalizeHeader(headers[i]), i);
        }
        return headerMap;
    }

    private String[] extractReceiptRow(String[] row, Map<String, Integer> headerMap) {
        return new String[] {
                resolveValue(row, headerMap, 0, "receipt_id", "receiptid"),
                resolveValue(row, headerMap, 1, "reference_no", "referenceno", "reference"),
                resolveValue(row, headerMap, 2, "payer_name", "payername"),
                resolveValue(row, headerMap, 3, "amount"),
                resolveValue(row, headerMap, 4, "receipt_date", "receiptdate", "date"),
                resolveValue(row, headerMap, 5, "entered_by", "enteredby")
        };
    }

    private String[] extractPayrollRow(String[] row, Map<String, Integer> headerMap) {
        return new String[] {
                resolveValue(row, headerMap, 0, "payroll_id", "payrollid"),
                resolveValue(row, headerMap, 1, "employee_id", "employeeid"),
                resolveValue(row, headerMap, 2, "employee_name", "employeename"),
                resolveValue(row, headerMap, 3, "salary"),
                resolveValue(row, headerMap, 4, "payment_date", "paymentdate"),
                resolveValue(row, headerMap, 5, "reference_no", "referenceno", "reference"),
                resolveValue(row, headerMap, 6, "status"),
                resolveValue(row, headerMap, 7, "created_at", "createdat"),
                resolveValue(row, headerMap, 8, "entered_by", "enteredby")
        };
    }

    private String[] extractStockRow(String[] row, Map<String, Integer> headerMap) {
        return new String[] {
                resolveValue(row, headerMap, 0, "purchase_id", "purchaseid"),
                resolveValue(row, headerMap, 1, "vendor_name", "vendorname", "vendor"),
                resolveValue(row, headerMap, 2, "invoice_no", "invoiceno", "invoice"),
                resolveValue(row, headerMap, 3, "amount"),
                resolveValue(row, headerMap, 4, "purchase_date", "purchasedate", "date"),
                resolveValue(row, headerMap, 5, "entered_by", "enteredby"),
                resolveValue(row, headerMap, 6, "created_at", "createdat")
        };
    }

    private String[] extractAccountingRow(String[] row, Map<String, Integer> headerMap) {
        return new String[] {
                resolveValue(row, headerMap, 0, "record_id", "recordid"),
                resolveValue(row, headerMap, 1, "reference_no", "referenceno", "reference"),
                resolveValue(row, headerMap, 2, "module"),
                resolveValue(row, headerMap, 3, "amount"),
                resolveValue(row, headerMap, 4, "record_date", "recorddate", "date"),
                resolveValue(row, headerMap, 5, "entered_by", "enteredby")
        };
    }

    private String resolveValue(String[] row, Map<String, Integer> headerMap, int fallbackIndex, String... aliases) {
        if (headerMap != null) {
            for (String alias : aliases) {
                Integer index = headerMap.get(normalizeHeader(alias));
                if (index != null && index < row.length) {
                    return safeValue(row, index);
                }
            }
        }
        return safeValue(row, fallbackIndex);
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }

    private String[] rowValues(Row row, DataFormatter formatter, int size) {
        String[] values = new String[size];
        for (int i = 0; i < size; i++) {
            values[i] = cellValue(row.getCell(i), formatter);
        }
        return values;
    }

    private String cellValue(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String safeValue(String[] data, int index) {
        return index < data.length ? data[index].trim() : "";
    }

//    D&T Parsing
    private BigDecimal parseBigDecimal(String raw) {
        String cleaned = raw == null ? "" : raw.trim().replace(",", "");
        if (cleaned.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cleaned);
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDate.now();
        }
        String cleaned = raw.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (cleaned.length() >= 10) {
            String firstTen = cleaned.substring(0, 10);
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDate.parse(firstTen, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        throw new DateTimeParseException("Unsupported date format", cleaned, 0);
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        String cleaned = raw.replace("Z", "").trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        try {
            return parseDate(cleaned).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        if (cleaned.length() >= 19) {
            String firstNineteen = cleaned.substring(0, 19);
            for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
                try {
                    return LocalDateTime.parse(firstNineteen, formatter);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        throw new DateTimeParseException("Unsupported date-time format", cleaned, 0);
    }

//    Validation MEthods
    private void validateReceipt(ReceiptDto dto) {
        if (dto.getReferenceNo() == null || dto.getReferenceNo().isBlank()) {
            dto.setValid(false);
            dto.setErrorMessage("Missing reference");
        } else if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            dto.setValid(false);
            dto.setErrorMessage("Invalid amount");
        } else {
            dto.setValid(true);
            dto.setErrorMessage("");
        }
    }

    private void validatePayroll(PayrollDto dto) {
        if (dto.getEmployeeName() == null || dto.getEmployeeName().isBlank()) {
            dto.setValid(false);
            dto.setErrorMessage("Missing employee name");
        } else if (dto.getSalary().compareTo(BigDecimal.ZERO) <= 0) {
            dto.setValid(false);
            dto.setErrorMessage("Invalid salary");
        } else {
            dto.setValid(true);
            dto.setErrorMessage("");
        }
    }

    private void validateStock(StockPurchaseDto dto) {
        if (dto.getVendorName() == null || dto.getVendorName().isBlank()) {
            dto.setValid(false);
            dto.setErrorMessage("Missing vendor name");
        } else if (dto.getAmount() <= 0) {
            dto.setValid(false);
            dto.setErrorMessage("Invalid amount");
        } else {
            dto.setValid(true);
            dto.setErrorMessage("");
        }
    }

    private void validateAccounting(AccountingRecordDto dto) {
        if (dto.getReferenceNo() == null || dto.getReferenceNo().isBlank()) {
            dto.setValid(false);
            dto.setErrorMessage("Missing reference");
        } else if (dto.getAmount() <= 0) {
            dto.setValid(false);
            dto.setErrorMessage("Invalid amount");
        } else {
            dto.setValid(true);
            dto.setErrorMessage("");
        }
    }
}
