package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.ProcessingSingleEventErrorSubscriber;
import com.jopencl.Event.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronous event subscriber that processes events in the calling thread.
 * Extends {@link ProcessingSingleEventErrorSubscriber} to provide synchronous
 * event processing with error handling capabilities.
 *
 * <p>Unlike {@link AsyncEventSubscriber}, this subscriber processes events
 * synchronously when {@link #processEvents()} is called, making it suitable
 * for scenarios where immediate event processing is required.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ProcessingSingleEventErrorSubscriber
 */
public class SyncEventSubscriber extends ProcessingSingleEventErrorSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncEventSubscriber.class);

    /**
     * Creates a new SyncEventSubscriber with optional automatic start.
     *
     * @param autoRun if true, automatically starts event processing
     */
    public SyncEventSubscriber(boolean autoRun) {
        super();
        LOGGER.debug("Created SyncEventSubscriber with autoRun={}", autoRun);

        if (autoRun) {
            run();
        }
    }

    /**
     * Creates a new SyncEventSubscriber without automatic start.
     */
    public SyncEventSubscriber() {
        this(false);
    }

    /**
     * Starts or resumes event subscription if the subscriber is in an appropriate state.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void run() {
        checkNotShutdown();

        if (canRun()) {
            LOGGER.info("Starting SyncEventSubscriber");
            setStatus(Status.RUNNING);
            subscribe();
        }
    }

    /**
     * Processes all currently queued events synchronously in the calling thread.
     * This method will process events until the queue is empty.
     *
     * <p>Note: This method does not block waiting for new events.
     */
    @SuppressWarnings("unchecked")
    public void processEvents () {
        LOGGER.debug("Starting synchronous event processing");
        Event<?> event;
        int processedCount = 0;

        while ((event = subscriberQueue.poll()) != null) {
            processEvent(event);
            processedCount++;
        }

        LOGGER.debug("Finished processing {} events", processedCount);
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
            LOGGER.info("Pausing SyncEventSubscriber");
            setStatus(Status.PAUSED);
            unsubscribe();
        }
    }

    /**
     * Stops event processing.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void stop() {
        checkNotShutdown();

        if (isRunning() || isPaused()) {
            LOGGER.info("Stopping SyncEventSubscriber");
            super.stop();
        }
    }

    /**
     * Permanently shuts down the subscriber.
     *
     * @throws IllegalStateException if already shut down
     */
    @Override
    public void shutdown() {
        checkNotShutdown();

        LOGGER.info("Shutting down SyncEventSubscriber");
        setStatus(Status.SHUTDOWN);
        unsubscribe();
    }
}
