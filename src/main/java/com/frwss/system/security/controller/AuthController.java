package com.frwss.system.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "expired", required = false) String expired,
                        Model model) {

        if (error != null) {
            model.addAttribute("alertType", "danger");
            model.addAttribute("alertMessage", "Invalid username or password.");
        } else if (logout != null) {
            model.addAttribute("alertType", "success");
            model.addAttribute("alertMessage", "You have been logged out successfully.");
        } else if (expired != null) {
            model.addAttribute("alertType", "warning");
            model.addAttribute("alertMessage", "Your session expired. Please sign in again.");
        }

        return "security/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "security/access-denied";
    }
}

