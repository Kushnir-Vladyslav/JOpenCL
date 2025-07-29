package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ScheduleEventPublisher;
import com.jopencl.Event.Status;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DelayedEventPublisher extends ScheduleEventPublisher {
    public DelayedEventPublisher (TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        scheduler = Executors.newScheduledThreadPool(1);
        this.timeUnit = timeUnit;
    }

    public DelayedEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> publish(Event<?> event, long delay, TimeUnit timeUnit) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (delay < 0) {
            throw new IllegalArgumentException("Delayed cannot be negative");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        checkNotShutdown();

        return scheduler.schedule(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                //log
            }
        }, delay, timeUnit);
    }

    public ScheduledFuture<?> publish(Event<?> event, long delay) {
        return publish(event, delay, timeUnit);
    }
}
