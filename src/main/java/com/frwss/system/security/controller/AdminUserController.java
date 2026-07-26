package com.frwss.system.security.controller;

import com.frwss.system.security.PinVerificationSession;
import com.frwss.system.security.dto.CreateUserRequest;
import com.frwss.system.security.dto.ResetPinRequest;
import com.frwss.system.security.exception.DuplicateUsernameException;
import com.frwss.system.security.service.UserManagementService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.util.stream.Collectors;


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


//    Displays all users with optional filters
    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            Model model) {

        model.addAttribute(
                "users",
                userManagementService.getAllUsers(username,role,enabled));

        model.addAttribute("searchUsername", username);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", enabled);

        return "admin/users/list";
    }


//    Displays create user page
    @GetMapping("/create")
    public String createUserForm(Model model) {
        model.addAttribute("createUserRequest",new CreateUserRequest());
        return "admin/users/create";
    }


//    Creates a new user
    @PostMapping("/create")
    public String createUser(
            @ModelAttribute("createUserRequest") CreateUserRequest request,
            RedirectAttributes redirectAttributes) {

        try {

            userManagementService.createUser(request);
            redirectAttributes.addFlashAttribute("successMessage","User created successfully.");
            return "redirect:/admin/users";

        } catch (DuplicateUsernameException | IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute("errorMessage",ex.getMessage());
            return "redirect:/admin/users/create";
        }
    }


//    Displays edit user page
    @GetMapping("/{id}/edit")
    public String editUser(
            @PathVariable Integer id,
            Model model) {

        model.addAttribute("user",userManagementService.getUserById(id));
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

            userManagementService.updateUser(id,role,enabled);

            redirectAttributes.addFlashAttribute("successMessage","User updated successfully.");
            return "redirect:/admin/users";

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute("errorMessage",ex.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
    }


    // Enables a single user
    @PostMapping("/{id}/enable")
    public String enableUser(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            userManagementService.enableUser(id);
            redirectAttributes.addFlashAttribute("successMessage","User enabled successfully.");

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",ex.getMessage());
        }
        return "redirect:/admin/users";
    }


    // Disables a single user
    @PostMapping("/{id}/disable")
    public String disableUser(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {

        try {

            userManagementService.disableUser(id);
            redirectAttributes.addFlashAttribute("successMessage","User disabled successfully.");

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",ex.getMessage());
        }
        return "redirect:/admin/users";
    }


//    Displays password reset form
    @GetMapping("/{id}/reset-password/form")
    public String resetPasswordForm(
            @PathVariable Integer id,
            Model model,
            HttpSession session) {

        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(session, "/admin/users/" + id + "/reset-password/form");
            return "redirect:/security/verify-pin";
        }

        model.addAttribute("user",userManagementService.getUserById(id));
        return "admin/users/reset-password";
    }


//    Resets user's password
    @PostMapping("/{id}/reset-password/form")
    public String resetPassword(
            @PathVariable Integer id,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(
                    session,
                    "/admin/users/" + id + "/reset-password/form"
            );

            return "redirect:/security/verify-pin";
        }

        userManagementService.resetPassword(
                id,
                password
        );

        pinVerificationSession.clear(session);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Password reset successfully."
        );

        return "redirect:/admin/users";
    }


//    Displays reset PIN page
    @GetMapping("/{id}/reset-pin")
    public String resetPinPage(
            @PathVariable Integer id,
            Model model,
            HttpSession session) {

        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(
                    session,
                    "/admin/users/" + id + "/reset-pin"
            );

            return "redirect:/security/verify-pin";
        }

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


    // Resets user's PIN
    @PostMapping("/{id}/reset-pin")
    public String resetPin(
            @PathVariable Integer id,
            @ModelAttribute ResetPinRequest resetPinRequest,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!pinVerificationSession.isVerified(session)) {

            pinVerificationSession.storeRedirect(
                    session,
                    "/admin/users/" + id + "/reset-pin"
            );

            return "redirect:/security/verify-pin";
        }

        userManagementService.resetPin(
                id,
                resetPinRequest.getPin()
        );

        pinVerificationSession.clear(session);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "PIN reset successfully."
        );

        return "redirect:/admin/users";
    }

    // Bulk Enable Users
    @PostMapping("/bulk/enable")
    public String bulkEnable(
            @RequestParam("userIds") String userIds,
            RedirectAttributes redirectAttributes) {

        List<Integer> ids = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        userManagementService.bulkEnable(ids);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                ids.size() + " user(s) enabled successfully."
        );

        return "redirect:/admin/users";
    }

    // Bulk Disable Users
    @PostMapping("/bulk/disable")
    public String bulkDisable(
            @RequestParam("userIds") String userIds,
            RedirectAttributes redirectAttributes) {

        List<Integer> ids = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        userManagementService.bulkDisable(ids);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                ids.size() + " user(s) disabled successfully."
        );

        return "redirect:/admin/users";
    }

    // Bulk Reset Password
    @PostMapping("/bulk/reset-password")
    public String bulkResetPassword(
            @RequestParam("userIds") String userIds,
            RedirectAttributes redirectAttributes) {

        List<Integer> ids = Arrays.stream(userIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        userManagementService.bulkResetPassword(
                ids,
                "ChangeMe123!"
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                ids.size() + " password(s) reset successfully."
        );

        return "redirect:/admin/users";
    }

}