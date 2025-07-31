package com.jopencl.Event;

/**
 * Functional interface for handling errors that occur during event processing.
 * Provides a mechanism for custom error handling strategies.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see Event
 */
@FunctionalInterface
public interface EventErrorHandler {
    /**
     * Handles an error that occurred during event processing.
     *
     * @param event the event that caused the error
     * @param e the exception that occurred
     */
    void handle(Event<?> event, Exception e);
}
