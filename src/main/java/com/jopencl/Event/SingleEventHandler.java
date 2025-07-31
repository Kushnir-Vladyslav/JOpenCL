package com.jopencl.Event;

/**
 * Functional interface for handling individual events.
 * Specializes {@link BaseEventHandler} for processing single events.
 *
 * @param <T> the type of event to handle, must extend Event
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see BaseEventHandler
 */
@FunctionalInterface
public interface SingleEventHandler<T extends Event<?>> extends BaseEventHandler<T> {
}
