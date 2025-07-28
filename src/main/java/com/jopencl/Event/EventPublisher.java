package com.jopencl.Event;


public abstract class EventPublisher {
    protected final EventManager eventManager = EventManager.getInstance();

    protected void publishEvent(Event event) {
        eventManager.publish(event);
    }

    public abstract void shutdown();
}

