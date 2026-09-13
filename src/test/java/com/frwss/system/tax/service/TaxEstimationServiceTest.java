package com.frwss.system.tax.service;

import com.frwss.system.audit.service.AuditService;
import com.frwss.system.security.SecurityContextAdapter;
import com.frwss.system.tax.repository.TaxConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.tax.dto.TaxEstimationRequestDto;
import com.frwss.system.tax.dto.TaxEstimationResponseDto;
import com.frwss.system.tax.model.TaxConfiguration;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tax Estimation Service Unit Tests")
class TaxEstimationServiceTest {

    @Mock
    private TaxConfigurationRepository taxConfigurationRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @InjectMocks
    private TaxEstimationService taxEstimationService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // estimateTax()
    // =========================================================================

    @Test
    @DisplayName("Should estimate tax successfully")
    void estimateTax_validConfiguration_returnsEstimatedTax() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = new TaxEstimationRequestDto();
        request.setModule("PAYROLL");
        request.setCategory("SALARY");
        request.setAmount(new BigDecimal("1000.00"));

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setModule("PAYROLL");
        configuration.setCategory("SALARY");
        configuration.setTaxRate(new BigDecimal("10"));

        when(taxConfigurationRepository
                .findActiveByModuleAndCategory("PAYROLL", "SALARY"))
                .thenReturn(Optional.of(configuration));

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response =
                taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(new BigDecimal("100.00"), response.getEstimatedTax());
        assertEquals(new BigDecimal("1100.00"), response.getTotalWithTax());
        assertEquals(new BigDecimal("10"), response.getTaxRate());
        assertEquals("admin", response.getEstimatedBy());

        verify(auditService).log(
                eq(AuditActionType.TAX_ESTIMATION),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should return error response when tax configuration is missing")
    void estimateTax_missingConfiguration_returnsErrorResponse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = new TaxEstimationRequestDto();
        request.setModule("PAYROLL");
        request.setCategory("BONUS");
        request.setAmount(new BigDecimal("1000"));

        when(taxConfigurationRepository
                .findActiveByModuleAndCategory("PAYROLL", "BONUS"))
                .thenReturn(Optional.empty());

        when(securityContextAdapter.getCurrentUsername())
                .thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response =
                taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(BigDecimal.ZERO, response.getEstimatedTax());
        assertEquals(BigDecimal.ZERO, response.getTaxRate());

        assertTrue(
                response.getDisclaimer()
                        .contains("No active tax configuration found.")
        );

        verify(auditService).log(
                eq(AuditActionType.TAX_ESTIMATION_ERROR),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }


    // =========================================================================
    // getTaxRatesByModule()
    // =========================================================================

    @Test
    @DisplayName("Should return tax rates for a module")
    void getTaxRatesByModule_returnsTaxRates() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxConfiguration vat = new TaxConfiguration();
        vat.setCategory("VAT");
        vat.setTaxRate(new BigDecimal("15"));

        TaxConfiguration nbt = new TaxConfiguration();
        nbt.setCategory("NBT");
        nbt.setTaxRate(new BigDecimal("2"));

        when(taxConfigurationRepository.findActiveByModule("FINANCE"))
                .thenReturn(List.of(vat, nbt));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Map<String, BigDecimal> result =
                taxEstimationService.getTaxRatesByModule("FINANCE");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("15"), result.get("VAT"));
        assertEquals(new BigDecimal("2"), result.get("NBT"));

