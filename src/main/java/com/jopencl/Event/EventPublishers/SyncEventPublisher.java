package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;

public class SyncEventPublisher extends EventPublisher {
    public void publish(Event<?> event) {
        publishEvent(event);
    }

    @Override
    public void shutdown () {}
}
