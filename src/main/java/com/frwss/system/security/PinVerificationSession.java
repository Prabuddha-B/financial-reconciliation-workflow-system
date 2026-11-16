// -------------------------------------------------------------------------------
//       Manages PIN verification status and redirect information in user sessions
// -------------------------------------------------------------------------------

package com.frwss.system.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class PinVerificationSession {

    private static final String REDIRECT_URL = "PIN_REDIRECT";
    private static final String PIN_VERIFIED = "PIN_VERIFIED";


    /**
     * Stores the original destination so the user can return to it
     * after successfully completing PIN verification.
     */
    public void storeRedirect(HttpSession session,String url) {

        session.setAttribute(REDIRECT_URL,url);
    }

    /**
     * Retrieves and removes the stored redirect destination
     * from the current HTTP session.
     */
    public String consumeRedirect(HttpSession session) {
        String url = (String) session.getAttribute(REDIRECT_URL);
        session.removeAttribute(REDIRECT_URL);
        return url;
    }

    /**
     * Marks the current HTTP session as successfully PIN-verified.
     */
    public void markVerified(HttpSession session) {
        session.setAttribute(PIN_VERIFIED,true);
    }

    /**
     * Checks whether the current HTTP session has a valid PIN verification flag.
     */
    public boolean isVerified(HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute(PIN_VERIFIED);
        return Boolean.TRUE.equals(verified);
    }

    /**
     * Removes the PIN verification status from the current HTTP session.
     */
    public void clear(HttpSession session) {
        session.removeAttribute(PIN_VERIFIED);
    }

}