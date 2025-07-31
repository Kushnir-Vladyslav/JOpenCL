package com.jopencl.Event.EventExceptions;

/**
 * Exception thrown when an event execution exceeds its timeout period.
 */
public class EventTimeoutException extends RuntimeException {

    public EventTimeoutException(String message) {
        super(message);
    }

    public EventTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
