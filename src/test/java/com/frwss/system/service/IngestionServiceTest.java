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

}