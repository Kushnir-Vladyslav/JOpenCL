package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.ControlledListFuture;
import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;

import java.util.concurrent.*;

public class SilentTimeoutEventPublisher extends ExecuteEventPublisher {
    private final ScheduledExecutorService timeoutScheduler;
    private final ControlledListFuture listFuture;

    public SilentTimeoutEventPublisher (TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        executor = Executors.newFixedThreadPool(1);
        listFuture = new ControlledListFuture();
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

        ScheduledFuture<?> scheduledFuture = timeoutScheduler.schedule(() -> {
            if (!future.isDone()) {
                //log
                future.cancel(true);
            }
        }, timeout, timeUnit);

        listFuture.add(future);
        listFuture.add(scheduledFuture);

        return future;
    }

    public Future<?> publish(Event<?> event, long timeout) {
        return publish(event, timeout, timeUnit);
    }

    @Override
    public void shutdown() {
        checkNotShutdown();
        timeoutScheduler.shutdownNow();
        listFuture.stopControlAndShutdown();
        super.shutdown();
    }
}
