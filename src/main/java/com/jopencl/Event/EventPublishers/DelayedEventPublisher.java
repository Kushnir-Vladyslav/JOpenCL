package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.ControlledListFuture;
import com.jopencl.Event.Event;
import com.jopencl.Event.ScheduleEventPublisher;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event publisher that schedules events for delayed publication.
 * Extends {@link ScheduleEventPublisher} to provide delayed event publishing capabilities.
 *
 * <p>This publisher:
 * <ul>
 * <li>Allows scheduling events with specific delays</li>
 * <li>Supports different time units for delay specification</li>
 * <li>Manages scheduled events through controlled futures</li>
 * </ul>
 *
 * <p>Note: All instances of DelayedEventPublisher share the same scheduler pool
 * through ControlledListFuture to optimize resource usage.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ScheduleEventPublisher
 * @see ControlledListFuture
 */
public class DelayedEventPublisher extends ScheduleEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DelayedEventPublisher.class);

    /** Controls and tracks scheduled futures */
    private final ControlledListFuture listFuture;

    /**
     * Creates a new DelayedEventPublisher with specified time unit.
     *
     * @param timeUnit the default time unit for delays
     * @throws IllegalArgumentException if timeUnit is null
     */
    public DelayedEventPublisher (TimeUnit timeUnit) {
        super();
        if (timeUnit == null) {
            LOGGER.error("Attempted to create publisher with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.listFuture = new ControlledListFuture();
        this.timeUnit = timeUnit;

        LOGGER.debug("Created DelayedEventPublisher with timeUnit={}, futures control status={}",
                timeUnit, listFuture.getStatus());
    }

    /**
     * Creates a new DelayedEventPublisher with milliseconds as default time unit.
     */
    public DelayedEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules an event for delayed publication.
     *
     * @param event the event to publish
     * @param delay the delay before publishing
     * @param timeUnit the time unit of the delay
     * @return a ScheduledFuture representing pending completion of the publication
     * @throws IllegalArgumentException if event is null, delay is negative, or timeUnit is null
     * @throws IllegalStateException if publisher is shut down
     */
    public ScheduledFuture<?> publish(Event<?> event, long delay, TimeUnit timeUnit) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event");
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (delay < 0) {
            LOGGER.error("Attempted to publish with negative delay: {}", delay);
            throw new IllegalArgumentException("Delayed cannot be negative");
        }
        if (timeUnit == null) {
            LOGGER.error("Attempted to publish with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        checkNotShutdown();

        LOGGER.info("Scheduling delayed event {} with delay {} {}, active futures: {}",
                event.getClass().getSimpleName(),
                delay,
                timeUnit,
                listFuture.getFutures().size());

        ScheduledFuture<?> scheduledFuture = scheduler.schedule(() -> {
            try {
                publishEvent(event);
                LOGGER.debug("Successfully published delayed event: {}",
                        event.getClass().getSimpleName());
            } catch (Exception e) {
                LOGGER.error("Error publishing delayed event {}: {}",
                        event.getClass().getSimpleName(),
                        e.getMessage());
            }
        }, delay, timeUnit);

        listFuture.add(scheduledFuture);

        return scheduledFuture;
    }

    /**
     * Schedules an event for delayed publication using the default time unit.
     *
     * @param event the event to publish
     * @param delay the delay before publishing
     * @return a ScheduledFuture representing pending completion of the publication
     * @throws IllegalArgumentException if event is null or delay is negative
     * @throws IllegalStateException if publisher is shut down
     */
    public ScheduledFuture<?> publish(Event<?> event, long delay) {
        return publish(event, delay, timeUnit);
    }

    /**
     * Gets the number of currently scheduled events.
     *
     * @return number of pending events
     * @throws IllegalStateException if publisher is shut down
     */
    public int getPendingEventsCount() {
        checkNotShutdown();
        return listFuture.getFutures().size();
    }

    /**
     * Cancels all pending scheduled events.
     * Events that are currently being published will complete normally.
     *
     * @return number of cancelled events
     * @throws IllegalStateException if publisher is shut down
     */
    public int cancelAllPendingEvents() {
        checkNotShutdown();

        int count = listFuture.getFutures().size();
        LOGGER.info("Cancelling {} pending events", count);
        listFuture.stopAll();

        return count;
    }

    /**
     * Shuts down the publisher and cancels all pending scheduled events.
     *
     * <p>Note: This operation might affect the shared scheduler if this is
     * the last active publisher instance.
     *
     * @throws IllegalStateException if already shut down
     */
    @Override
    public void shutdown() {
        checkNotShutdown();

        int pendingCount = getPendingEventsCount();
        if (pendingCount > 0) {
            LOGGER.info("Shutting down with {} pending events, cancelling all", pendingCount);
        }
        listFuture.stopControlAndShutdown();
        super.shutdown();
    }
}
