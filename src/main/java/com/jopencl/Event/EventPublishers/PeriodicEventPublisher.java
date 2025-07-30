package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ScheduleEventPublisher;
import com.jopencl.Event.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PeriodicEventPublisher extends ScheduleEventPublisher {
    private final Map<String, ScheduledFuture<?>> scheduledTasks;

    public PeriodicEventPublisher (TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        scheduler = Executors.newScheduledThreadPool(1);
        scheduledTasks = new HashMap<>();
        this.timeUnit = timeUnit;
    }

    public PeriodicEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> publish(Event<?> event, String identifier, long period, TimeUnit timeUnit) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (identifier == null) {
            throw new IllegalArgumentException("Identifier cannot be null");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive");
        }
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        checkNotShutdown();

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
        return task;
    }

    public ScheduledFuture<?> publish(Event<?> event, String identifier, long period) {
        return publish(event, identifier, period, timeUnit);
    }

    public void cancel (String identifier) {
        checkNotShutdown();

        if (identifier == null) {
            throw new IllegalArgumentException("Identifier cannot be null");
        }

        ScheduledFuture<?> task = scheduledTasks.remove(identifier);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    public void shutdown () {
        checkNotShutdown();
        for (ScheduledFuture<?> task : scheduledTasks.values()) {
            task.cancel(false);
        }
        scheduledTasks.clear();
        super.shutdown();
    }
}
