package com.frwss.system.tax.controller;

import com.frwss.system.tax.model.TaxConfiguration;
import com.frwss.system.tax.service.TaxConfigurationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.frwss.system.tax.exception.DuplicateTaxConfigurationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

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

    @GetMapping("/create")
    public String createConfigurationForm(Model model) {

        model.addAttribute("configuration", new TaxConfiguration());

        return "admin/tax-configurations/create";
    }



    @PostMapping("/create")
    public String createConfiguration(
            @Valid @ModelAttribute("configuration") TaxConfiguration configuration,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "admin/tax-configurations/create";
        }

        try {

            taxConfigurationService.createConfiguration(configuration);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tax configuration created successfully."
            );

            return "redirect:/admin/tax-configurations";

        } catch (DuplicateTaxConfigurationException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/admin/tax-configurations/create";
        }
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateConfiguration(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        taxConfigurationService.deactivateConfiguration(id);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Tax configuration deactivated successfully."
        );

        return "redirect:/admin/tax-configurations";
    }


    @PostMapping("/{id}/activate")
    public String activateConfiguration(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            taxConfigurationService.activateConfiguration(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tax configuration activated successfully."
            );

        } catch (DuplicateTaxConfigurationException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

        }

        return "redirect:/admin/tax-configurations";
    }

    @GetMapping("/{id}/edit")
    public String editConfiguration(@PathVariable Long id,Model model) {
        model.addAttribute("configuration",taxConfigurationService.getConfiguration(id));
        return "admin/tax-configurations/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateConfiguration(
            @PathVariable Long id,
            @Valid @ModelAttribute("configuration") TaxConfiguration configuration,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "admin/tax-configurations/edit";
        }

        taxConfigurationService.updateTaxRate(
                id,
                configuration.getTaxRate(),
                configuration.getDescription()
        );

        return "redirect:/admin/tax-configurations";
    }
}