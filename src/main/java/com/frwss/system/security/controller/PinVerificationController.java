package com.frwss.system.security.controller;

import com.frwss.system.security.PinVerificationSession;
import com.frwss.system.security.dto.PinVerificationRequest;
import com.frwss.system.security.service.UserManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/security")
public class PinVerificationController {

    private final UserManagementService userManagementService;
    private final PinVerificationSession pinVerificationSession;

    public PinVerificationController(
            UserManagementService userManagementService,
            PinVerificationSession pinVerificationSession) {

        this.userManagementService = userManagementService;
        this.pinVerificationSession = pinVerificationSession;
    }


//    Displays the PIN verification page
    @GetMapping("/verify-pin")
    public String verifyPinPage(Model model) {

        model.addAttribute(
                "pinVerificationRequest",
                new PinVerificationRequest()
        );

        return "security/verify-pin";
    }

//    Verifies the current user's PIN before executing the requested sensitive operation
    @PostMapping("/verify-pin")
    public String verifyPin(
            @ModelAttribute PinVerificationRequest request,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        try {

            boolean valid =
                    userManagementService.verifyCurrentUserPin(
                            request.getPin()
                    );

            if (!valid) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Invalid PIN."
                );

                return "redirect:/security/verify-pin";
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "PIN verified successfully."
            );

            // Mark the current session as PIN verified
            pinVerificationSession.markVerified(session);


            //    Retrieves the stored destination
            String redirectUrl =
                    pinVerificationSession.consumeRedirect(session);

            //    Redirects to the requested page
            if (redirectUrl != null &&
                    !redirectUrl.isBlank()) {

                System.out.println("Redirecting to: " + redirectUrl);

                return "redirect:" + redirectUrl;
            }

            //    Uses the default destination
            System.out.println("No redirect URL found.");

            return "redirect:/admin/users";



        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/security/verify-pin";
        }
    }

}