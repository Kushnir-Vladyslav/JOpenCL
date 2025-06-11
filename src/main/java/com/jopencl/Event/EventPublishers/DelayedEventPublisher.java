package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ScheduleEventPublisher;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DelayedEventPublisher extends ScheduleEventPublisher {
    public DelayedEventPublisher (TimeUnit timeUnit) {
        scheduler = Executors.newScheduledThreadPool(1);
        this.timeUnit = timeUnit;
    }

    public DelayedEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    public void publish(Event event, long delay, TimeUnit timeUnit) {
        scheduler.schedule(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                //log
            }
        }, delay, timeUnit);
    }

    public void publish(Event event, long delay) {
        publish(event, delay, timeUnit);
    }
}
