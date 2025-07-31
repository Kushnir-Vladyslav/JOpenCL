package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Asynchronous event publisher that processes events in a separate thread.
 * Uses a single-threaded executor to ensure events are published in order
 * while not blocking the calling thread.
 *
 * <p>This publisher provides fire-and-forget semantics - events are submitted
 * for asynchronous processing and any errors during publishing are logged
 * but do not affect the caller.
 *
 * <p>The publisher maintains FIFO ordering of events through the use of
 * a single-threaded executor.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-06-11
 * @see ExecuteEventPublisher
 * @see Event
 */
public class AsyncEventPublisher extends ExecuteEventPublisher {
    /** Logger instance for this async publisher */
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncEventPublisher.class);

    /**
     * Creates a new AsyncEventPublisher with a single-threaded executor.
     * The executor ensures FIFO ordering of event processing.
     */
    public AsyncEventPublisher () {
        LOGGER.debug("Initializing AsyncEventPublisher with single-threaded executor");
        executor = Executors.newFixedThreadPool(1);
        LOGGER.info("AsyncEventPublisher initialized successfully");
    }

    /**
     * Publishes an event asynchronously without blocking the calling thread.
     * The event will be processed by a background thread in FIFO order.
     *
     * <p>This method provides fire-and-forget semantics. Any errors during
     * event publishing will be logged but will not be propagated to the caller.
     *
     * @param event the event to publish asynchronously, must not be null
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException if this publisher has been shut down
     * @throws IllegalStateException if the executor is not available
     */
    public void publish(Event<?> event) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event in AsyncEventPublisher");
            throw new IllegalArgumentException("Event cannot be null");
        }

        checkNotShutdown();

        executor.submit(() -> {
            try {
                publishEvent(event);
            } catch (IllegalStateException e) {
                LOGGER.warn("Failed to publish event {} - publisher shut down during processing: {}", event, e.getMessage());
            } catch (Exception e) {
                LOGGER.error("Failed to publish async event {}: {}", event, e.getMessage(), e);
            }
        });
    }
}
