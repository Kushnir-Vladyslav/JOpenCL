package com.jopencl.Event.Events;

import com.jopencl.Event.Event;

import java.util.List;

public class ListEvents <T extends Event<?>> extends Event<List<T>> {
    private final Class<T> eventType;

    public ListEvents (Class<T> eventType, List<T> events) {
        this.eventType = eventType;
        data = events;
    }

    public Class<T> getEventType() {
        return eventType;
    }
}
