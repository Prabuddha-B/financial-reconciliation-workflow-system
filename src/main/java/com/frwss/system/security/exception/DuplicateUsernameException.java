// ---------- Indicates a duplicate username ----------

package com.frwss.system.security.exception;



public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String message) {
        super(message);
    }

}