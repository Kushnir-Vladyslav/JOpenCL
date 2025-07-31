package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventPublisher;
import com.jopencl.Event.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronous event publisher that executes events in the calling thread.
 * Extends {@link EventPublisher} to provide immediate, synchronous event publishing.
 *
 * <p>This publisher:
 * <ul>
 * <li>Executes events immediately in the calling thread</li>
 * <li>Provides simple, synchronous event publishing</li>
 * <li>Blocks until event processing is complete</li>
 * <li>Has minimal overhead compared to asynchronous publishers</li>
 * </ul>
 *
 * <p>Use this publisher when:
 * <ul>
 * <li>Event order must be strictly maintained</li>
 * <li>Events must be processed immediately</li>
 * <li>You want to ensure event completion before continuing</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see EventPublisher
 */
public class SyncEventPublisher extends EventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncEventPublisher.class);

    /**
     * Creates a new SyncEventPublisher.
     * The publisher is ready to use immediately after creation.
     */
    public SyncEventPublisher() {
        LOGGER.debug("Created new SyncEventPublisher");
    }

    /**
     * Publishes an event synchronously in the calling thread.
     * This method blocks until the event is fully processed.
     *
     * @param event the event to publish
     * @throws IllegalArgumentException if event is null
     * @throws IllegalStateException    if publisher is shut down
     */
    public void publish(Event<?> event) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event");
            throw new IllegalArgumentException("Event cannot be null");
        }

        checkNotShutdown();

        try {
            publishEvent(event);
        } catch (RuntimeException e) {
            LOGGER.error("Error publishing event {}: {}",
                    event.getClass().getSimpleName(),
                    e.getMessage());
            throw e;
        }
    }

    /**
     * Shuts down the publisher.
     * After shutdown, no more events can be published.
     *
     * @throws IllegalStateException if already shut down
     */
    @Override
    public void shutdown() {
        checkNotShutdown();

        LOGGER.info("Shutting down SyncEventPublisher");
        setStatus(Status.SHUTDOWN);
    }
}
