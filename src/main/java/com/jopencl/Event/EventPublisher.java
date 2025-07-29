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

    protected void checkNotShutdown () {
        if (status == Status.SHUTDOWN) {
            //log
            throw new IllegalStateException(this.getClass().getSimpleName() + " was already disabled.");
        }
    }

    public abstract void shutdown();
}

