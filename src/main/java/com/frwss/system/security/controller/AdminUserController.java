package com.frwss.system.security.controller;

import com.frwss.system.security.exception.DuplicateUsernameException;
import com.frwss.system.security.service.UserManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.frwss.system.security.dto.CreateUserRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * Creates a new system user.
     */
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
}