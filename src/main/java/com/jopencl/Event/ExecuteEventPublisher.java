package com.jopencl.Event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ExecuteEventPublisher extends EventPublisher{
    protected ExecutorService executor;
    protected TimeUnit timeUnit;

    public void shutdown() {
        executor.shutdown();
    }
}
