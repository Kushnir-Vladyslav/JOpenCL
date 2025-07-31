package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ProcessingSingleEventErrorSubscriber;
import com.jopencl.Event.Status;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Asynchronous event subscriber that processes events in a separate thread.
 * Extends {@link ProcessingSingleEventErrorSubscriber} to provide asynchronous event processing
 * with error handling capabilities.
 *
 * <p>This subscriber uses a single-threaded executor to process events asynchronously.
 * It supports the following operations:
 * <ul>
 * <li>Automatic or manual start of event processing</li>
 * <li>Pausing and resuming event processing</li>
 * <li>Graceful shutdown of the processing thread</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ProcessingSingleEventErrorSubscriber
 */
public class AsyncEventSubscriber extends ProcessingSingleEventErrorSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncEventSubscriber.class);

    /** Executor service for running the event processing loop */
    private final ExecutorService executor;
    /** Future representing the running event processing task */
    private volatile Future<?> dispatchTask;

    /**
     * Creates a new AsyncEventSubscriber with optional automatic start.
     *
     * @param autoRun if true, automatically starts event processing
     */
    public AsyncEventSubscriber(boolean autoRun) {
        super();
        executor = Executors.newFixedThreadPool(1);
        LOGGER.debug("Created AsyncEventSubscriber with autoRun={}", autoRun);

        if (autoRun) {
            run();
        }
    }

    /**
     * Creates a new AsyncEventSubscriber without automatic start.
     */
    public AsyncEventSubscriber() {
        this(false);
    }

    /**
     * Starts or resumes event processing if the subscriber is in an appropriate state.
     *
     * @throws IllegalStateException if the subscriber is already shut down
     */
    @Override
    public void run() {
        checkNotShutdown();

        if (canRun()) {
            LOGGER.info("Starting AsyncEventSubscriber");
            setStatus(Status.RUNNING);
            subscribe();
            dispatchTask = executor.submit(this::processEvents);
        }
    }

    /**
     * Internal method that runs in a separate thread to process events.
     * Continues running until interrupted or subscriber is stopped.
     */
    @SuppressWarnings("unchecked")
    private void processEvents () {
        LOGGER.debug("Event processing loop started");
        while (status == Status.RUNNING) {
            try {
                Event<?> event = subscriberQueue.take();
                processEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.debug("Event processing loop interrupted");
                break;
            }
        }
        LOGGER.debug("Event processing loop stopped");
    }

    /**
     * Temporarily pauses event processing.
     *
     * @throws IllegalStateException if the subscriber is already shut down
     */
    @Override
    public void pause() {
        checkNotShutdown();

        if (isRunning()) {
            LOGGER.info("Pausing AsyncEventSubscriber");
            setStatus(Status.PAUSED);
            unsubscribe();
            dispatchTask.cancel(true);
        }
    }

    /**
     * Stops event processing and cleans up resources.
     *
     * @throws IllegalStateException if the subscriber is already shut down
     */
    @Override
    public void stop() {
        checkNotShutdown();

        if (isRunning() || isPaused()) {
            LOGGER.info("Stopping AsyncEventSubscriber");
            dispatchTask.cancel(true);
            super.stop();
        }
    }

    /**
     * Permanently shuts down the subscriber and its executor service.
     *
     * @throws IllegalStateException if the subscriber is already shut down
     */
    @Override
    public void shutdown() {
        checkNotShutdown();

        LOGGER.info("Shutting down AsyncEventSubscriber");
        unsubscribe();
        executor.shutdownNow();
        setStatus(Status.SHUTDOWN);
    }
}
