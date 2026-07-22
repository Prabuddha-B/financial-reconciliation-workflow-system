package com.frwss.system.security.controller;

import com.frwss.system.security.exception.DuplicateUsernameException;
import com.frwss.system.security.service.UserManagementService;
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

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
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

    //    Displays the reset password page
    @GetMapping("/{id}/reset-password")
    public String resetPasswordPage(
            @PathVariable Integer id,
            Model model) {

        model.addAttribute(
                "user",
                userManagementService.getUserById(id)
        );

        return "admin/users/reset-password";
    }

    //    Resets the user's password
    @PostMapping("/{id}/reset-password")
    public String resetPassword(
            @PathVariable Integer id,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {

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

}