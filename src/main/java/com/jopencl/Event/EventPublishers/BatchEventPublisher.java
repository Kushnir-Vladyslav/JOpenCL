package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ExecuteEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publisher that accumulates events and publishes them in batches.
 * Extends {@link ExecuteEventPublisher} to provide batch publishing capabilities.
 *
 * <p>This publisher:
 * <ul>
 * <li>Collects events until reaching a specified batch size</li>
 * <li>Automatically publishes when batch is full</li>
 * <li>Supports manual flushing of incomplete batches</li>
 * <li>Uses a single-threaded executor for batch processing</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ExecuteEventPublisher
 */
public class BatchEventPublisher extends ExecuteEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchEventPublisher.class);

    /** Maximum size of event batches before automatic publishing */
    private final int batchSize;
    /** Current batch of events waiting to be published */
    private List<Event<?>> batch;

    /**
     * Creates a new BatchEventPublisher with specified batch size.
     *
     * @param batchSize the number of events to accumulate before publishing
     * @throws IllegalArgumentException if batchSize is less than or equal to 0
     */
    public BatchEventPublisher (int batchSize) {
        super();
        if (batchSize <= 0) {
            LOGGER.error("Attempted to create BatchEventPublisher with invalid batch size: {}", batchSize);
            throw new IllegalArgumentException("Batch size must be greater than 0");
        }

        this.batchSize = batchSize;
        this.batch = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(1);

        LOGGER.debug("Created BatchEventPublisher with batchSize={}", batchSize);
    }

    /**
     * Adds an event to the current batch and publishes if batch size is reached.
     *
     * @param event the event to publish
     * @throws IllegalArgumentException if event is null
     * @throws IllegalStateException if publisher is shut down
     */
    public void publish (Event<?> event) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event");
            throw new IllegalArgumentException("Event cannot be null");
        }

        checkNotShutdown();

        batch.add(event);

        if (batch.size() >= batchSize) {
            LOGGER.debug("Batch size reached ({}), flushing", batchSize);
            flush();
        }
    }

    /**
     * Forces publishing of current batch, regardless of its size.
     *
     * @throws IllegalStateException if publisher is shut down
     */
    public void flush () {
        checkNotShutdown();

        if (batch.isEmpty()) {
            LOGGER.debug("Flush called on empty batch, ignoring");
            return;
        }

        List<Event<?>> fullBatch = batch;
        batch = new ArrayList<>();

        LOGGER.debug("Flushing batch of {} events", fullBatch.size());

        executor.submit(() -> {
            int failedCount = 0;
            for (Event<?> event : fullBatch) {
                try {
                    publishEvent(event);
                } catch (Exception e) {
                    failedCount++;
                    LOGGER.error("Error publishing event {}: {}",
                            event.getClass().getSimpleName(),
                            e.getMessage());
                }
            }
            if (failedCount > 0) {
                LOGGER.warn("Batch completed with {} failed events out of {}", failedCount, batchSize);
            } else {
                LOGGER.debug("Successfully published batch of {} events", batchSize);
            }
        });
    }

    /**
     * {@inheritDoc}
     * Additionally ensures any remaining events in the current batch are published.
     */
    @Override
    public void shutdown() {
        if (!batch.isEmpty()) {
            LOGGER.info("Publishing remaining {} events before shutdown", batch.size());
            flush();
        }
        super.shutdown();
    }
}
