package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;

import java.util.concurrent.*;

public class TimeoutEventPublisher extends ExecuteEventPublisher {
    public TimeoutEventPublisher (TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        executor = Executors.newFixedThreadPool(1);
        this.timeUnit = timeUnit;
    }

    public TimeoutEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    public void publish(Event<?> event, long timeout, TimeUnit timeUnit) {
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

        Future<?> future = executor.submit(() -> publishEvent(event));
        try {
            future.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            future.cancel(true);
            //log
            //throw
        } catch (Exception e) {
            //throw
        }
    }

    public void publish(Event<?> event, long timeout) {
        publish(event, timeout, timeUnit);
    }
}
