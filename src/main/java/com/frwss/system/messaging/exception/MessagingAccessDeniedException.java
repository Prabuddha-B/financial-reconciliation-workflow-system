package com.frwss.system.messaging.exception;

/** The logged-in user is not allowed to read, reply to or delete the message or notification. */
public class MessagingAccessDeniedException extends MessagingException {

    public MessagingAccessDeniedException(String message) {
        super(message);
    }
}
