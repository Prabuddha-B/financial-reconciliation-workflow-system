package com.frwss.system.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Stock Purchase Unit Tests")
class StockPurchaseTest {
    @Test
    @DisplayName("Should store and return all stock purchase fields")
    void stockPurchase_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        StockPurchase purchase = new StockPurchase();
        LocalDate date = LocalDate.of(2026, 1, 1); LocalDateTime created = LocalDateTime.now();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        purchase.setPurchaseId("S001"); purchase.setVendorName("Vendor"); purchase.setInvoiceNo("INV001");
        purchase.setAmount(new BigDecimal("20.00")); purchase.setPurchaseDate(date); purchase.setEnteredBy("admin");
        purchase.setCreatedAt(created);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("S001", purchase.getPurchaseId()); assertEquals("Vendor", purchase.getVendorName());
        assertEquals("INV001", purchase.getInvoiceNo()); assertEquals(new BigDecimal("20.00"), purchase.getAmount());
        assertEquals(date, purchase.getPurchaseDate()); assertEquals("admin", purchase.getEnteredBy());
        assertEquals(created, purchase.getCreatedAt());
    }
}
