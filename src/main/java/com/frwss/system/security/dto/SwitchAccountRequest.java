package com.frwss.system.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


// Request used for demo account switching
public class SwitchAccountRequest {

    // Selected target user
    @NotNull
    private Integer userId;

    // Target user's PIN
    @NotBlank
    private String pin;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}