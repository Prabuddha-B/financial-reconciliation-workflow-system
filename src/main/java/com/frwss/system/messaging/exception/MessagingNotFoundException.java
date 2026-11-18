package com.frwss.system.messaging.exception;

/** A message, notification or user the request refers to does not exist. */
public class MessagingNotFoundException extends MessagingException {

    public MessagingNotFoundException(String message) {
        super(message);
    }
}
