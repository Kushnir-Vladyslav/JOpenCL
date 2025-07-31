package com.jopencl.Event;

import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for components that subscribe to and process events from the event management system.
 * Provides common functionality for event subscription, queue management, and lifecycle control.
 *
 * <p>This class maintains its own priority queue for received events and provides
 * lifecycle management through status tracking. Subscribers can be paused, stopped,
 * and shutdown as needed.
 *
 * <p>Subclasses must implement the {@link #run()}, {@link #pause()}, and {@link #shutdown()}
 * methods to define their specific event processing and lifecycle behavior.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see EventManager
 * @see Event
 */
public abstract class EventSubscriber {
    /** Logger instance for this subscriber */
    private static final Logger LOGGER = LoggerFactory.getLogger(EventSubscriber.class);

    /** Reference to the singleton event manager */
    protected static final EventManager eventManager;
    /** Queue for storing received events before processing */
    protected final PriorityBlockingQueue<Event<?>> subscriberQueue;

    /** Current operational status of this subscriber */
    protected volatile Status status = Status.CREATED;

    /**
     * Static initialization of the EventManager instance.
     */
    static {
        eventManager = EventManager.getInstance();
    }

    /**
     * Creates a new EventSubscriber with a priority queue for event processing.
     */
    protected EventSubscriber() {
        this.subscriberQueue = new PriorityBlockingQueue<>(10, Event::priorityComparator);
        LOGGER.debug("Created new EventSubscriber: {}", this.getClass().getSimpleName());
    }

    /**
     * Starts the event processing for this subscriber.
     * Subclasses must implement this method to define their specific processing behavior.
     */
    public abstract void run();

    /**
     * Registers this subscriber with the EventManager to begin receiving events.
     *
     * @throws IllegalStateException if the subscriber is shutdown
     */
    protected void subscribe() {
        if (isShutdown()) {
            LOGGER.error("Attempted to subscribe when subscriber is shutdown: {}",
                    this.getClass().getSimpleName());
            throw new IllegalStateException("Subscriber is shutdown and cannot be subscribed");
        }

        LOGGER.debug("Subscribing {} to EventManager", this.getClass().getSimpleName());
        eventManager.subscribe(this);
    }


    /**
     * Handles incoming events by adding them to the subscriber's queue.
     * This method is called by the EventManager when dispatching events.
     *
     * @param event the event to process
     */
    public void onEvent(Event<?> event) {
        LOGGER.debug("Clearing event queue for {}", this.getClass().getSimpleName());
        subscriberQueue.put(event);
    }

    /**
     * Clears all pending events from the subscriber's queue.
     */
    protected void clearQueue() {
        subscriberQueue.clear();
    }

    /**
     * Returns the current operational status of this subscriber.
     *
     * @return the current status, never null
     */
    public Status getStatus () {
        return status;
    }

    /**
     * Checks if this subscriber is still operational and not shut down.
     * This method should be called by subclasses before performing operations
     * that are not allowed in shutdown state.
     *
     * @throws IllegalStateException if this subscriber has been shut down
     */
    protected void checkNotShutdown() {
        if (isShutdown()) {
            String message = this.getClass().getSimpleName() + " was already shut down and cannot be used";
            LOGGER.error("Operation attempted on shut down subscriber: {}", this.getClass().getSimpleName());
            throw new IllegalStateException(message);
        }
    }

    /**
     * Temporarily pauses event processing.
     * Subclasses must implement this method to define their specific pause behavior.
     */
    public abstract void pause();

    /**
     * Stops this subscriber, clearing its queue and unsubscribing from the EventManager.
     * After stopping, the subscriber can be restarted.
     */
    public void stop() {
        LOGGER.info("Stopping subscriber: {}", this.getClass().getSimpleName());
        subscriberQueue.clear();
        unsubscribe();
        setStatus(Status.STOPPED);
    }

    /**
     * Determines whether this publisher has been just created.
     *
     * @return true if the publisher is in CREATED status, false otherwise
     */
    public boolean isCreated() {
        return status == Status.CREATED;
    }

    /**
     * Determines whether this publisher is currently running and operational.
     *
     * @return true if the publisher is in RUNNING status, false otherwise
     */
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    /**
     * Determines whether this publisher has been paused.
     *
     * @return true if the publisher is in PAUSED status, false otherwise
     */
    public boolean isPaused() {
        return status == Status.PAUSED;
    }

    /**
     * Determines whether this publisher has been stopped.
     *
     * @return true if the publisher is in STOPPED status, false otherwise
     */
    public boolean isStopped() {
        return status == Status.STOPPED;
    }

    /**
     * Determines whether this publisher has been shut down.
     *
     * @return true if the publisher is in SHUTDOWN status, false otherwise
     */
    public boolean isShutdown() {
        return status == Status.SHUTDOWN;
    }

    /**
     * Checks if the subscriber can be started or resumed.
     *
     * @return true if the subscriber is in CREATED, PAUSED, or STOPPED state
     */
    protected boolean canRun() {
        return isCreated() || isPaused() || isStopped();
    }

    /**
     * Permanently shuts down this subscriber.
     * Subclasses must implement this method to define their specific shutdown behavior.
     */
    public abstract void shutdown();

    /**
     * Sets the status of this subscriber and logs the change.
     * This method is protected to allow subclasses to manage their lifecycle.
     *
     * @param newStatus the new status to set
     * @throws IllegalArgumentException if newStatus is null
     */
    protected void setStatus(Status newStatus) {
        if (newStatus == null) {
            LOGGER.error("Attempted to set null status in {}", this.getClass().getSimpleName());
            throw new IllegalArgumentException("Status cannot be null");
        }

        Status oldStatus = this.status;
        this.status = newStatus;

        if (oldStatus != newStatus) {
            LOGGER.info("Subscriber {} status changed from {} to {}",
                    this.getClass().getSimpleName(), oldStatus, newStatus);
        }
    }

    /**
     * Unregisters this subscriber from the EventManager.
     */
    protected void unsubscribe(){
        LOGGER.debug("Unsubscribing {} from EventManager", this.getClass().getSimpleName());
        eventManager.unsubscribe(this);
    }

    /**
     * Returns a string representation of this subscriber including its class name and status.
     *
     * @return a string representation of this subscriber
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{status=" + status + "}";
    }
}
