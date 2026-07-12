package com.frwss.system.tax.controller;

import com.frwss.system.tax.model.TaxConfiguration;
import com.frwss.system.tax.service.TaxConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Tax Configuration Controller Unit Tests")
class AdminTaxConfigurationControllerTest {

    @Mock
    private TaxConfigurationService taxConfigurationService;

    @InjectMocks
    private AdminTaxConfigurationController adminTaxConfigurationController;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // listConfigurations()
    // =========================================================================

    @Test
    @DisplayName("Should display tax configuration list")
    void listConfigurations_returnsListView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        TaxConfiguration config1 = new TaxConfiguration();
        TaxConfiguration config2 = new TaxConfiguration();

        when(taxConfigurationService.getAllConfigurations())
                .thenReturn(List.of(config1, config2));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                adminTaxConfigurationController.listConfigurations(model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("admin/tax-configurations/list", view);

        assertNotNull(model.getAttribute("configurations"));

        verify(taxConfigurationService).getAllConfigurations();
    }

    // =========================================================================
    // editConfiguration()
    // =========================================================================

    @Test
    @DisplayName("Should display edit configuration page")
    void editConfiguration_returnsEditView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long id = 1L;

        Model model = new ConcurrentModel();

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setId(id);

        when(taxConfigurationService.getConfiguration(id))
                .thenReturn(configuration);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                adminTaxConfigurationController.editConfiguration(id, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("admin/tax-configurations/edit", view);

        assertEquals(
                configuration,
                model.getAttribute("configuration")
        );

        verify(taxConfigurationService).getConfiguration(id);
    }

    // =========================================================================
    // updateConfiguration()
    // =========================================================================

    @Test
    @DisplayName("Should update tax configuration successfully")
    void updateConfiguration_updatesTaxRate() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long id = 1L;

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setTaxRate(new BigDecimal("18"));
        configuration.setDescription("Updated tax rate");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                adminTaxConfigurationController.updateConfiguration(
                        id,
                        configuration);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "redirect:/admin/tax-configurations",
                view
        );

        verify(taxConfigurationService)
                .updateTaxRate(
                        id,
                        new BigDecimal("18"),
                        "Updated tax rate"
                );
    }

    @Test
    @DisplayName("Should redirect to configuration list after update")
    void updateConfiguration_redirectsToList() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Long id = 2L;

        TaxConfiguration configuration = new TaxConfiguration();
        configuration.setTaxRate(new BigDecimal("15"));
        configuration.setDescription("Annual revision");

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                adminTaxConfigurationController.updateConfiguration(
                        id,
                        configuration);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "redirect:/admin/tax-configurations",
                view
        );
    }

}