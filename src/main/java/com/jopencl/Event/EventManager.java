package com.jopencl.Event;

import java.util.concurrent.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central event management system that implements the Singleton pattern.
 * Handles event publishing, subscription, and dispatching to registered subscribers.
 * Thread-safe implementation using {@link CopyOnWriteArrayList} for subscribers
 * and {@link PriorityBlockingQueue} for event queue.
 *
 * <p>The EventManager supports the following lifecycle states:
 * <ul>
 * <li>{@code CREATED} - Initial state after instantiation</li>
 * <li>{@code RUNNING} - Active event processing</li>
 * <li>{@code PAUSED} - Temporarily stopped, can be resumed</li>
 * <li>{@code STOPPED} - Stopped, can be restarted</li>
 * <li>{@code SHUTDOWN} - Permanently disabled</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-06-11
 */
public class EventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);

    private final PriorityBlockingQueue<Event<?>> eventQueue;
    private final List<EventSubscriber> subscribers;
    private final ExecutorService executor;
    private volatile Future<?> dispatchTask;
    private volatile Status status = Status.CREATED;

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
        if (status == Status.CREATED || status == Status.PAUSED || status == Status.STOPPED) {
            LOGGER.info("Starting EventManager dispatch thread");
            this.status = Status.RUNNING;
            if (!executor.isShutdown()) {
                this.dispatchTask = this.executor.submit(this::dispatchEvents);
            } else {
                LOGGER.error("Attempted to run the disabled EventManager");
                throw new IllegalStateException("EventManager was already disabled.");
            }
        } else if (status == Status.SHUTDOWN) {
            LOGGER.error("Attempted to run the disabled EventManager");
            throw new IllegalStateException("EventManager was already disabled.");
        }
    }

    /**
     * Publishes an event to the event queue.
     * The event will be dispatched to all registered subscribers.
     *
     * @param event the event to publish
     * @throws IllegalArgumentException if the event is null
     */
    public void publish(Event<?> event) {
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
        LOGGER.info("Subscriber {} registered successfully. Total subscribers: {}",
                subscriber.getClass().getSimpleName(), subscribers.size());
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

        boolean removed = subscribers.remove(subscriber);
        if (removed) {
            LOGGER.info("Subscriber {} unregistered successfully. Total subscribers: {}",
                    subscriber.getClass().getSimpleName(), subscribers.size());
        } else {
            LOGGER.warn("Attempted to unregister subscriber {} that was not registered",
                    subscriber.getClass().getSimpleName());
        }
    }

    /**
     * Internal method that runs in a separate thread to dispatch events to subscribers.
     * Continues running until shutdown is called or thread is interrupted.
     */
    private void dispatchEvents() {
        LOGGER.info("Event dispatch thread started");
        while (status == Status.RUNNING) {
            try {
                Event<?> event = eventQueue.take();
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
     * Temporarily pauses the event dispatch thread.
     * The EventManager can be resumed by calling {@link #run()}.
     *
     * @throws IllegalStateException if the EventManager is already shut down
     */
    public void pause() {
        if (status == Status.RUNNING) {
            LOGGER.info("Stopping EventManager");
            status = Status.PAUSED;
            dispatchTask.cancel(true);
        } else if (status == Status.SHUTDOWN) {
            LOGGER.error("Attempted to pause the disabled EventManager");
            throw new IllegalStateException("EventManager was already disabled.");
        }
    }

    /**
     * Stops the event dispatch thread if it is not already stopped.
     * After stopping, EventManager can be restarted.
     * Can be called multiple times safely.
     */
    public void stop() {
        if (status == Status.RUNNING || status == Status.PAUSED) {
            LOGGER.info("Stopping EventManager");
            status = Status.STOPPED;
            dispatchTask.cancel(true);
            eventQueue.clear();
        } else if (status == Status.SHUTDOWN) {
            LOGGER.error("Attempted to stop the disabled EventManager");
            throw new IllegalStateException("EventManager was already disabled.");
        }
    }

    /**
     * Shuts down the EventManager by stopping the dispatch thread.
     * Can be called multiple times safely.
     */
    public void shutdown() {
        if (status != Status.SHUTDOWN) {
            LOGGER.info("Shutting down EventManager");
            status = Status.SHUTDOWN;
            executor.shutdownNow();
            eventQueue.clear();
        } else {
            LOGGER.error("Attempted to shutdown the disabled EventManager");
            throw new IllegalStateException("EventManager was already disabled.");
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

    /**
     * Returns the current status of the EventManager.
     * This method is primarily for testing and monitoring.
     *
     * @return the current status of the EventManager
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Returns comprehensive status information about the EventManager.
     *
     * @return status information including queue size, subscriber count, and current state
     */
    public String getStatusInfo() {
        return String.format("EventManager[status=%s, subscribers=%d, queueSize=%d]",
                status, subscribers.size(), eventQueue.size());
    }
}
