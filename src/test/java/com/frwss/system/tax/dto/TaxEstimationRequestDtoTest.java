package com.frwss.system.tax.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Tax Estimation Request DTO Unit Tests")
class TaxEstimationRequestDtoTest {
    @Test
    @DisplayName("Should store and return request fields")
    void taxEstimationRequestDto_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        TaxEstimationRequestDto dto = new TaxEstimationRequestDto();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        dto.setModule("PAYROLL"); dto.setCategory("SALARY"); dto.setAmount(new BigDecimal("1000"));

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("PAYROLL", dto.getModule()); assertEquals("SALARY", dto.getCategory()); assertEquals(new BigDecimal("1000"), dto.getAmount());
    }
}
