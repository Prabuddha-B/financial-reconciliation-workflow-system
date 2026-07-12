package com.frwss.system.tax.service;

import com.frwss.system.audit.service.AuditService;
import com.frwss.system.security.SecurityContextAdapter;
import com.frwss.system.tax.model.TaxConfiguration;
import com.frwss.system.tax.repository.TaxConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frwss.system.audit.model.AuditActionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tax Configuration Service Unit Tests")
class TaxConfigurationServiceTest {

    @Mock
    private TaxConfigurationRepository taxConfigurationRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @InjectMocks
    private TaxConfigurationService taxConfigurationService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // getAllConfigurations()
    // =========================================================================

    @Test
    @DisplayName("Should return all tax configurations")
    void getAllConfigurations_returnsConfigurations() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxConfiguration config1 = new TaxConfiguration();
        TaxConfiguration config2 = new TaxConfiguration();

        when(taxConfigurationRepository.findAll())
                .thenReturn(List.of(config1, config2));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<TaxConfiguration> result =
                taxConfigurationService.getAllConfigurations();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(2, result.size());

        verify(taxConfigurationRepository).findAll();
    }


    // =========================================================================
    // getConfiguration()
    // =========================================================================

    @Test
    @DisplayName("Should return tax configuration by ID")
    void getConfiguration_existingConfiguration_returnsConfiguration() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long id = 1L;

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setId(id);

        when(taxConfigurationRepository.findById(id))
                .thenReturn(Optional.of(configuration));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxConfiguration result =
                taxConfigurationService.getConfiguration(id);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(id, result.getId());

        verify(taxConfigurationRepository).findById(id);
    }

    @Test
    @DisplayName("Should throw exception when configuration is not found")
    void getConfiguration_missingConfiguration_throwsException() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long id = 99L;

        when(taxConfigurationRepository.findById(id))
                .thenReturn(Optional.empty());

        // ---------------------------------------------------------------------
        // Act & Assert
        // ---------------------------------------------------------------------

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> taxConfigurationService.getConfiguration(id)
                );

        assertEquals(
                "Tax configuration not found",
                exception.getMessage()
        );
    }

    // =========================================================================
    // updateTaxRate()
    // =========================================================================

    @Test
    @DisplayName("Should update tax rate successfully")
    void updateTaxRate_updatesConfiguration() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long id = 1L;

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setId(id);
        configuration.setModule("PAYROLL");
        configuration.setCategory("SALARY");

        when(taxConfigurationRepository.findById(id))
                .thenReturn(Optional.of(configuration));

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        taxConfigurationService.updateTaxRate(
                id,
                new BigDecimal("18"),
                "Updated VAT"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                new BigDecimal("18"),
                configuration.getTaxRate()
        );

        assertEquals(
                "Updated VAT",
                configuration.getDescription()
        );

        verify(taxConfigurationRepository).save(configuration);
    }

    @Test
    @DisplayName("Should create audit log after updating tax configuration")
    void updateTaxRate_createsAuditLog() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long id = 1L;

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setId(id);
        configuration.setModule("PAYROLL");
        configuration.setCategory("SALARY");

        when(taxConfigurationRepository.findById(id))
                .thenReturn(Optional.of(configuration));

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        taxConfigurationService.updateTaxRate(
                id,
                new BigDecimal("15"),
                "Annual revision"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        verify(auditService).log(
                eq(AuditActionType.MANUAL_MODIFICATION),
                eq("TaxConfiguration"),
                eq("1"),
                eq("admin"),
                contains("PAYROLL")
        );
    }
}