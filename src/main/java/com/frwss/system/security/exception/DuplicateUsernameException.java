// -------------------------------------------------------------------------------
//       Represents an error caused by attempting to register an existing username
// -------------------------------------------------------------------------------
package com.frwss.system.security.exception;

public class DuplicateUsernameException extends RuntimeException {

    /**
     * Creates a duplicate username exception with a descriptive error message.
     */
    public DuplicateUsernameException(String message) {
        super(message);
    }

}