// -------------------------------------------------------------------------------
//       Transfers and validates data submitted when creating a new user account
// -------------------------------------------------------------------------------
package com.frwss.system.security.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateUserRequest {

    // Username provided for the new user account.
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String role;

    @NotBlank
    private String pin;

    // Confirms the initial security PIN entered during account creation.
    @NotBlank
    private String confirmPin;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getConfirmPin() {
        return confirmPin;
    }

    public void setConfirmPin(String confirmPin) {
        this.confirmPin = confirmPin;
    }
}