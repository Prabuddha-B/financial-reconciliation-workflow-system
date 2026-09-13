package com.frwss.system.tax.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tax Estimation Response DTO Unit Tests")
class TaxEstimationResponseDtoTest {
    @Test
    @DisplayName("Should initialize disclaimer and timestamp by default")
    void taxEstimationResponseDto_defaults_initializesMetadata() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        TaxEstimationResponseDto dto = new TaxEstimationResponseDto();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        String disclaimer = dto.getDisclaimer();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertNotNull(disclaimer); assertNotNull(dto.getEstimatedAt());
    }

    @Test
    @DisplayName("Should store and return all response fields")
    void taxEstimationResponseDto_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        TaxEstimationResponseDto dto = new TaxEstimationResponseDto(); LocalDateTime time = LocalDateTime.now();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        dto.setModule("PAYROLL"); dto.setCategory("SALARY"); dto.setOriginalAmount(BigDecimal.ONE); dto.setTaxRate(BigDecimal.TEN);
        dto.setEstimatedTax(new BigDecimal("2")); dto.setTotalWithTax(new BigDecimal("12")); dto.setDisclaimer("Notice"); dto.setEstimatedAt(time); dto.setEstimatedBy("admin");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("PAYROLL", dto.getModule()); assertEquals("SALARY", dto.getCategory()); assertEquals(BigDecimal.ONE, dto.getOriginalAmount());
        assertEquals(BigDecimal.TEN, dto.getTaxRate()); assertEquals(new BigDecimal("2"), dto.getEstimatedTax()); assertEquals(new BigDecimal("12"), dto.getTotalWithTax());
        assertEquals("Notice", dto.getDisclaimer()); assertEquals(time, dto.getEstimatedAt()); assertEquals("admin", dto.getEstimatedBy());
    }
}
