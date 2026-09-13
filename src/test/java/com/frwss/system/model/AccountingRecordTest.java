package com.frwss.system.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Accounting Record Unit Tests")
class AccountingRecordTest {
    @Test
    @DisplayName("Should store and return all accounting record fields")
    void accountingRecord_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        AccountingRecord record = new AccountingRecord();
        LocalDate date = LocalDate.of(2026, 1, 1);
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 10, 0);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        record.setRecordId("A001"); record.setReferenceNo("REF001"); record.setModule("FINANCE");
        record.setAmount(new BigDecimal("10.50")); record.setRecordDate(date); record.setEnteredBy("admin");
        record.setParentRecordId("P001"); record.setBulkTransaction(true); record.setBulkReferencePrefix("BULK");
        record.setCreatedAt(created);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("A001", record.getRecordId()); assertEquals("REF001", record.getReferenceNo());
        assertEquals("FINANCE", record.getModule()); assertEquals(new BigDecimal("10.50"), record.getAmount());
        assertEquals(date, record.getRecordDate()); assertEquals("admin", record.getEnteredBy());
        assertEquals("P001", record.getParentRecordId()); assertTrue(record.getBulkTransaction());
        assertEquals("BULK", record.getBulkReferencePrefix()); assertEquals(created, record.getCreatedAt());
    }
}
