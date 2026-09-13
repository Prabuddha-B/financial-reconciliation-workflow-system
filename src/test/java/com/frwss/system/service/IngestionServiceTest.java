package com.frwss.system.service;

import com.frwss.system.dto.IngestionResult;
import com.frwss.system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frwss.system.service.IngestionService.DashboardSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frwss.system.dto.ReceiptDto;
import com.frwss.system.dto.PayrollDto;
import com.frwss.system.dto.StockPurchaseDto;
import com.frwss.system.dto.AccountingRecordDto;
import com.frwss.system.service.IngestionService.SaveSummary;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import com.frwss.system.model.FinancialRecord;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.mockito.ArgumentMatchers.anyList;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ingestion Service Unit Tests")
class IngestionServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private StockPurchaseRepository stockPurchaseRepository;

    @Mock
    private AccountingRecordRepository accountingRecordRepository;

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @InjectMocks
    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked repositories.
    }

    // =========================================================================
    // getDashboardSummary()
    // =========================================================================

    @Test
    @DisplayName("Should return correct dashboard summary")
    void getDashboardSummary_returnsCorrectCounts() {

        // ---------------------------------------------------------------------
        // Arrange: Mock repository counts.
        // ---------------------------------------------------------------------

        when(receiptRepository.count()).thenReturn(10L);
        when(payrollRepository.count()).thenReturn(5L);
        when(stockPurchaseRepository.count()).thenReturn(3L);
        when(accountingRecordRepository.count()).thenReturn(2L);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        DashboardSummary summary =
                ingestionService.getDashboardSummary();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(20L, summary.totalCount());
        assertEquals(20L, summary.validCount());
        assertEquals(0L, summary.invalidCount());

        verify(receiptRepository).count();
        verify(payrollRepository).count();
        verify(stockPurchaseRepository).count();
        verify(accountingRecordRepository).count();
    }


    // =========================================================================
    // saveProcessedRecords()
    // =========================================================================

    @Test
    @DisplayName("Should save valid receipt records successfully")
    void saveProcessedRecords_validReceiptRecords_returnsSaveSummary() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ReceiptDto receipt = new ReceiptDto(
                "R001",
                "REF001",
                "John Doe",
                new BigDecimal("1500.00"),
                "2026-07-01",
                "admin"
        );

        receipt.setValid(true);

        when(receiptRepository.existsById("R001"))
                .thenReturn(false);

        when(receiptRepository.existsByReferenceNo("REF001"))
                .thenReturn(false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        SaveSummary summary =
                ingestionService.saveProcessedRecords(
                        List.of(receipt),
                        "RECEIPT"
                );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, summary.savedCount());
        assertEquals(0, summary.skippedDuplicateCount());
        assertEquals(0, summary.skippedInvalidCount());

        verify(receiptRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should return empty save summary when record list is empty")
    void saveProcessedRecords_emptyList_returnsEmptySummary() {

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        SaveSummary summary =
                ingestionService.saveProcessedRecords(
                        Collections.emptyList(),
                        "RECEIPT"
                );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(0, summary.savedCount());
        assertEquals(0, summary.skippedDuplicateCount());
        assertEquals(0, summary.skippedInvalidCount());

        verifyNoInteractions(receiptRepository);
    }

    @Test
    @DisplayName("Should throw exception for unknown data type")
    void saveProcessedRecords_unknownDataType_throwsException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        List<String> records = List.of("dummy");

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> ingestionService.saveProcessedRecords(
                                records,
                                "UNKNOWN"
                        )
                );

        assertEquals(
                "Unknown data type for saving: UNKNOWN",
                exception.getMessage()
        );
    }

    // =========================================================================
    // processFile()
    // =========================================================================

    @Test
    @DisplayName("Should process receipt CSV file successfully")
    void processFile_validReceiptCsv_returnsRecords() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        String csv =
                """
                receipt_id,reference_no,payer_name,amount,receipt_date,entered_by
                R001,REF001,John Doe,1500,2026-07-01,admin
                """;

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "receipts.csv",
                        "text/csv",
                        csv.getBytes(StandardCharsets.UTF_8)
                );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<?> records =
                ingestionService.processFile(file, "RECEIPT");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(records);
        assertEquals(1, records.size());

        ReceiptDto dto = (ReceiptDto) records.get(0);

        assertEquals("R001", dto.getReceiptId());
        assertTrue(dto.isValid());
    }

    @Test
    @DisplayName("Should throw exception for invalid data type")
    void processFile_invalidDataType_throwsException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "dummy.csv",
                        "text/csv",
                        "dummy".getBytes(StandardCharsets.UTF_8)
                );

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> ingestionService.processFile(file, "INVALID")
                );

        assertTrue(
                exception.getMessage().contains("Invalid data type")
                        || exception.getMessage().contains("Failed to process")
        );
    }

    // =========================================================================
    // processFileForPreview()
    // =========================================================================

    @Test
    @DisplayName("Should throw exception when preview data type is invalid")
    void processFileForPreview_invalidType_throwsException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.csv",
                        "text/csv",
                        "dummy".getBytes(StandardCharsets.UTF_8)
                );

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> ingestionService.processFileForPreview(
                                file,
                                "INVALID"
                        )
                );

        assertEquals(
                "Invalid data type selected",
                exception.getMessage()
        );
    }

    // =========================================================================
    // processCSV()
    // =========================================================================

    @Test
    @DisplayName("Should process CSV and save new financial records")
    void processCSV_validFile_savesFinancialRecords() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        String csv =
                """
                Reference ID,Amount,Date
                REF001,1500.00,2026-07-01
                """;

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "financial.csv",
                        "text/csv",
                        csv.getBytes(StandardCharsets.UTF_8)
                );

        when(financialRecordRepository
                .existsByReferenceIdAndAmountAndDate(
                        "REF001",
                        1500.00,
                        LocalDate.parse("2026-07-01")
                ))
                .thenReturn(false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ingestionService.processCSV(file);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<List<FinancialRecord>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(financialRecordRepository)
                .saveAll(captor.capture());

        List<FinancialRecord> savedRecords = captor.getValue();

        assertEquals(1, savedRecords.size());

        FinancialRecord record = savedRecords.get(0);

        assertEquals("REF001", record.getReferenceId());
        assertEquals(1500.00, record.getAmount());
        assertEquals(LocalDate.parse("2026-07-01"), record.getDate());
    }

    @Test
    @DisplayName("Should skip duplicate financial records")
    void processCSV_duplicateRecord_doesNotSaveRecord() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        String csv =
                """
                Reference ID,Amount,Date
                REF001,1500.00,2026-07-01
                """;

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "financial.csv",
                        "text/csv",
                        csv.getBytes(StandardCharsets.UTF_8)
                );

        when(financialRecordRepository
                .existsByReferenceIdAndAmountAndDate(
                        "REF001",
                        1500.00,
                        LocalDate.parse("2026-07-01")
                ))
                .thenReturn(true);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ingestionService.processCSV(file);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<List<FinancialRecord>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(financialRecordRepository)
                .saveAll(captor.capture());

        assertTrue(captor.getValue().isEmpty());
    }

    // =========================================================================
    // processFileForPreview() record types
    // =========================================================================

    @Test
    @DisplayName("Should preview payroll CSV records")
    void processFileForPreview_validPayrollCsv_returnsRecords() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = csvFile(
                "payroll.csv",
                "payroll_id,employee_id,employee_name,salary,payment_date,reference_no,status,created_at,entered_by\n"
                        + "P001,E001,Jane Doe,2500,2026-07-01,REF001,PAID,2026-07-01T10:00:00,admin\n"
        );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        IngestionResult result = ingestionService.processFileForPreview(file, "PAYROLL");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.getValidRecords());
        assertEquals(0, result.getInvalidRecords());
        assertEquals("P001", ((PayrollDto) result.getRecords().get(0)).getPayrollId());
    }

    @Test
    @DisplayName("Should preview stock CSV records")
    void processFileForPreview_validStockCsv_returnsRecords() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = csvFile(
                "stock.csv",
                "purchase_id,vendor_name,invoice_no,amount,purchase_date,entered_by,created_at\n"
                        + "S001,Vendor,V001,1250.50,2026-07-01,admin,2026-07-01T10:00:00\n"
        );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        IngestionResult result = ingestionService.processFileForPreview(file, "STOCK");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.getValidRecords());
        assertEquals("S001", ((StockPurchaseDto) result.getRecords().get(0)).getPurchaseId());
    }

    @Test
    @DisplayName("Should preview accounting CSV records")
    void processFileForPreview_validAccountingCsv_returnsRecords() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = csvFile(
                "accounting.csv",
                "record_id,reference_no,module,amount,record_date,entered_by\n"
                        + "A001,REF001,FINANCE,500.25,2026-07-01,admin\n"
        );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        IngestionResult result = ingestionService.processFileForPreview(file, "ACCOUNTING");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.getValidRecords());
        assertEquals("A001", ((AccountingRecordDto) result.getRecords().get(0)).getRecordId());
    }

    @Test
    @DisplayName("Should mark invalid receipt rows during preview")
    void processFileForPreview_invalidReceiptValues_marksRecordInvalid() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = csvFile(
                "invalid-receipt.csv",
                "receipt_id,reference_no,payer_name,amount,receipt_date,entered_by\n"
                        + "R001,,John Doe,not-a-number,not-a-date,admin\n"
        );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        IngestionResult result = ingestionService.processFileForPreview(file, "RECEIPT");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.getTotalRecords());
        assertEquals(0, result.getValidRecords());
        assertEquals(1, result.getInvalidRecords());
        assertFalse(((ReceiptDto) result.getRecords().get(0)).isValid());
    }

    @Test
    @DisplayName("Should ignore empty rows and mark incomplete rows invalid")
    void processFileForPreview_emptyAndIncompleteRows_marksIncompleteRowInvalid() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = csvFile(
                "empty.csv",
                "receipt_id,reference_no,payer_name,amount,receipt_date,entered_by\n"
                        + "\n"
                        + "R001,REF001\n"
        );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        IngestionResult result = ingestionService.processFileForPreview(file, "RECEIPT");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.getTotalRecords());
        assertEquals(0, result.getValidRecords());
        assertEquals(1, result.getInvalidRecords());
    }

    @Test
    @DisplayName("Should preview receipt XLSX records")
    void processFileForPreview_validReceiptXlsx_returnsRecords() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        MockMultipartFile file = xlsxFile(
                "receipts.xlsx",
                new String[]{"receipt_id", "reference_no", "payer_name", "amount", "receipt_date", "entered_by"},
                new String[]{"R002", "REF002", "Jane Doe", "300.50", "2026-07-02", "admin"}
        );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        IngestionResult result = ingestionService.processFileForPreview(file, "RECEIPT");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.getValidRecords());
        assertEquals("R002", ((ReceiptDto) result.getRecords().get(0)).getReceiptId());
    }

    // =========================================================================
    // saveProcessedRecords() record types and branches
    // =========================================================================

    @Test
    @DisplayName("Should save payroll records and skip invalid payroll records")
    void saveProcessedRecords_payrollRecords_returnsSaveSummary() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        PayrollDto valid = new PayrollDto(
                "P001", "E001", "Jane Doe", new BigDecimal("2500"),
                LocalDate.of(2026, 7, 1), "REF001", "PAID",
                LocalDateTime.of(2026, 7, 1, 10, 0), "admin"
        );
        PayrollDto invalid = new PayrollDto(
                "P002", "E002", "Invalid", BigDecimal.ZERO,
                LocalDate.of(2026, 7, 1), "REF002", "INVALID",
                LocalDateTime.now(), "admin"
        );
        invalid.setValid(false);
        when(payrollRepository.existsById("P001")).thenReturn(false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        SaveSummary summary = ingestionService.saveProcessedRecords(
                List.of(valid, invalid),
                "PAYROLL"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, summary.savedCount());
        assertEquals(0, summary.skippedDuplicateCount());
        assertEquals(1, summary.skippedInvalidCount());
        verify(payrollRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip duplicate stock purchase IDs and invoice numbers")
    void saveProcessedRecords_duplicateStockRecords_skipsDuplicates() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        StockPurchaseDto first = stockDto("S001", "INV001");
        StockPurchaseDto duplicate = stockDto("S001", "INV001");
        when(stockPurchaseRepository.existsById("S001")).thenReturn(false);
        when(stockPurchaseRepository.existsByInvoiceNo("INV001")).thenReturn(false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        SaveSummary summary = ingestionService.saveProcessedRecords(
                List.of(first, duplicate),
                "STOCK"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, summary.savedCount());
        assertEquals(1, summary.skippedDuplicateCount());
        verify(stockPurchaseRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should save accounting records")
    void saveProcessedRecords_validAccountingRecords_savesRecords() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecordDto dto = new AccountingRecordDto(
                "A001", "REF001", "FINANCE", 500.25, "2026-07-01", "admin"
        );
        dto.setValid(true);
        when(accountingRecordRepository.existsById("A001")).thenReturn(false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        SaveSummary summary = ingestionService.saveProcessedRecords(
                List.of(dto),
                "ACCOUNTING"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, summary.savedCount());
        assertEquals(0, summary.skippedInvalidCount());
        verify(accountingRecordRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should skip duplicate accounting records")
    void saveProcessedRecords_duplicateAccountingRecord_skipsDuplicate() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AccountingRecordDto first = new AccountingRecordDto(
                "A001", "REF001", "FINANCE", 500.25, "2026-07-01", "admin"
        );
        AccountingRecordDto duplicate = new AccountingRecordDto(
                "A001", "REF002", "FINANCE", 600.25, "2026-07-01", "admin"
        );
        first.setValid(true);
        duplicate.setValid(true);
        when(accountingRecordRepository.existsById("A001")).thenReturn(false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        SaveSummary summary = ingestionService.saveProcessedRecords(
                List.of(first, duplicate),
                "ACCOUNTING"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, summary.savedCount());
        assertEquals(1, summary.skippedDuplicateCount());
        verify(accountingRecordRepository).saveAll(anyList());
    }

    private MockMultipartFile csvFile(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile xlsxFile(String filename, String[] headers, String[] values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet1");
            var headerRow = sheet.createRow(0);
            var valueRow = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                valueRow.createCell(i).setCellValue(values[i]);
            }
            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }

    private StockPurchaseDto stockDto(String purchaseId, String invoiceNo) {
        StockPurchaseDto dto = new StockPurchaseDto();
        dto.setPurchaseId(purchaseId);
        dto.setVendorName("Vendor");
        dto.setInvoiceNo(invoiceNo);
        dto.setAmount(100.50);
        dto.setPurchaseDate("2026-07-01");
        dto.setCreatedAt("2026-07-01T10:00:00");
        dto.setEnteredBy("admin");
        dto.setValid(true);
        return dto;
    }

}
