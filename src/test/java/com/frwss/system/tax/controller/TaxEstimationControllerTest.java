package com.frwss.system.tax.controller;

import com.frwss.system.tax.dto.TaxEstimationRequestDto;
import com.frwss.system.tax.dto.TaxEstimationResponseDto;
import com.frwss.system.tax.service.TaxEstimationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tax Estimation Controller Unit Tests")
class TaxEstimationControllerTest {

    @Mock
    private TaxEstimationService taxEstimationService;

    @InjectMocks
    private TaxEstimationController taxEstimationController;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // showTaxEstimationPage()
    // =========================================================================

    @Test
    @DisplayName("Should display tax estimation page")
    void showTaxEstimationPage_returnsView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        when(taxEstimationService.getAvailableModules())
                .thenReturn(List.of("PAYROLL", "FINANCE"));

        when(taxEstimationService.getAvailableCategories())
                .thenReturn(List.of("SALARY", "VAT"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                taxEstimationController.showTaxEstimationPage(model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("tax/tax-estimation", view);

        assertNotNull(model.getAttribute("request"));
        assertNotNull(model.getAttribute("modules"));
        assertNotNull(model.getAttribute("categories"));

        verify(taxEstimationService).getAvailableModules();
        verify(taxEstimationService).getAvailableCategories();
    }

    // =========================================================================
    // estimateTax()
    // =========================================================================

    @Test
    @DisplayName("Should estimate tax successfully")
    void estimateTax_returnsEstimationResult() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        TaxEstimationRequestDto request = new TaxEstimationRequestDto();

        TaxEstimationResponseDto response =
                new TaxEstimationResponseDto();

        response.setEstimatedTax(new BigDecimal("150.00"));

        when(taxEstimationService.estimateTax(request))
                .thenReturn(response);

        when(taxEstimationService.getAvailableModules())
                .thenReturn(List.of("PAYROLL"));

        when(taxEstimationService.getAvailableCategories())
                .thenReturn(List.of("SALARY"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String view =
                taxEstimationController.estimateTax(
                        request,
                        model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("tax/tax-estimation", view);

        assertEquals(request, model.getAttribute("request"));
        assertEquals(response, model.getAttribute("result"));

        verify(taxEstimationService).estimateTax(request);
    }

    @Test
    @DisplayName("Should reload available modules after tax estimation")
    void estimateTax_loadsAvailableModules() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        TaxEstimationRequestDto request =
                new TaxEstimationRequestDto();

        when(taxEstimationService.estimateTax(request))
                .thenReturn(new TaxEstimationResponseDto());

        when(taxEstimationService.getAvailableModules())
                .thenReturn(List.of("PAYROLL", "FINANCE"));

        when(taxEstimationService.getAvailableCategories())
                .thenReturn(List.of("SALARY"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        taxEstimationController.estimateTax(request, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                List.of("PAYROLL", "FINANCE"),
                model.getAttribute("modules")
        );
    }

    @Test
    @DisplayName("Should reload available categories after tax estimation")
    void estimateTax_loadsAvailableCategories() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        TaxEstimationRequestDto request =
                new TaxEstimationRequestDto();

        when(taxEstimationService.estimateTax(request))
                .thenReturn(new TaxEstimationResponseDto());

        when(taxEstimationService.getAvailableModules())
                .thenReturn(List.of("PAYROLL"));

        when(taxEstimationService.getAvailableCategories())
                .thenReturn(List.of("SALARY", "VAT"));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        taxEstimationController.estimateTax(request, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                List.of("SALARY", "VAT"),
                model.getAttribute("categories")
        );
    }
}