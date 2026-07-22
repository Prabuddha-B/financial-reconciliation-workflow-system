package com.frwss.system.security.controller;

import com.frwss.system.security.PinVerificationSession;
import com.frwss.system.security.dto.ResetPinRequest;
import com.frwss.system.security.exception.DuplicateUsernameException;
import com.frwss.system.security.service.UserManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.frwss.system.security.dto.CreateUserRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;
    private final PinVerificationSession pinVerificationSession;

    public AdminUserController(
            UserManagementService userManagementService,
            PinVerificationSession pinVerificationSession) {

        this.userManagementService = userManagementService;
        this.pinVerificationSession = pinVerificationSession;
    }

    @GetMapping
    public String listUsers(Model model) {

        model.addAttribute(
                "users",
                userManagementService.getAllUsers()
        );

        return "admin/users/list";
    }

    @GetMapping("/create")
    public String createUserForm(Model model) {

        model.addAttribute(
                "createUserRequest",
                new CreateUserRequest()
        );

        return "admin/users/create";
    }

//    Creates a new system user
    @PostMapping("/create")
    public String createUser(
            @ModelAttribute("createUserRequest") CreateUserRequest request,
            RedirectAttributes redirectAttributes) {

        try {

            userManagementService.createUser(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User created successfully."
            );

            return "redirect:/admin/users";

        } catch ( DuplicateUsernameException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/admin/users/create";
        }
    }

//    Displays the edit user page
    @GetMapping("/{id}/edit")
    public String editUser(
            @PathVariable Integer id,
            Model model) {

        model.addAttribute(
                "user",
                userManagementService.getUserById(id)
        );

        return "admin/users/edit";
    }

    //    Updates an existing user
    @PostMapping("/{id}/edit")
    public String updateUser(
            @PathVariable Integer id,
            @RequestParam String role,
            @RequestParam boolean enabled,
            RedirectAttributes redirectAttributes) {

        try {

            userManagementService.updateUser(
                    id,
                    role,
                    enabled
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User updated successfully."
            );

            return "redirect:/admin/users";

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    ex.getMessage()
            );

            return "redirect:/admin/users/" + id + "/edit";
        }
    }



    //    Displays the password reset form
    @GetMapping("/{id}/reset-password/form")
    public String resetPasswordForm(
            @PathVariable Integer id,
            Model model,
            HttpSession session) {

        //    Redirects if PIN is not verified
        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(
                    session,
                    "/admin/users/" + id + "/reset-password/form"
            );

            return "redirect:/security/verify-pin";
        }

        //    Clears the verification flag
        pinVerificationSession.clear(session);

        model.addAttribute(
                "user",
                userManagementService.getUserById(id)
        );

        return "admin/users/reset-password";
    }




    //    Resets the user's password after successful PIN verification
    @PostMapping("/{id}/reset-password/form")
    public String resetPassword(
            @PathVariable Integer id,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        //    Prevents direct POST requests without PIN verification
        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(
                    session,
                    "/admin/users/" + id + "/reset-password/form"
            );

            return "redirect:/security/verify-pin";
        }

        //    Consumes the verification
        pinVerificationSession.clear(session);

        userManagementService.resetPassword(
                id,
                password
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Password reset successfully."
        );

        return "redirect:/admin/users";
    }

    //    Displays the reset PIN page
    @GetMapping("/{id}/reset-pin")
    public String resetPinPage(
            @PathVariable Integer id,
            Model model,
            HttpSession session) {

        //    Redirects to PIN verification if not already verified
        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(
                    session,
                    "/admin/users/" + id + "/reset-pin"
            );

            return "redirect:/security/verify-pin";
        }

        //    Consumes the verification so it cannot be reused
        pinVerificationSession.clear(session);

        model.addAttribute(
                "user",
                userManagementService.getUserById(id)
        );

        model.addAttribute(
                "resetPinRequest",
                new ResetPinRequest()
        );

        return "admin/users/reset-pin";
    }

    //    Resets the user's PIN after successful PIN verification
    @PostMapping("/{id}/reset-pin")
    public String resetPin(
            @PathVariable Integer id,
            @ModelAttribute ResetPinRequest resetPinRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        //    Prevents direct POST requests without PIN verification
        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(
                    session,
                    "/admin/users/" + id + "/reset-pin"
            );

            return "redirect:/security/verify-pin";
        }

        //    Consumes the verification
        pinVerificationSession.clear(session);

        userManagementService.resetPin(
                id,
                resetPinRequest.getPin()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "PIN reset successfully."
        );

        return "redirect:/admin/users";
    }

}