package com.jopencl.Event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class ExecuteEventPublisher extends EventPublisher{
    protected ExecutorService executor;
    protected TimeUnit timeUnit;

    @Override
    public void shutdown() {
        if(status != Status.SHUTDOWN) {
            status = Status.SHUTDOWN;
            if (executor != null) {
                executor.shutdownNow();
            }
        } else {
            throw new IllegalStateException(this.getClass().getSimpleName() + " was already disabled.");
        }
    }
}
