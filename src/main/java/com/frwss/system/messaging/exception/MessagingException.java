package com.frwss.system.messaging.exception;

/**
 * Base class for errors raised by the messaging and notification services (for example invalid
 * input). It extends RuntimeException, so existing code that catches RuntimeException keeps working.
 */
public class MessagingException extends RuntimeException {

    public MessagingException(String message) {
        super(message);
    }
}
