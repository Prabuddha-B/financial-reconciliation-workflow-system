package com.frwss.system.tax.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tax Configuration Unit Tests")
class TaxConfigurationTest {
    @Test
    @DisplayName("Should use active configuration defaults")
    void taxConfiguration_defaults_activeByDefault() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        TaxConfiguration configuration = new TaxConfiguration();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        Boolean active = configuration.getIsActive();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertTrue(active); assertNull(configuration.getCreatedAt()); assertNull(configuration.getUpdatedAt());
    }

    @Test
    @DisplayName("Should store and return all tax configuration fields")
    void taxConfiguration_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        TaxConfiguration configuration = new TaxConfiguration(); LocalDateTime time = LocalDateTime.now();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        configuration.setId(1L); configuration.setCategory("SALARY"); configuration.setModule("PAYROLL");
        configuration.setTaxRate(new BigDecimal("18")); configuration.setDescription("Salary tax"); configuration.setIsActive(false);
        configuration.setCreatedAt(time); configuration.setUpdatedAt(time); configuration.setUpdatedBy("admin");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(1L, configuration.getId()); assertEquals("SALARY", configuration.getCategory()); assertEquals("PAYROLL", configuration.getModule());
        assertEquals(new BigDecimal("18"), configuration.getTaxRate()); assertEquals("Salary tax", configuration.getDescription());
        assertFalse(configuration.getIsActive()); assertEquals(time, configuration.getCreatedAt()); assertEquals(time, configuration.getUpdatedAt());
        assertEquals("admin", configuration.getUpdatedBy());
    }
}
