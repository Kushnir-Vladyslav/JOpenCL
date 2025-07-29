package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;
import com.jopencl.Event.Status;

public class SyncEventPublisher extends EventPublisher {
    public void publish(Event<?> event) {
        checkNotShutdown();
        publishEvent(event);
    }

    @Override
    public void shutdown () {
        checkNotShutdown();
        status = Status.SHUTDOWN;
    }
}
