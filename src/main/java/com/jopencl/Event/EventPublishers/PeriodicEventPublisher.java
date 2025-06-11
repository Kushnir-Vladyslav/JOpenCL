package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ScheduleEventPublisher;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PeriodicEventPublisher extends ScheduleEventPublisher {
    private Map<String, ScheduledFuture<?>> scheduledTasks;

    public PeriodicEventPublisher (TimeUnit timeUnit) {
        scheduler = Executors.newScheduledThreadPool(1);
        this.timeUnit = timeUnit;
    }

    public PeriodicEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    public void publish(Event event, String identifier, long period, TimeUnit timeUnit) {
        ScheduledFuture<?> task = scheduledTasks.get(identifier);
        if (task != null) {
            //log
            task.cancel(false);
        }

        task = scheduler.scheduleAtFixedRate(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                //log
            }
        }, 0, period, timeUnit);

        scheduledTasks.put(identifier, task);
    }

    public void publish(Event event, String identifier, long period) {
        publish(event, identifier, period, timeUnit);
    }

    public void cancel (Event event) {
        ScheduledFuture<?> task = scheduledTasks.remove(event);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    public void shutdown () {
        for (ScheduledFuture<?> task : scheduledTasks.values()) {
            task.cancel(false);
        }
        super.shutdown();
    }
}
