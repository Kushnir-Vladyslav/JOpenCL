package com.jopencl.Event;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventManager;

public abstract class EventPublisher {
    protected final EventManager eventManager = EventManager.getInstance();

    protected void publishEvent(Event event) {
        eventManager.publish(event);
    }
}

