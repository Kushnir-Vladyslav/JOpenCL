package com.jopencl.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for subscribers that process events with error handling capabilities.
 * Extends {@link ProcessingSingleEventSubscriber} by adding error handling and statistics tracking.
 *
 * <p>This class maintains:
 * <ul>
 * <li>Error handlers for specific event types</li>
 * <li>Error statistics including total error count</li>
 * <li>Information about the last occurred error</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see ProcessingSingleEventSubscriber
 * @see EventErrorHandler
 */
public abstract class ProcessingSingleEventErrorSubscriber extends ProcessingSingleEventSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingSingleEventErrorSubscriber.class);

    /** Thread-safe map storing error handlers for each event type */
    protected final Map<Class<? extends Event<?>>, EventErrorHandler> errorHandlers;

    /** Counter for total number of errors occurred */
    protected final AtomicLong totalErrorCounter;
    /** Last exception that occurred during event processing */
    protected volatile Exception lastException;
    /** Last event that failed during processing */
    protected volatile Event<?> lastFailedEvent;

    /**
     * Creates a new ProcessingSingleEventErrorSubscriber with empty handler maps
     * and initialized error tracking.
     */
    protected ProcessingSingleEventErrorSubscriber() {
        super();
        this.errorHandlers = new ConcurrentHashMap<>();
        this.totalErrorCounter = new AtomicLong(0);
        LOGGER.debug("Created new ProcessingSingleEventErrorSubscriber: {}",
                this.getClass().getSimpleName());
    }

    /**
     * Registers both normal and error handlers for a specific event type.
     *
     * @param <T> the type of event to handle
     * @param eventType the class object representing the event type
     * @param handler the handler for processing events of the specified type
     * @param errorHandler the handler for processing errors during event handling
     */
    public <T extends Event<?>> void subscribeEvent(Class<T> eventType, SingleEventHandler<T> handler, EventErrorHandler errorHandler) {
        if (errorHandler != null) {
            LOGGER.debug("Registering error handler for event type {} in {}",
                    eventType.getSimpleName(), this.getClass().getSimpleName());
            errorHandlers.put(eventType, errorHandler);
        }
        super.subscribeEvent(eventType, handler);
    }

    /**
     * Processes an event with error handling.
     * If an exception occurs during processing, it's handled by the appropriate error handler.
     *
     * @param <T> the type of event to process
     * @param event the event to process
     */
    @Override
    protected <T extends Event<?>> void processEvent(T event) {
        try {
            super.processEvent(event);
        } catch (Exception e) {
            handleException(event, e);
        }
    }

    /**
     * Unregisters both normal and error handlers for the specified event types.
     *
     * @param eventsType the event types to unregister
     */
    @Override
    public void unsubscribeEvent(Class<?> ... eventsType) {
        if (eventsType == null) {
            return;
        }
        for (Class<?> eventType : eventsType) {
            LOGGER.debug("Unregistering error handler for event type {} in {}",
                    eventType.getSimpleName(), this.getClass().getSimpleName());
            errorHandlers.remove(eventType);
        }
        super.unsubscribeEvent(eventsType);
    }

    /**
     * Removes all registered event and error handlers.
     */
    @Override
    public void clearSubscribeEvents() {
        LOGGER.debug("Clearing all error handlers in {}", this.getClass().getSimpleName());
        errorHandlers.clear();
        super.clearSubscribeEvents();
    }

    /**
     * Handles exceptions that occur during event processing.
     * Updates error statistics and delegates to the appropriate error handler if one exists.
     *
     * @param event the event that caused the exception
     * @param exception the exception that occurred
     */
    protected void handleException(Event<?> event, Exception exception) {
        LOGGER.error("Error processing event {} in {}: {}",
                event.getClass().getSimpleName(),
                this.getClass().getSimpleName(),
                exception.getMessage());

        totalErrorCounter.incrementAndGet();
        lastException = exception;
        lastFailedEvent = event;

        EventErrorHandler errorHandler = errorHandlers.get(event.getClass());

        if (errorHandler != null) {
            try {
                errorHandler.handle(event, exception);
            } catch (Exception e) {
                LOGGER.error("Error handler failed for event {} in {}: {}",
                        event.getClass().getSimpleName(),
                        this.getClass().getSimpleName(),
                        e.getMessage());
            }
        }
    }

    /**
     * Returns the total number of errors that have occurred.
     *
     * @return the total error count
     */
    public long getTotalErrorCount() {
        return totalErrorCounter.get();
    }

    /**
     * Returns the last event that failed during processing.
     *
     * @return the last failed event, or null if no errors have occurred
     */
    public Event<?> getLastFailedEvent() {
        return lastFailedEvent;
    }

    /**
     * Returns the last event that failed during processing.
     *
     * @return the last failed event, or null if no errors have occurred
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * Clears all error statistics, including error count and last error information.
     */
    public void clearErrorStatistics() {
        LOGGER.debug("Clearing error statistics in {}", this.getClass().getSimpleName());
        totalErrorCounter.set(0);
        lastException = null;
        lastFailedEvent = null;
    }

    /**
     * Stops this subscriber, clearing all error statistics and handlers.
     */
    @Override
    public void stop() {
        LOGGER.debug("Stopping ProcessingSingleEventErrorSubscriber: {}",
                this.getClass().getSimpleName());
        clearErrorStatistics();
        clearSubscribeEvents();
        super.stop();
    }
}
