package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SilentTimeoutEventPublisher extends ExecuteEventPublisher {
    private final ScheduledExecutorService timeoutScheduler;

    public SilentTimeoutEventPublisher (TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        executor = Executors.newFixedThreadPool(1);
        timeoutScheduler = Executors.newScheduledThreadPool(1);
        this.timeUnit = timeUnit;
    }

    public SilentTimeoutEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    public Future<?> publish(Event<?> event, long timeout, TimeUnit timeUnit) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        checkNotShutdown();

        Future<?> future = executor.submit(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                //log
            }
        });

        timeoutScheduler.schedule(() -> {
            if (!future.isDone()) {
                //log
                future.cancel(true);
            }
        }, timeout, timeUnit);

        return future;
    }

    public Future<?> publish(Event<?> event, long timeout) {
        return publish(event, timeout, timeUnit);
    }

    @Override
    public void shutdown() {
        checkNotShutdown();
        timeoutScheduler.shutdownNow();
    }
}
