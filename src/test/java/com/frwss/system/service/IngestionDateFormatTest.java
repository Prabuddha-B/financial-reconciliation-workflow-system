package com.frwss.system.service;

import com.frwss.system.dto.AccountingRecordDto;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.repository.FinancialRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks how slash-separated dates are read when records are saved.
 * Day-first (the local convention) must win over month-first.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ingestion Date Format Unit Tests")
class IngestionDateFormatTest {

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

    @Test
    @DisplayName("Should read 05/08/2026 as 5 August, not 8 May")
    void saveProcessedRecords_ambiguousSlashDate_isReadDayFirst() {
        assertEquals(LocalDate.of(2026, 8, 5), savedRecordDate("05/08/2026"));
    }

    @Test
    @DisplayName("Should read 5/8/2026 (no leading zeros) as 5 August")
    void saveProcessedRecords_ambiguousShortDate_isReadDayFirst() {
        assertEquals(LocalDate.of(2026, 8, 5), savedRecordDate("5/8/2026"));
    }

    @Test
    @DisplayName("Should read 15/08/2026 as 15 August")
    void saveProcessedRecords_dayAbove12_isReadDayFirst() {
        assertEquals(LocalDate.of(2026, 8, 15), savedRecordDate("15/08/2026"));
    }

    @Test
    @DisplayName("Should fall back to month-first when the value cannot be day-first (08/15/2026)")
    void saveProcessedRecords_monthFirstOnlyValue_fallsBackToMonthFirst() {
        assertEquals(LocalDate.of(2026, 8, 15), savedRecordDate("08/15/2026"));
    }

    @Test
    @DisplayName("Should still read ISO dates unchanged")
    void saveProcessedRecords_isoDate_isUnchanged() {
        assertEquals(LocalDate.of(2026, 8, 5), savedRecordDate("2026-08-05"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    @SuppressWarnings("unchecked")
    private LocalDate savedRecordDate(String rawDate) {
        AccountingRecordDto dto = new AccountingRecordDto(
                "A001", "REF001", "FINANCE", 500.25, rawDate, "admin");
        dto.setValid(true);

        when(accountingRecordRepository.existsById("A001")).thenReturn(false);

        ingestionService.saveProcessedRecords(List.of(dto), "ACCOUNTING");

        ArgumentCaptor<List<AccountingRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(accountingRecordRepository).saveAll(captor.capture());

        return captor.getValue().get(0).getRecordDate();
    }
}