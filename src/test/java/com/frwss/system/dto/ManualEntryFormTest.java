package com.frwss.system.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Manual Entry Form Unit Tests")
class ManualEntryFormTest {
    @Test
    @DisplayName("Should store and return all form fields")
    void manualEntryForm_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        ManualEntryForm form = new ManualEntryForm(); LocalDate date = LocalDate.now(); LocalDateTime time = LocalDateTime.now();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        form.setReceiptId("R"); form.setPayrollId("P"); form.setEmployeeId("E"); form.setPurchaseId("S"); form.setRecordId("A");
        form.setReferenceNo("REF"); form.setPayerName("Payer"); form.setEmployeeName("Employee"); form.setVendorName("Vendor");
        form.setInvoiceNo("INV"); form.setModule("FINANCE"); form.setStatus("PAID"); form.setEnteredBy("admin");
        form.setAmount(BigDecimal.ONE); form.setSalary(BigDecimal.TEN); form.setReceiptDate(date); form.setPaymentDate(date);
        form.setPurchaseDate(date); form.setRecordDate(date); form.setCreatedAt(time);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("R", form.getReceiptId()); assertEquals("P", form.getPayrollId()); assertEquals("E", form.getEmployeeId());
        assertEquals("S", form.getPurchaseId()); assertEquals("A", form.getRecordId()); assertEquals("REF", form.getReferenceNo());
        assertEquals("Payer", form.getPayerName()); assertEquals("Employee", form.getEmployeeName()); assertEquals("Vendor", form.getVendorName());
        assertEquals("INV", form.getInvoiceNo()); assertEquals("FINANCE", form.getModule()); assertEquals("PAID", form.getStatus());
        assertEquals("admin", form.getEnteredBy()); assertEquals(BigDecimal.ONE, form.getAmount()); assertEquals(BigDecimal.TEN, form.getSalary());
        assertEquals(date, form.getReceiptDate()); assertEquals(date, form.getPaymentDate()); assertEquals(date, form.getPurchaseDate());
        assertEquals(date, form.getRecordDate()); assertEquals(time, form.getCreatedAt());
    }
}
