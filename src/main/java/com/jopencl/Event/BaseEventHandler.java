package com.jopencl.Event;

/**
 * Base functional interface for event handlers in the event system.
 * Defines the contract for handling events of a specific type.
 *
 * @param <T> the type of event to handle, must extend Event
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see Event
 */
@FunctionalInterface
public interface BaseEventHandler <T extends Event<?>>{
    /**
     * Handles an event of the specified type.
     *
     * @param event the event to handle
     */
    void handle(T event);
}
