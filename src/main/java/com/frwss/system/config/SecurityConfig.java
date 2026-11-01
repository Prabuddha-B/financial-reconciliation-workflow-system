package com.frwss.system.config;

import org.springframework.http.HttpMethod;
import com.frwss.system.security.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/ingestion/upload", "/ingestion/preview", "/ingestion/save",
                                "/ingestion/manual/**", "/ingestion/bulk-upload", "/upload-csv")
                        .hasAnyRole("ENTRY_STAFF", "ADMIN")
                        // Actions that change money or reconciliation data: entry staff and admin only.
                        // Reading (GET) stays open to every logged-in user so auditors can still view.
                        .requestMatchers(HttpMethod.POST, "/payment/**", "/reconciliation/**")
                        .hasAnyRole("ENTRY_STAFF", "ADMIN")
                        .requestMatchers("/audit/**").hasAnyRole("AUDITOR", "ADMIN")
                        .requestMatchers("/workflow/**")
                        .hasAnyRole("APPROVER", "AUDITOR", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/access-denied")
                )
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login?expired=true")
                        .maximumSessions(1)
                )
                .userDetailsService(userDetailsService)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Provides Spring Security's AuthenticationManager
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)throws Exception {
        return configuration.getAuthenticationManager();
    }
}
