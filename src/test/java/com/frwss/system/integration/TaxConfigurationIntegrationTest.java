package com.frwss.system.integration;

import com.frwss.system.tax.model.TaxConfiguration;
import com.frwss.system.tax.repository.TaxConfigurationRepository;
import com.frwss.system.tax.service.TaxConfigurationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Tax Configuration Integration Tests")
class TaxConfigurationIntegrationTest {

    @Autowired
    private TaxConfigurationService taxConfigurationService;

    @Autowired
    private TaxConfigurationRepository taxConfigurationRepository;

    // =========================================================================
    // Tax configuration lifecycle
    // =========================================================================

    @Test
    @DisplayName("Should create, retrieve, and update a tax configuration")
    void taxConfigurationLifecycle_createAndUpdate_persistsValues() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setModule("INTEGRATION-PAYROLL");
        configuration.setCategory("INTEGRATION-SALARY");
        configuration.setTaxRate(new BigDecimal("10.50"));
        configuration.setDescription("Integration tax configuration");
        configuration.setIsActive(true);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxConfiguration savedConfiguration = taxConfigurationRepository.save(configuration);
        TaxConfiguration retrievedConfiguration = taxConfigurationService.getConfiguration(
                savedConfiguration.getId()
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(
                savedConfiguration.getId()
        );

        assertEquals(
                "INTEGRATION-PAYROLL",
                retrievedConfiguration.getModule()
        );

        assertEquals(
                "INTEGRATION-SALARY",
                retrievedConfiguration.getCategory()
        );

        assertEquals(
                new BigDecimal("10.50"),
                retrievedConfiguration.getTaxRate()
        );

        assertEquals(
                "Integration tax configuration",
                retrievedConfiguration.getDescription()
        );

        taxConfigurationService.updateTaxRate(
                savedConfiguration.getId(),
                new BigDecimal("12.75"),
                "Updated integration tax rate"
        );

        TaxConfiguration updatedConfiguration = taxConfigurationRepository.findById(
                savedConfiguration.getId()
        ).orElseThrow();

        assertEquals(
                new BigDecimal("12.75"),
                updatedConfiguration.getTaxRate()
        );

        assertEquals(
                "Updated integration tax rate",
                updatedConfiguration.getDescription()
        );
    }
}
