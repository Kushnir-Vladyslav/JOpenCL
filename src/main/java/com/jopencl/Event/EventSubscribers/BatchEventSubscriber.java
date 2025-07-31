package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.Events.ListEvents;
import com.jopencl.Event.ProcessingListEventErrorSubscriber;
import com.jopencl.Event.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event subscriber that processes events in batches.
 * Extends {@link ProcessingListEventErrorSubscriber} to provide batch processing capabilities
 * with error handling.
 *
 * <p>This subscriber accumulates events of the same type until reaching a specified batch size,
 * then processes them as a group using {@link ListEvents}.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ProcessingListEventErrorSubscriber
 * @see ListEvents
 */
public class BatchEventSubscriber extends ProcessingListEventErrorSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchEventSubscriber.class);

    /** Executor service for running the event processing loop and batch processing */
    private final ExecutorService executor;
    /** Future representing the running event processing task */
    private volatile Future<?> dispatchTask;
    /** Maximum size of event batches before processing */
    private final int batchSize;

    /** Map storing incomplete batches of events by their type */
    protected final Map<Class<? extends Event<?>>, List<Event<?>>> batches;

    /**
     * Creates a new BatchEventSubscriber with specified batch size and auto-run option.
     *
     * @param batchSize the number of events to accumulate before processing
     * @param autoRun if true, automatically starts event processing
     * @throws IllegalArgumentException if batchSize is less than or equal to 0
     */
    public BatchEventSubscriber (int batchSize, boolean autoRun) {
        super();
        if (batchSize <= 0) {
            LOGGER.error("Attempted to create BatchEventSubscriber with invalid batch size: {}", batchSize);
            throw new RuntimeException("negative size");
        }

        this.batchSize = batchSize;
        this.executor = Executors.newFixedThreadPool(2);
        this.batches = new ConcurrentHashMap<>();

        LOGGER.debug("Created BatchEventSubscriber with batchSize={}, autoRun={}", batchSize, autoRun);

        if (autoRun) {
            run();
        }
    }

    /**
     * Creates a new BatchEventSubscriber with specified batch size without auto-run.
     *
     * @param batchSize the number of events to accumulate before processing
     * @throws IllegalArgumentException if batchSize is less than or equal to 0
     */
    public BatchEventSubscriber (int batchSize) {
        this(batchSize, false);
    }

    /**
     * Starts or resumes event processing if the subscriber is in an appropriate state.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void run() {
        checkNotShutdown();

        if (canRun()) {
            LOGGER.info("Starting BatchEventSubscriber");
            setStatus(Status.RUNNING);
            subscribe();
            dispatchTask = executor.submit(this::processEvents);
        }
    }

    /**
     * Internal method that runs in a separate thread to collect and process events.
     * Events are accumulated until reaching the batch size, then processed as a group.
     */
    private void processEvents () {
        LOGGER.debug("Event processing loop started");
        while (status == Status.RUNNING) {
            try {
                Event<?> event = subscriberQueue.take();

                @SuppressWarnings("unchecked")
                Class<Event<?>> eventType = (Class<Event<?>>) event.getClass();

                if (handlers.containsKey(eventType)) {
                    List<Event<?>> batch = batches.computeIfAbsent(eventType, k -> new ArrayList<>());

                    batch.add(event);

                    if (batch.size() >= batchSize) {
                        batch = batches.remove(eventType);
                        processEvent(new ListEvents<>(eventType, batch));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.debug("Event processing loop interrupted");
                break;
            }
        }
        LOGGER.debug("Event processing loop stopped");
    }

    public void flush() {
        checkNotShutdown();

        LOGGER.debug("Flushing all batches");
        for (Class<? extends Event<?>> eventType : batches.keySet()) {
            List listEvent = batches.remove(eventType);
            executor.submit(() -> processEvent(new ListEvents<>(eventType, listEvent)));
        }
    }

    /**
     * Temporarily pauses event processing.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void pause() {
        checkNotShutdown();

        if (isRunning()) {
            LOGGER.info("Pausing BatchEventSubscriber");
            setStatus(Status.PAUSED);
            unsubscribe();
            dispatchTask.cancel(true);
        }
    }

    /**
     * Stops event processing and cleans up resources.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void stop() {
        checkNotShutdown();

        if (isRunning() || isPaused()) {
            LOGGER.info("Stopping BatchEventSubscriber");
            dispatchTask.cancel(true);
            super.stop();
        }
    }

    /**
     * Permanently shuts down the subscriber and its executor service.
     *
     * @throws IllegalStateException if already shut down
     */
    @Override
    public void shutdown() {
        checkNotShutdown();

        LOGGER.info("Shutting down BatchEventSubscriber");
        setStatus(Status.SHUTDOWN);
        unsubscribe();
        executor.shutdownNow();
    }
}
