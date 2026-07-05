package com.frwss.system.tax.controller;

import com.frwss.system.tax.model.TaxConfiguration;
import com.frwss.system.tax.service.TaxConfigurationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/tax-configurations")
public class AdminTaxConfigurationController {

    private final TaxConfigurationService taxConfigurationService;

    public AdminTaxConfigurationController(TaxConfigurationService taxConfigurationService) {
        this.taxConfigurationService = taxConfigurationService;
    }

    @GetMapping
    public String listConfigurations(Model model) {
        model.addAttribute("configurations",taxConfigurationService.getAllConfigurations());
        return "admin/tax-configurations/list";
    }

    @GetMapping("/{id}/edit")
    public String editConfiguration(@PathVariable Long id,Model model) {
        model.addAttribute("configuration",taxConfigurationService.getConfiguration(id));
        return "admin/tax-configurations/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateConfiguration(@PathVariable Long id,@ModelAttribute("configuration") TaxConfiguration configuration) {

        taxConfigurationService.updateTaxRate(
                id,
                configuration.getTaxRate(),
                configuration.getDescription()
        );

        return "redirect:/admin/tax-configurations";
    }
}