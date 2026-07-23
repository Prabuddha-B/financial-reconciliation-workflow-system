//------------ Handles user management operations ------------------

package com.frwss.system.security.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.security.SecurityContextAdapter;
import com.frwss.system.security.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.frwss.system.model.User;
import org.springframework.transaction.annotation.Transactional;
import com.frwss.system.security.dto.CreateUserRequest;
import com.frwss.system.security.model.Role;
import com.frwss.system.security.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.frwss.system.security.exception.DuplicateUsernameException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextAdapter securityContextAdapter;


    public UserManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            SecurityContextAdapter securityContextAdapter
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.securityContextAdapter = securityContextAdapter;
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers(String username,String role,Boolean enabled) {

        return userRepository.findUsersByFilters(
                username,role,enabled);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return getAllUsers(null, null,null);
    }


//    Creates a new user
    @Transactional
    public void createUser(CreateUserRequest request) {

        // Prevent duplicate usernames
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUsernameException("Username already exists.");
        }

        // Validate PIN
        if (request.getPin() == null || request.getPin().isBlank()) {
            throw new IllegalArgumentException("PIN is required.");
        }

        if (request.getConfirmPin() == null || request.getConfirmPin().isBlank()) {
            throw new IllegalArgumentException("Confirm PIN is required.");
        }

        if (!request.getPin().matches("\\d{4,6}")) {
            throw new IllegalArgumentException(
                    "PIN must contain 4 to 6 digits."
            );
        }

        if (!request.getPin().equals(request.getConfirmPin())) {
            throw new IllegalArgumentException(
                    "PINs do not match."
            );
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid role selected.")
                );

        User user = new User();

        Integer nextId = userRepository.findMaxUserId() + 1;

        user.setUserId(nextId);
        user.setUsername(request.getUsername());

        // Encode password
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        // Encode PIN
        user.setPinHash(
                passwordEncoder.encode(request.getPin())
        );

        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        // Keep compatibility with existing code
        user.setRole(role.getName());

        user.setRoles(Set.of(role));

        userRepository.save(user);

        auditService.log(
                AuditActionType.USER_CREATED,
                "User",
                user.getUserId().toString(),
                securityContextAdapter.getCurrentUsername(),
                "Created user '" + user.getUsername()
                        + "' with role " + role.getName()
        );
    }

