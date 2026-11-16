// -------------------------------------------------------------------------------
//       Handles web requests for the Tax Estimation module
// -------------------------------------------------------------------------------
package com.frwss.system.tax.controller;

import com.frwss.system.tax.dto.TaxEstimationRequestDto;
import com.frwss.system.tax.dto.TaxEstimationResponseDto;
import com.frwss.system.tax.service.TaxEstimationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tax")
public class TaxEstimationController {

    private final TaxEstimationService taxEstimationService;

    /**
     * Initializes the controller with the tax estimation service.
     *
     * @param taxEstimationService service responsible for tax estimation operations
     */
    public TaxEstimationController(TaxEstimationService taxEstimationService) {
        this.taxEstimationService = taxEstimationService;
    }

    /**
     * Displays the tax estimation page with the available modules and categories.
     *
     * @param model model used to provide form data and configuration options to the view
     * @return the tax estimation page
     */
    @GetMapping
    public String showTaxEstimationPage(Model model) {
        model.addAttribute("request", new TaxEstimationRequestDto());
        model.addAttribute("modules", taxEstimationService.getAvailableModules());
        model.addAttribute("categories", taxEstimationService.getAvailableCategories());
        return "tax/tax-estimation";
    }

    /**
     * Processes a tax estimation request and returns the page with the calculated result.
     *
     * @param request submitted tax estimation details
     * @param model model used to provide the request, result, and configuration options
     * @return the tax estimation page displaying the estimation result
     */
    @PostMapping("/estimate")
    public String estimateTax(@ModelAttribute("request") TaxEstimationRequestDto request, Model model) {

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        model.addAttribute("request", request);
        model.addAttribute("result", response);
        model.addAttribute("modules", taxEstimationService.getAvailableModules());
        model.addAttribute("categories", taxEstimationService.getAvailableCategories());

        return "tax/tax-estimation";
    }
}