package com.jopencl.Event;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ScheduleEventPublisher extends EventPublisher {
    protected ScheduledExecutorService scheduler;
    protected TimeUnit timeUnit;

    @Override
    public void shutdown () {
        scheduler.shutdown();
    }
}