//    Returns a user by ID
    @Transactional(readOnly = true)
    public User getUserById(Integer userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );
    }


    //    Updates an existing user
    @Transactional
    public void updateUser(Integer userId, String roleName, boolean enabled) {

        User user = getUserById(userId);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new IllegalArgumentException("Role not found.")
                );

        String currentUser = securityContextAdapter.getCurrentUsername();

        if (user.getUsername().equals(currentUser) && !enabled) {
            throw new IllegalArgumentException(
                    "You cannot disable your own account."
            );
        }

        boolean roleChanged = !user.getRoles().contains(role);
        boolean statusChanged = user.isEnabled() != enabled;

        user.getRoles().clear();
        user.getRoles().add(role);
        user.setEnabled(enabled);

        userRepository.save(user);

        if (statusChanged) {

            auditService.log(
                    enabled
                            ? AuditActionType.USER_ENABLED
                            : AuditActionType.USER_DISABLED,
                    "User",
                    user.getUserId().toString(),
                    currentUser,
                    (enabled ? "Enabled " : "Disabled ")
                            + "user '" + user.getUsername() + "'"
            );

        } else if (roleChanged) {

            auditService.log(
                    AuditActionType.USER_UPDATED,
                    "User",
                    user.getUserId().toString(),
                    currentUser,
                    "Updated role for user '" + user.getUsername() + "'"
            );

        }
    }

    //    Resets a user's password
    @Transactional
    public void resetPassword(Integer userId,String password) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        user.setPassword(
                passwordEncoder.encode(password)
        );

        userRepository.save(user);

        auditService.log(
                AuditActionType.PASSWORD_RESET,
                "User",
                user.getUserId().toString(),
                securityContextAdapter.getCurrentUsername(),
                "Reset password for user '" + user.getUsername() + "'"
        );
    }

    //    Resets a user's PIN
    @Transactional
    public void resetPin(Integer userId,String pin) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        user.setPinHash(
                passwordEncoder.encode(pin)
        );

        userRepository.save(user);

        auditService.log(
                AuditActionType.PIN_RESET,
                "User",
                user.getUserId().toString(),
                securityContextAdapter.getCurrentUsername(),
                "Reset PIN for user '" + user.getUsername() + "'"
        );
    }

    @Transactional(readOnly = true)
    public boolean verifyCurrentUserPin(String pin) {

        String username = securityContextAdapter.getCurrentUsername();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Current user not found.")
                );

        if (user.getPinHash() == null) {
            throw new IllegalArgumentException(
                    "No approval PIN has been configured."
            );
        }

        return passwordEncoder.matches(
                pin,
                user.getPinHash()
        );
    }

    //    Returns enabled demo accounts with the same role as the current user
    @Transactional(readOnly = true)
    public List<User> getEligibleSwitchAccounts() {

        String currentUsername = securityContextAdapter.getCurrentUsername();
        String currentRole = securityContextAdapter.getCurrentUserRole();

        return userRepository.findEnabledUsersByRoleNameExcludingUsername(
                "ROLE_" + currentRole,
                currentUsername
        );
    }

    //    Validates the selected demo account before switching sessions
    @Transactional(readOnly = true)
    public User validateSwitchAccount(Integer userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Selected user not found.")
                );

        String currentUsername = securityContextAdapter.getCurrentUsername();
        String currentRole = "ROLE_"
                + securityContextAdapter.getCurrentUserRole();

        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException(
                    "You cannot switch to the current account."
            );
        }

        if (!user.isEnabled()) {
            throw new IllegalArgumentException(
                    "Selected user account is disabled."
            );
        }

        boolean hasSameRole = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(currentRole));

        if (!hasSameRole) {
            throw new IllegalArgumentException(
                    "You can only switch to an account with the same role."
            );
        }

        return user;
    }

    //    Verifies the selected user's PIN
    @Transactional(readOnly = true)
    public void verifySwitchAccountPin(Integer userId,String pin) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        if (!user.isEnabled()) {

            throw new IllegalArgumentException(
                    "Selected account is disabled."
            );
        }

        if (!passwordEncoder.matches(
                pin,
                user.getPinHash())) {

            throw new IllegalArgumentException(
                    "Invalid PIN."
            );
        }
    }

    // Enables a single user
    @Transactional
    public void enableUser(Integer userId) {

        User user = getUserById(userId);

        if (user.isEnabled()) {
            return;
        }

        user.setEnabled(true);

        userRepository.save(user);

        auditService.log(
                AuditActionType.USER_ENABLED,
                "User",
                user.getUserId().toString(),
                securityContextAdapter.getCurrentUsername(),
                "Enabled user '" + user.getUsername() + "'"
        );
    }

    // Disables a single user
    @Transactional
    public void disableUser(Integer userId) {

        User user = getUserById(userId);

        String currentUser = securityContextAdapter.getCurrentUsername();

        if (user.getUsername().equals(currentUser)) {
            throw new IllegalArgumentException(
                    "You cannot disable your own account."
            );
        }

        if (!user.isEnabled()) {
            return;
        }

        user.setEnabled(false);

        userRepository.save(user);

        auditService.log(
                AuditActionType.USER_DISABLED,
                "User",
                user.getUserId().toString(),
                currentUser,
                "Disabled user '" + user.getUsername() + "'"
        );
    }

    // Enables multiple user
    @Transactional
    public void bulkEnable(List<Integer> userIds) {
        for (Integer userId : userIds) {
            enableUser(userId);
        }
    }

    // Disables multiple users
    @Transactional
    public void bulkDisable(List<Integer> userIds) {
        for (Integer userId : userIds) {
            disableUser(userId);
        }
    }

    // Bulk password reset
    @Transactional
    public void bulkResetPassword(List<Integer> userIds,String temporaryPassword) {

        String currentUser = securityContextAdapter.getCurrentUsername();

        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {

            user.setPassword(
                    passwordEncoder.encode(
                            temporaryPassword
                    )
            );

            auditService.log(
                    AuditActionType.PASSWORD_RESET,
                    "User",
                    user.getUserId().toString(),
                    currentUser,
                    "Bulk reset password for user '" + user.getUsername() + "'"
            );
        }

        userRepository.saveAll(users);
    }
}
