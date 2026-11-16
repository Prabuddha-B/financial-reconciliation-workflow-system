// -------------------------------------------------------------------------------
//       Handles requests for the Administrator Dashboard
// -------------------------------------------------------------------------------
package com.frwss.system.admin.controller;

import com.frwss.system.admin.service.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    // Constructor
    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retrieves dashboard summary information and recent activities,
     * adds them to the model, and returns the administrator dashboard view.
     */
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        model.addAttribute(
                "dashboard",
                dashboardService.getDashboardData()
        );
        model.addAttribute(
                "recentActivities",
                dashboardService.getRecentActivities()
        );

        return "admin/dashboard";
    }

}
