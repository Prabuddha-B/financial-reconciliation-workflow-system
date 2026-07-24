package com.frwss.system.admin.controller;

import com.frwss.system.admin.service.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * ------------------------------------------------------------
 * Admin Dashboard Controller
 * ------------------------------------------------------------
 * Handles requests for the Administrator Dashboard.
 * ------------------------------------------------------------
 */
@Controller
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ------------------------------------------------------------
    // Display Admin Dashboard
    // ------------------------------------------------------------
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        model.addAttribute(
                "dashboard",
                dashboardService.getDashboardData()
        );

        return "admin/dashboard";
    }

}