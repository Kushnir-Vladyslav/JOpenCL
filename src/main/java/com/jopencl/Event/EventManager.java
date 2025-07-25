package com.jopencl.Event;

import java.util.concurrent.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central event management system that implements the Singleton pattern.
 * Handles event publishing, subscription, and dispatching to registered subscribers.
 * Thread-safe implementation using {@link CopyOnWriteArrayList} for subscribers
 * and {@link LinkedBlockingQueue} for event queue.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-06-11
 */
public class EventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    private final PriorityBlockingQueue<Event> eventQueue;
    private final List<EventSubscriber> subscribers;
    private final ExecutorService executor;
    private volatile boolean isRunning;

    /**
     * Holder class for lazy initialization of EventManager singleton.
     */
    private static class EventManagerHolder {
        private static final EventManager INSTANCE = new EventManager();
    }

    /**
     * Private constructor to prevent direct instantiation.
     * Initializes the event queue, subscriber list, and dispatch thread.
     */
    private EventManager() {
        LOGGER.debug("Initializing EventManager");
        this.eventQueue = new PriorityBlockingQueue<>(10, Event::priorityComparator);
        this.subscribers = new CopyOnWriteArrayList<>();

        executor = Executors.newFixedThreadPool(1);

        run();
    }

    /**
     * Gets the singleton instance of EventManager.
     *
     * @return the singleton instance of EventManager
     */
    public static EventManager getInstance () {
        return EventManagerHolder.INSTANCE;
    }

    /**
     * Starts the event dispatch thread if it's not already running.
     * Thread-safe method that ensures the dispatch thread is started only once.
     *
     * @throws IllegalArgumentException if the EventManager is disabled
     */
    public synchronized void run() {
        if (!isRunning) {
            LOGGER.info("Starting EventManager dispatch thread");
            this.isRunning = true;
            this.eventQueue.clear();
            if (!executor.isShutdown()) {
                this.executor.submit(this::dispatchEvents);
            } else {
                LOGGER.error("Attempted to run the disabled EventManager");
                throw new IllegalStateException("EventManager was already disabled.");
            }
        }
    }

    /**
     * Publishes an event to the event queue.
     * The event will be dispatched to all registered subscribers.
     *
     * @param event the event to publish
     * @throws IllegalArgumentException if the event is null
     */
    public void publish(Event event) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event");
            throw new IllegalArgumentException("Event cannot be null");
        }

        LOGGER.debug("Publishing event: {}", event);
        eventQueue.put(event);
    }

    /**
     * Registers a subscriber to receive events.
     *
     * @param subscriber the subscriber to register
     * @throws IllegalArgumentException if the subscriber is null
     */
    public void subscribe(EventSubscriber subscriber) {
        if (subscriber == null) {
            LOGGER.error("Attempted to register null subscriber");
            throw new IllegalArgumentException("Subscriber cannot be null");
        }

        LOGGER.debug("Registering subscriber: {}", subscriber.getClass().getSimpleName());
        subscribers.add(subscriber);
    }

    /**
     * Unregisters a subscriber from receiving events.
     *
     * @param subscriber the subscriber to unregister
     * @throws IllegalArgumentException if the subscriber is null
     */
    public void unsubscribe(EventSubscriber subscriber) {
        if (subscriber == null) {
            LOGGER.error("Attempted to unregister null subscriber");
            throw new IllegalArgumentException("Subscriber cannot be null");
        }

        LOGGER.debug("Unregistering subscriber: {}", subscriber.getClass().getSimpleName());
        subscribers.remove(subscriber);
    }

    /**
     * Internal method that runs in a separate thread to dispatch events to subscribers.
     * Continues running until shutdown is called or thread is interrupted.
     */
    private void dispatchEvents() {
        LOGGER.info("Event dispatch thread started");
        while (isRunning) {
            try {
                Event event = eventQueue.take();
                for (EventSubscriber subscriber : subscribers) {
                    try {
                        subscriber.onEvent(event);
                    } catch (Exception e) {
                        LOGGER.error("Error dispatching event {} to subscriber {}: {}",
                                event, subscriber.getClass().getSimpleName(), e.getMessage(), e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("Event dispatch thread interrupted");
                break;
            }
        }
        LOGGER.info("Event dispatch thread stopped");
    }

    /**
     * Stops the event dispatch thread if it is not already stopped.
     * After stopping, EventManager can be restarted.
     * Can be called multiple times safely.
     */
    public void stop() {
        if (isRunning) {
            LOGGER.info("Stopping EventManager");
            isRunning = false;
            eventQueue.clear();
        }
    }

    /**
     * Shuts down the EventManager by stopping the dispatch thread.
     * Can be called multiple times safely.
     */
    public void shutdown() {
        if (isRunning) {
            LOGGER.info("Shutting down EventManager");
            isRunning = false;
            executor.shutdown();
            eventQueue.clear();
        }
    }

    /**
     * Returns the current number of subscribers.
     * This method is primarily for testing and monitoring.
     *
     * @return the number of current subscribers
     */
    public int getSubscriberCount() {
        return subscribers.size();
    }

    /**
     * Returns the current size of the event queue.
     * This method is primarily for testing and monitoring.
     *
     * @return the current size of the event queue
     */
    public int getQueueSize() {
        return eventQueue.size();
    }
}
