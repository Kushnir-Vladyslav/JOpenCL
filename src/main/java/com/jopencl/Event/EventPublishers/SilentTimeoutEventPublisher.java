package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.ControlledListFuture;
import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;

import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event publisher that executes events with timeout constraints, silently cancelling them if they exceed the timeout.
 * Extends {@link ExecuteEventPublisher} to provide timeout-controlled event publishing capabilities.
 *
 * <p>This publisher:
 * <ul>
 * <li>Executes events with specified timeout periods</li>
 * <li>Silently cancels events that exceed their timeout</li>
 * <li>Uses {@link ControlledListFuture} for managing futures</li>
 * <li>Provides cleanup of completed and cancelled tasks</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ExecuteEventPublisher
 * @see ControlledListFuture
 */
public class SilentTimeoutEventPublisher extends ExecuteEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SilentTimeoutEventPublisher.class);

    /** Scheduler for timeout management */
    private final ScheduledExecutorService timeoutScheduler;
    /** Controls and tracks futures */
    private final ControlledListFuture listFuture;

    /**
     * Creates a new SilentTimeoutEventPublisher with specified time unit.
     *
     * @param timeUnit the default time unit for timeouts
     * @throws IllegalArgumentException if timeUnit is null
     */
    public SilentTimeoutEventPublisher (TimeUnit timeUnit) {
        super();
        if (timeUnit == null) {
            LOGGER.error("Attempted to create publisher with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        this.executor = Executors.newFixedThreadPool(1);
        this.listFuture = new ControlledListFuture();
        this.timeoutScheduler = Executors.newScheduledThreadPool(1);
        this.timeUnit = timeUnit;

        LOGGER.debug("Created SilentTimeoutEventPublisher with timeUnit={}", timeUnit);
    }

    /**
     * Creates a new SilentTimeoutEventPublisher with milliseconds as default time unit.
     */
    public SilentTimeoutEventPublisher () {
        this(TimeUnit.MILLISECONDS);
    }

    /**
     * Publishes an event with a timeout constraint.
     * If the event execution exceeds the timeout, it will be silently cancelled.
     *
     * @param event the event to publish
     * @param timeout the maximum time to wait for event completion
     * @param timeUnit the time unit of the timeout
     * @return a Future representing pending completion of the event
     * @throws IllegalArgumentException if event is null, timeout is negative, or timeUnit is null
     * @throws IllegalStateException if publisher is shut down
     */
    public Future<?> publish(Event<?> event, long timeout, TimeUnit timeUnit) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event");
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (timeout < 0) {
            LOGGER.error("Attempted to publish with negative timeout: {}", timeout);
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        if (timeUnit == null) {
            LOGGER.error("Attempted to publish with null TimeUnit");
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }

        checkNotShutdown();

        LOGGER.debug("Publishing event {} with timeout {} {}",
                event.getClass().getSimpleName(),
                timeout,
                timeUnit);

        Future<?> future = executor.submit(() -> {
            try {
                publishEvent(event);
            } catch (Exception e) {
                LOGGER.error("Error publishing event {}: {}",
                        event.getClass().getSimpleName(),
                        e.getMessage());
            }
        });

        ScheduledFuture<?> scheduledFuture = timeoutScheduler.schedule(() -> {
            if (!future.isDone()) {
                LOGGER.info("Event {} exceeded timeout of {} {}, cancelling",
                        event.getClass().getSimpleName(),
                        timeout,
                        timeUnit);
                future.cancel(true);
            }
        }, timeout, timeUnit);

        listFuture.add(future);
        listFuture.add(scheduledFuture);

        return future;
    }

    /**
     * Publishes an event with a timeout constraint using the default time unit.
     *
     * @param event the event to publish
     * @param timeout the maximum time to wait for event completion
     * @return a Future representing pending completion of the event
     * @throws IllegalArgumentException if event is null or timeout is negative
     * @throws IllegalStateException if publisher is shut down
     */
    public Future<?> publish(Event<?> event, long timeout) {
        return publish(event, timeout, timeUnit);
    }

    /**
     * Gets the number of currently executing events.
     *
     * @return number of pending events
     * @throws IllegalStateException if publisher is shut down
     */
    public int getPendingEventsCount() {
        checkNotShutdown();
        return listFuture.getFutures().size() / 2; // Divide by 2 because each event has 2 futures
    }

    /**
     * Cancels all pending events.
     *
     * @return number of cancelled events
     * @throws IllegalStateException if publisher is shut down
     */
    public int cancelAllPendingEvents() {
        checkNotShutdown();

        int count = getPendingEventsCount();
        if (count > 0) {
            LOGGER.info("Cancelling {} pending events", count);
            listFuture.stopAll();
        }

        return count;
    }

    /**
     * Shuts down the publisher and cancels all pending events.
     *
     * @throws IllegalStateException if already shut down
     */
    @Override
    public void shutdown() {
        checkNotShutdown();

        int pendingCount = getPendingEventsCount();
        if (pendingCount > 0) {
            LOGGER.info("Shutting down with {} pending events", pendingCount);
        }

        timeoutScheduler.shutdownNow();
        listFuture.stopControlAndShutdown();
        super.shutdown();
    }
}
