package com.jopencl.Event.Events;

import com.jopencl.Event.Event;

import java.util.List;

/**
 * A specialized event type that contains a list of events of a specific type.
 * This class allows for batch processing of multiple events of the same type.
 *
 * @param <T> the type of events contained in the list, must extend Event
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see Event
 */
public class ListEvents <T extends Event<?>> extends Event<List<T>> {
    /** The class object representing the type of events in the list */
    private final Class<T> eventType;

    /**
     * Creates a new ListEvents instance with the specified event type and list of events.
     *
     * @param eventType the class object representing the type of events in the list
     * @param events the list of events to be contained in this ListEvents
     */
    public ListEvents (Class<T> eventType, List<T> events) {
        this.eventType = eventType;
        data = events;
    }

    /**
     * Returns the class object representing the type of events in the list.
     *
     * @return the event type class
     */
    public Class<T> getEventType() {
        return eventType;
    }
}
