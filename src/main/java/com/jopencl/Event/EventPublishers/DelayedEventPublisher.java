package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.ControlledListFuture;
import com.jopencl.Event.Event;
import com.jopencl.Event.ScheduleEventPublisher;
import com.jopencl.Event.Status;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DelayedEventPublisher extends ScheduleEventPublisher {
    private final ControlledListFuture listFuture;

    public DelayedEventPublisher (TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        scheduler = Executors.newScheduledThreadPool(1);
        listFuture = new ControlledListFuture();
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

        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                //log
            }
        }, delay, timeUnit);

        listFuture.add(scheduledFuture);

        return scheduledFuture;
    }

    public ScheduledFuture<?> publish(Event<?> event, long delay) {
        return publish(event, delay, timeUnit);
    }

    @Override
    public void shutdown() {
        checkNotShutdown();
        listFuture.stopControlAndShutdown();
        super.shutdown();
    }
}
