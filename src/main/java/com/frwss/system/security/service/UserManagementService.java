//------------ Handles user management operations ------------------

package com.frwss.system.security.service;

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
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void createUser(CreateUserRequest request) {

        // Prevent duplicate usernames
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUsernameException("Username already exists.");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid role selected.")
                );

        User user = new User();

        Integer nextId = userRepository.findMaxUserId() + 1;

        user.setUserId(nextId);
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }

}