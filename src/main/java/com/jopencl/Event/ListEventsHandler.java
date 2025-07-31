package com.jopencl.Event;

import com.jopencl.Event.Events.ListEvents;

/**
 * Functional interface for handling lists of events.
 * Specializes {@link BaseEventHandler} for {@link ListEvents} type events.
 *
 * @param <T> the type of events in the list, must extend Event
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see BaseEventHandler
 * @see ListEvents
 */
@FunctionalInterface
public interface ListEventsHandler<T extends Event<?>> extends BaseEventHandler<ListEvents<T>> {
}
