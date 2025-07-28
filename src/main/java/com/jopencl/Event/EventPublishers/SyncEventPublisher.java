package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;
import com.jopencl.Event.Status;

public class SyncEventPublisher extends EventPublisher {
    public void publish(Event<?> event) {
        if(status != Status.SHUTDOWN) {
            publishEvent(event);
        } else {
            throw new IllegalStateException("SyncEventPublisher was already disabled.");
        }
    }

    @Override
    public void shutdown () {
        if(status != Status.SHUTDOWN) {
            status = Status.SHUTDOWN;
        } else {
            throw new IllegalStateException("SyncEventPublisher was already disabled.");
        }
    }
}
