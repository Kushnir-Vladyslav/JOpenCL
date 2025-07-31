package com.jopencl.Event.EventSubscribers;

import com.jopencl.Event.Event;
import com.jopencl.Event.EventSubscriber;
import com.jopencl.Event.Status;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event subscriber that buffers events for batch collection.
 * This subscriber allows filtering and collecting events without immediate processing.
 *
 * <p>Features:
 * <ul>
 * <li>Event type filtering</li>
 * <li>Batch collection of events</li>
 * <li>Optional automatic start</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see EventSubscriber
 */
public class BufferedEventSubscriber extends EventSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferedEventSubscriber.class);

    /** Set of event types to filter */
    protected final Set<Class<?>> eventFilter;

    /**
     * Creates a new BufferedEventSubscriber with optional automatic start.
     *
     * @param autoRun if true, automatically starts event processing
     */
    public BufferedEventSubscriber(boolean autoRun) {
        super();
        this.eventFilter = new HashSet<>();
        LOGGER.debug("Created BufferedEventSubscriber with autoRun={}", autoRun);

        if (autoRun) {
            run();
        }
    }

    /**
     * Creates a new BufferedEventSubscriber with automatic start enabled.
     */
    public BufferedEventSubscriber() {
        this(true);
    }

    /**
     * Starts or resumes event buffering if the subscriber is in an appropriate state.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void run() {
        checkNotShutdown();

        if (canRun()) {
            LOGGER.info("Starting BufferedEventSubscriber");
            setStatus(Status.RUNNING);
            subscribe();
        }
    }

    /**
     * Adds event types to the filter set.
     * Events of these types will be included in filtered collections.
     *
     * @param eventTypes the event types to subscribe to
     */
    public void subscribeEvent (Class<? extends Event<?>> ... eventTypes) {
        if (eventTypes == null) {
            return;
        }
        for (Class<? extends Event<?>> event : eventTypes) {
            if (event != null) {
                LOGGER.debug("Adding event type to filter: {}", event.getSimpleName());
                eventFilter.add(event);
            }
        }
    }

    /**
     * Removes event types from the filter set.
     *
     * @param eventTypes the event types to unsubscribe from
     */
    public void unsubscribeEvent (Class<? extends Event<?>> ... eventTypes) {
        if (eventTypes == null) {
            return;
        }
        for (Class<? extends Event<?>> event : eventTypes) {
            if (event != null) {
                LOGGER.debug("Removing event type from filter: {}", event.getSimpleName());
                eventFilter.remove(event);
            }
        }
    }

    /**
     * Collects and returns all buffered events, regardless of their type.
     *
     * @return list of all buffered events
     */
    public List<Event<?>> collectAllEvents () {
        List<Event<?>> events = new ArrayList<>();
        subscriberQueue.drainTo(events);
        LOGGER.debug("Collected {} events (unfiltered)", events.size());
        return events;
    }

    /**
     * Collects and returns only events of types present in the filter set.
     *
     * @return list of filtered events
     */
    public List<Event<?>> collectEvents () {
        List<Event<?>> events = new ArrayList<>();
        Event<?> event;

        while ((event = subscriberQueue.poll()) != null) {
            if (eventFilter.contains(event.getClass())) {
                events.add(event);
            }
        }

        LOGGER.debug("Collected {} events (filtered)", events.size());
        return events;
    }

    /**
     * Temporarily pauses event buffering.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void pause() {
        checkNotShutdown();

        if (isRunning()) {
            LOGGER.info("Pausing BufferedEventSubscriber");
            setStatus(Status.PAUSED);
            unsubscribe();
        }
    }

    /**
     * Stops event buffering.
     *
     * @throws IllegalStateException if the subscriber is shut down
     */
    @Override
    public void stop() {
        checkNotShutdown();

        if (isRunning() || isPaused()) {
            LOGGER.info("Stopping BufferedEventSubscriber");
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

        LOGGER.info("Shutting down BufferedEventSubscriber");
        setStatus(Status.SHUTDOWN);
        unsubscribe();
    }
}
