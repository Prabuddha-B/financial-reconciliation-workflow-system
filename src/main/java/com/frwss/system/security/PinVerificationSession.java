package com.frwss.system.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class PinVerificationSession {

    // Session attribute keys
    private static final String REDIRECT_URL = "PIN_REDIRECT";
    private static final String PIN_VERIFIED = "PIN_VERIFIED";

    // ------------------------------------------------------------
    // Stores the destination after successful PIN verification
    // ------------------------------------------------------------
    public void storeRedirect(
            HttpSession session,
            String url) {

        session.setAttribute(
                REDIRECT_URL,
                url
        );
    }

    // ------------------------------------------------------------
    // Returns and clears the stored destination
    // ------------------------------------------------------------
    public String consumeRedirect(
            HttpSession session) {

        String url = (String) session.getAttribute(
                REDIRECT_URL
        );

        session.removeAttribute(
                REDIRECT_URL
        );

        return url;
    }

    // ------------------------------------------------------------
    // Marks the current session as PIN verified
    // ------------------------------------------------------------
    public void markVerified(HttpSession session) {

        session.setAttribute(
                PIN_VERIFIED,
                true
        );
    }

    // ------------------------------------------------------------
    // Checks whether the current session has already verified the PIN
    // ------------------------------------------------------------
    public boolean isVerified(HttpSession session) {

        Boolean verified = (Boolean) session.getAttribute(
                PIN_VERIFIED
        );

        return Boolean.TRUE.equals(verified);
    }

    // ------------------------------------------------------------
    // Clears the PIN verification flag
    // ------------------------------------------------------------
    public void clear(HttpSession session) {

        session.removeAttribute(
                PIN_VERIFIED
        );
    }

}