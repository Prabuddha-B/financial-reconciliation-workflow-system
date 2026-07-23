package com.frwss.system.security.controller;

import com.frwss.system.model.User;
import com.frwss.system.security.dto.SwitchAccountRequest;
import com.frwss.system.security.service.DemoAccountSwitchService;
import com.frwss.system.security.service.UserManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/security")
public class SwitchAccountController {

    private final UserManagementService userManagementService;
    private final DemoAccountSwitchService demoAccountSwitchService;

    public SwitchAccountController(
            UserManagementService userManagementService,
            DemoAccountSwitchService demoAccountSwitchService) {

        this.userManagementService = userManagementService;
        this.demoAccountSwitchService = demoAccountSwitchService;
    }


//    Displays eligible accounts for demo account switching
    @GetMapping("/switch-account")
    public String switchAccountPage(Model model) {
        model.addAttribute("switchAccountRequest",new SwitchAccountRequest());
        model.addAttribute("eligibleAccounts",userManagementService.getEligibleSwitchAccounts());
        return "security/switch-account";
    }

//    Switches to another enabled account within the same role
    @PostMapping("/switch-account")
    public String switchAccount(
            @ModelAttribute SwitchAccountRequest request,
            RedirectAttributes redirectAttributes) {

        try {

            // Validate the selected account
            User targetUser = userManagementService.validateSwitchAccount(request.getUserId());
            // Verify the selected account's PIN
            userManagementService.verifySwitchAccountPin(request.getUserId(),request.getPin());

            // Replace the authenticated session
            demoAccountSwitchService.switchAccount(targetUser);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Successfully switched to '"
                            + targetUser.getUsername()
                            + "'.");

            return "redirect:/";

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute("errorMessage",ex.getMessage());
            return "redirect:/security/switch-account";
        }
    }

}