        verify(taxConfigurationRepository)
                .findActiveByModule("FINANCE");
    }

    // =========================================================================
    // getAllActiveConfigurations()
    // =========================================================================

    @Test
    @DisplayName("Should return all active tax configurations")
    void getAllActiveConfigurations_returnsConfigurations() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxConfiguration config1 = new TaxConfiguration();
        TaxConfiguration config2 = new TaxConfiguration();

        when(taxConfigurationRepository.findAllActive())
                .thenReturn(List.of(config1, config2));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<TaxConfiguration> result =
                taxEstimationService.getAllActiveConfigurations();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(2, result.size());

        verify(taxConfigurationRepository)
                .findAllActive();
    }

    // =========================================================================
    // hasTaxConfiguration()
    // =========================================================================

    @Test
    @DisplayName("Should return true when tax configuration exists")
    void hasTaxConfiguration_existingConfiguration_returnsTrue() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(taxConfigurationRepository
                .existsByModuleAndCategoryAndIsActiveTrue(
                        "PAYROLL",
                        "SALARY"))
                .thenReturn(true);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result =
                taxEstimationService.hasTaxConfiguration(
                        "PAYROLL",
                        "SALARY");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(result);

        verify(taxConfigurationRepository)
                .existsByModuleAndCategoryAndIsActiveTrue(
                        "PAYROLL",
                        "SALARY");
    }

    @Test
    @DisplayName("Should return false when tax configuration does not exist")
    void hasTaxConfiguration_missingConfiguration_returnsFalse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(taxConfigurationRepository
                .existsByModuleAndCategoryAndIsActiveTrue(
                        "PAYROLL",
                        "BONUS"))
                .thenReturn(false);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        boolean result =
                taxEstimationService.hasTaxConfiguration(
                        "PAYROLL",
                        "BONUS");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(result);
    }

    // =========================================================================
    // getAvailableModules()
    // =========================================================================

    @Test
    @DisplayName("Should return available modules")
    void getAvailableModules_returnsModules() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(taxConfigurationRepository.findAllActiveModules())
                .thenReturn(List.of("PAYROLL", "FINANCE"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<String> result =
                taxEstimationService.getAvailableModules();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(2, result.size());
        assertTrue(result.contains("PAYROLL"));
        assertTrue(result.contains("FINANCE"));

        verify(taxConfigurationRepository)
                .findAllActiveModules();
    }

    // =========================================================================
    // getAvailableCategories()
    // =========================================================================

    @Test
    @DisplayName("Should return available categories")
    void getAvailableCategories_returnsCategories() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(taxConfigurationRepository.findAllActiveCategories())
                .thenReturn(List.of("SALARY", "VAT"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<String> result =
                taxEstimationService.getAvailableCategories();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(2, result.size());
        assertTrue(result.contains("SALARY"));
        assertTrue(result.contains("VAT"));

        verify(taxConfigurationRepository)
                .findAllActiveCategories();
    }

    // =========================================================================
    // estimateTax() edge and failure paths
    // =========================================================================

    @Test
    @DisplayName("Should estimate zero tax for a zero amount")
    void estimateTax_zeroAmount_returnsZeroTax() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = request("PAYROLL", "SALARY", BigDecimal.ZERO);
        TaxConfiguration configuration = configuration(new BigDecimal("18"));
        when(taxConfigurationRepository.findActiveByModuleAndCategory("PAYROLL", "SALARY"))
                .thenReturn(Optional.of(configuration));
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                0,
                BigDecimal.ZERO.setScale(2).compareTo(response.getEstimatedTax())
        );
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(response.getTotalWithTax())
        );
    }

    @Test
    @DisplayName("Should calculate negative tax for a negative amount as currently implemented")
    void estimateTax_negativeAmount_returnsNegativeTax() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = request("PAYROLL", "SALARY", new BigDecimal("-100"));
        when(taxConfigurationRepository.findActiveByModuleAndCategory("PAYROLL", "SALARY"))
                .thenReturn(Optional.of(configuration(new BigDecimal("10"))));
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                new BigDecimal("-10.00"),
                response.getEstimatedTax()
        );
        assertEquals(
                0,
                new BigDecimal("-110").compareTo(response.getTotalWithTax())
        );
    }

    @Test
    @DisplayName("Should calculate tax for a large amount")
    void estimateTax_largeAmount_returnsEstimatedTax() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = request(
                "FINANCE",
                "VAT",
                new BigDecimal("999999999999999999.99")
        );
        when(taxConfigurationRepository.findActiveByModuleAndCategory("FINANCE", "VAT"))
                .thenReturn(Optional.of(configuration(new BigDecimal("18"))));
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                new BigDecimal("179999999999999999.9982").setScale(2, RoundingMode.HALF_UP),
                response.getEstimatedTax()
        );
    }

    @Test
    @DisplayName("Should round tax using half up rounding")
    void estimateTax_fractionalTaxAmount_roundsHalfUp() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = request("FINANCE", "VAT", new BigDecimal("10.00"));
        when(taxConfigurationRepository.findActiveByModuleAndCategory("FINANCE", "VAT"))
                .thenReturn(Optional.of(configuration(new BigDecimal("2.55"))));
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                new BigDecimal("0.26"),
                response.getEstimatedTax()
        );
        assertEquals(
                new BigDecimal("10.26"),
                response.getTotalWithTax()
        );
    }

    @Test
    @DisplayName("Should return an error response for an inactive configuration")
    void estimateTax_inactiveConfiguration_returnsErrorResponse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = request("PAYROLL", "BONUS", new BigDecimal("1000"));
        when(taxConfigurationRepository.findActiveByModuleAndCategory("PAYROLL", "BONUS"))
                .thenReturn(Optional.empty());
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                BigDecimal.ZERO,
                response.getTaxRate()
        );
        assertTrue(
                response.getDisclaimer().contains("No active tax configuration found.")
        );
    }

    @Test
    @DisplayName("Should use the latest category rate when multiple configurations share a category")
    void getTaxRatesByModule_multipleConfigurations_returnsCategoryMap() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxConfiguration first = configuration(new BigDecimal("10"));
        first.setCategory("VAT");
        TaxConfiguration second = configuration(new BigDecimal("15"));
        second.setCategory("VAT");
        when(taxConfigurationRepository.findActiveByModule("FINANCE"))
                .thenReturn(List.of(first, second));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        Map<String, BigDecimal> result = taxEstimationService.getTaxRatesByModule("FINANCE");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                1,
                result.size()
        );
        assertEquals(
                new BigDecimal("15"),
                result.get("VAT")
        );
    }

    @Test
    @DisplayName("Should propagate repository failures while finding tax configuration")
    void estimateTax_repositoryFailure_returnsErrorResponse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = request("PAYROLL", "SALARY", new BigDecimal("1000"));
        when(taxConfigurationRepository.findActiveByModuleAndCategory("PAYROLL", "SALARY"))
                .thenThrow(new RuntimeException("Repository failure"));
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                response.getDisclaimer().contains("Repository failure")
        );
        verify(auditService).log(
                eq(AuditActionType.TAX_ESTIMATION_ERROR),
                anyString(),
                anyString(),
                anyString(),
                contains("Repository failure")
        );
    }

    @Test
    @DisplayName("Should return an error response when audit logging fails")
    void estimateTax_auditFailure_returnsErrorResponse() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        TaxEstimationRequestDto request = request("PAYROLL", "SALARY", new BigDecimal("1000"));
        when(taxConfigurationRepository.findActiveByModuleAndCategory("PAYROLL", "SALARY"))
                .thenReturn(Optional.of(configuration(new BigDecimal("10"))));
        when(securityContextAdapter.getCurrentUsername()).thenReturn("admin");
        doThrow(new RuntimeException("Audit failure"))
                .when(auditService)
                .log(eq(AuditActionType.TAX_ESTIMATION), anyString(), anyString(), anyString(), anyString());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(
                response.getDisclaimer().contains("Audit failure")
        );
        verify(auditService).log(
                eq(AuditActionType.TAX_ESTIMATION_ERROR),
                anyString(),
                anyString(),
                anyString(),
                contains("Audit failure")
        );
    }

    private TaxEstimationRequestDto request(String module, String category, BigDecimal amount) {
        TaxEstimationRequestDto request = new TaxEstimationRequestDto();
        request.setModule(module);
        request.setCategory(category);
        request.setAmount(amount);
        return request;
    }

    private TaxConfiguration configuration(BigDecimal taxRate) {
        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setModule("PAYROLL");
        configuration.setCategory("SALARY");
        configuration.setTaxRate(taxRate);
        return configuration;
    }

}
