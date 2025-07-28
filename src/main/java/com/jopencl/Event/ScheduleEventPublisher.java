package com.jopencl.Event;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ScheduleEventPublisher extends EventPublisher {
    protected ScheduledExecutorService scheduler;
    protected TimeUnit timeUnit;

    @Override
    public void shutdown () {
        if(status != Status.SHUTDOWN) {
            status = Status.SHUTDOWN;
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        } else {
            throw new IllegalStateException(this.getClass().getSimpleName() + " was already disabled.");
        }
    }
}
