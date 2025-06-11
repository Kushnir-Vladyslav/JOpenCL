package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SilentTimeoutEventPublisher extends ExecuteEventPublisher {
    public SilentTimeoutEventPublisher (TimeUnit timeUnit) {
        executor = Executors.newFixedThreadPool(1);
        this.timeUnit = timeUnit;
    }

    public SilentTimeoutEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    public void publish(Event event, long timeout, TimeUnit timeUnit) {
        Future<?> future = executor.submit(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                //log
            }
        });

        executor.submit(() -> {
            try {
                future.get(timeout, timeUnit);
            } catch (Exception e) {
                //log
                future.cancel(true);
            }
        });
    }

    public void publish(Event event, long timeout) {
        publish(event, timeout, timeUnit);
    }
}
