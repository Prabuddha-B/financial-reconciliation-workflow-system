// -------------------------------------------------------------------------------
//       Transfers the new PIN submitted for a user PIN reset operation
// -------------------------------------------------------------------------------

package com.frwss.system.security.dto;

// DTO for resetting a user's PIN
public class ResetPinRequest {

    private String pin;

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

}