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

    public TaxEstimationController(TaxEstimationService taxEstimationService) {
        this.taxEstimationService = taxEstimationService;
    }

    @GetMapping
    public String showTaxEstimationPage(Model model) {
        model.addAttribute("request", new TaxEstimationRequestDto());
        model.addAttribute("modules", taxEstimationService.getAvailableModules());
        model.addAttribute("categories", taxEstimationService.getAvailableCategories());
        return "tax/tax-estimation";
    }

    @PostMapping("/estimate")
    public String estimateTax(
            @ModelAttribute("request") TaxEstimationRequestDto request, Model model) {

        TaxEstimationResponseDto response = taxEstimationService.estimateTax(request);

        model.addAttribute("request", request);
        model.addAttribute("result", response);
        model.addAttribute("modules", taxEstimationService.getAvailableModules());
        model.addAttribute("categories", taxEstimationService.getAvailableCategories());

        return "tax/tax-estimation";
    }
}