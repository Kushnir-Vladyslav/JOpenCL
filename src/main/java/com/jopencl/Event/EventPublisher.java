package com.jopencl.Event;


public abstract class EventPublisher {
    protected final EventManager eventManager = EventManager.getInstance();
    protected volatile Status status = Status.RUNNING;

    protected void publishEvent(Event<?> event) {
        eventManager.publish(event);
    }

    public Status getStatus() {
        return status;
    }

    public abstract void shutdown();
}

