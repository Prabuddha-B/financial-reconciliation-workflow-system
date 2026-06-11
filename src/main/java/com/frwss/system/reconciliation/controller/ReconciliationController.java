package com.frwss.system.reconciliation.controller;

import com.frwss.system.reconciliation.model.ReconciliationStatus;
import com.frwss.system.reconciliation.service.ReconciliationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService){
        this.reconciliationService = reconciliationService;
    }

    @GetMapping
    public String showReconciliationDashboard(Model model){
        model.addAttribute("results", reconciliationService.getAllResults());
        model.addAttribute("exactResults", reconciliationService.getResultsByStatus(ReconciliationStatus.EXACT));
        model.addAttribute("orphanResults", reconciliationService.getResultsByStatus(ReconciliationStatus.ORPHAN));
        model.addAttribute("partialResults", reconciliationService.getResultsByStatus(ReconciliationStatus.PARTIAL));

        return "reconciliation/dashboard";
    }

    @PostMapping("/run-exact")
    public String runExactReconciliation(RedirectAttributes redirectAttributes){
        // temporary user until Spring Security is fully integrated
        reconciliationService.runExactReconciliation("admin");

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Exact reconciliation completed successfully."
        );

        return "redirect:/reconciliation";
    }

    @PostMapping("/run-partial")
    public String runPartialReconciliation(RedirectAttributes redirectAttributes){
        reconciliationService.runPartialReconciliation("admin", 3);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Partial reconciliation completed successfully with ±3 days tolerance."
        );

        return "redirect:/reconciliation";
    }
}
