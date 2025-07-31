package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ScheduleEventPublisher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event publisher that schedules events for periodic publication.
 * Extends {@link ScheduleEventPublisher} to provide recurring event publishing capabilities.
 *
 * <p>This publisher:
 * <ul>
 * <li>Supports multiple concurrent periodic tasks</li>
 * <li>Allows identification and management of tasks by unique identifiers</li>
 * <li>Provides ability to cancel specific periodic publications</li>
 * <li>Maintains clean shutdown with task cancellation</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ScheduleEventPublisher
 */
public class PeriodicEventPublisher extends ScheduleEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicEventPublisher.class);

    /** Maps task identifiers to their scheduled futures */
    private final Map<String, ScheduledFuture<?>> scheduledTasks;

    /**
     * Creates a new PeriodicEventPublisher with specified time unit.
     *
     * @param timeUnit the default time unit for periods
     * @throws IllegalArgumentException if timeUnit is null
     */
    public PeriodicEventPublisher (TimeUnit timeUnit) {
        super();
        if (timeUnit == null) {
            LOGGER.error("Attempted to create publisher with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduledTasks = new HashMap<>();
        this.timeUnit = timeUnit;

        LOGGER.debug("Created PeriodicEventPublisher with timeUnit={}", timeUnit);
    }

    /**
     * Creates a new PeriodicEventPublisher with milliseconds as default time unit.
     */
    public PeriodicEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules an event for periodic publication.
     *
     * @param event the event to publish periodically
     * @param identifier unique identifier for this periodic task
     * @param period the period between publications
     * @param timeUnit the time unit of the period
     * @return a ScheduledFuture representing the periodic task
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws IllegalStateException if publisher is shut down
     */
    public ScheduledFuture<?> publish(Event<?> event, String identifier, long period, TimeUnit timeUnit) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event");
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (identifier == null) {
            LOGGER.error("Attempted to publish with null identifier");
            throw new IllegalArgumentException("Identifier cannot be null");
        }
        if (period <= 0) {
            LOGGER.error("Attempted to publish with invalid period: {}", period);
            throw new IllegalArgumentException("Period must be positive");
        }
        if (timeUnit == null) {
            LOGGER.error("Attempted to publish with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        checkNotShutdown();

        ScheduledFuture<?> task = scheduledTasks.get(identifier);
        if (task != null) {
            LOGGER.info("Cancelling existing task for identifier: {}", identifier);
            task.cancel(false);
        }

        LOGGER.info("Scheduling periodic event {} with identifier '{}', period {} {}",
                event.getClass().getSimpleName(),
                identifier,
                period,
                timeUnit);

        task = scheduler.scheduleAtFixedRate(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error publishing periodic event {} ({}): {}",
                        event.getClass().getSimpleName(),
                        identifier,
                        e.getMessage());
            }
        }, 0, period, timeUnit);

        scheduledTasks.put(identifier, task);
        return task;
    }

    /**
     * Schedules an event for periodic publication using the default time unit.
     *
     * @param event the event to publish periodically
     * @param identifier unique identifier for this periodic task
     * @param period the period between publications
     * @return a ScheduledFuture representing the periodic task
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws IllegalStateException if publisher is shut down
     */
    public ScheduledFuture<?> publish(Event<?> event, String identifier, long period) {
        return publish(event, identifier, period, timeUnit);
    }

    /**
     * Cancels a specific periodic task.
     *
     * @param identifier the identifier of the task to cancel
     * @throws IllegalArgumentException if identifier is null
     * @throws IllegalStateException if publisher is shut down
     */
    public void cancel (String identifier) {
        checkNotShutdown();

        if (identifier == null) {
            LOGGER.error("Attempted to cancel task with null identifier");
            throw new IllegalArgumentException("Identifier cannot be null");
        }

        ScheduledFuture<?> task = scheduledTasks.remove(identifier);
        if (task != null) {
            LOGGER.info("Cancelling periodic task: {}", identifier);
            task.cancel(false);
        } else {
            LOGGER.debug("No task found for identifier: {}", identifier);
        }
    }

    /**
     * Gets the number of currently active periodic tasks.
     *
     * @return number of active tasks
     * @throws IllegalStateException if publisher is shut down
     */
    public int getActiveTasksCount() {
        checkNotShutdown();
        return scheduledTasks.size();
    }

    /**
     * Checks if a periodic task exists for the given identifier.
     *
     * @param identifier the task identifier to check
     * @return true if a task exists for the identifier
     * @throws IllegalArgumentException if identifier is null
     * @throws IllegalStateException if publisher is shut down
     */
    public boolean hasTask(String identifier) {
        checkNotShutdown();

        if (identifier == null) {
            LOGGER.error("Attempted to check task with null identifier");
            throw new IllegalArgumentException("Identifier cannot be null");
        }

        return scheduledTasks.containsKey(identifier);
    }

    /**
     * Shuts down the publisher and cancels all periodic tasks.
     *
     * @throws IllegalStateException if already shut down
     */
    @Override
    public void shutdown () {
        checkNotShutdown();

        int tasksCount = scheduledTasks.size();
        if (tasksCount > 0) {
            LOGGER.info("Shutting down with {} active tasks", tasksCount);
            for (Map.Entry<String, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
                LOGGER.debug("Cancelling task: {}", entry.getKey());
                entry.getValue().cancel(false);
            }
            scheduledTasks.clear();
        }

        super.shutdown();
    }
}
