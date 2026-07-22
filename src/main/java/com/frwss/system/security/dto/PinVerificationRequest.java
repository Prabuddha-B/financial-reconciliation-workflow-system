package com.frwss.system.security.dto;

// DTO used to verify a user's approval PIN
public class PinVerificationRequest {

    private String pin;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

}