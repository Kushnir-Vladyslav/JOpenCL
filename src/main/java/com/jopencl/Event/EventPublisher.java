package com.jopencl.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for components that publish events to the event management system.
 * Provides common functionality for event publishing and lifecycle management.
 *
 * <p>This class maintains a reference to the singleton {@link EventManager} and provides
 * protected methods for subclasses to publish events safely. It also handles basic
 * lifecycle management with status tracking.
 *
 * <p>Subclasses must implement the {@link #shutdown()} method to define their specific
 * cleanup behavior when the publisher is being shut down.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-06-11
 * @see EventManager
 * @see Event
 */
public abstract class EventPublisher {
    /** Logger instance for this publisher */
    private static final Logger LOGGER = LoggerFactory.getLogger(EventPublisher.class);

    /** Reference to the singleton event manager */
    protected final EventManager eventManager = EventManager.getInstance();
    /** Current operational status of this publisher */
    protected volatile Status status = Status.RUNNING;

    /**
     * Publishes an event to the event management system.
     * This method is thread-safe and will validate the publisher's status before publishing.
     *
     * @param event the event to publish, must not be null
     * @throws IllegalArgumentException if the event is null
     * @throws IllegalStateException if this publisher has been shut down
     */
    protected void publishEvent(Event<?> event) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event from {}", this.getClass().getSimpleName());
            throw new IllegalArgumentException("Event cannot be null");
        }

        checkNotShutdown();

        try {
            eventManager.publish(event);
        } catch (Exception e) {
            LOGGER.error("Failed to publish event {} from {}: {}",
                    event, this.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Returns the current operational status of this publisher.
     *
     * @return the current status, never null
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Checks if this publisher is still operational and not shut down.
     * This method is called automatically by {@link #publishEvent(Event)} but can
     * also be used by subclasses to validate state before performing operations.
     *
     * @throws IllegalStateException if this publisher has been shut down
     */
    protected void checkNotShutdown () {
        if (status == Status.SHUTDOWN) {
            String message = this.getClass().getSimpleName() + " was already shut down and cannot be used";
            LOGGER.error("Operation attempted on shut down publisher: {}", this.getClass().getSimpleName());
            throw new IllegalStateException(message);
        }
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
     * Determines whether this publisher has been shut down.
     *
     * @return true if the publisher is in SHUTDOWN status, false otherwise
     */
    public boolean isShutdown() {
        return status == Status.SHUTDOWN;
    }

    /**
     * Shuts down this publisher and releases any resources.
     * After calling this method, the publisher cannot be used for publishing events.
     *
     * <p>Subclasses must implement this method to define their specific shutdown behavior,
     * such as cleaning up resources, stopping background threads, etc.
     * Subclasses should call {@code setStatus(Status.SHUTDOWN)} when shutdown is complete.
     *
     * @throws IllegalStateException if shutdown fails or if already shut down
     */
    public abstract void shutdown();

    /**
     * Returns a string representation of this publisher including its class name and status.
     *
     * @return a string representation of this publisher
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{status=" + status + "}";
    }
}

