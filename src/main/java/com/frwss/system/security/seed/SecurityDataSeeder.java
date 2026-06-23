package com.frwss.system.security.seed;

import com.frwss.system.model.User;
import com.frwss.system.repository.UserRepository;
import com.frwss.system.security.model.Role;
import com.frwss.system.security.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class SecurityDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityDataSeeder(UserRepository userRepository,
                              RoleRepository roleRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role admin = createRole("ROLE_ADMIN", "System administrator");
        Role entry = createRole("ROLE_ENTRY_STAFF", "Data entry staff");
        Role auditor = createRole("ROLE_AUDITOR", "Audit user");

        createUser(1, "admin", "admin123", admin);
        createUser(2, "entry", "entry123", entry);
        createUser(3, "auditor", "auditor123", auditor);
    }

    private Role createRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    private void createUser(Integer id, String username, String password, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setRoles(Set.of(role));

        userRepository.save(user);
    }
}
