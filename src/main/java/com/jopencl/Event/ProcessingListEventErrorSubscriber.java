package com.jopencl.Event;

import com.jopencl.Event.Events.ListEvents;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for subscribers that process lists of events with error handling capabilities.
 * Extends {@link ProcessingListEventSubscriber} by adding error handling and statistics tracking.
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
 * @see ProcessingListEventSubscriber
 * @see EventErrorHandler
 */
public abstract class ProcessingListEventErrorSubscriber extends ProcessingListEventSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingListEventErrorSubscriber.class);

    /** Thread-safe map storing error handlers for each event type */
    protected final Map<Class<? extends Event<?>>, EventErrorHandler> errorHandlers;

    /** Counter for total number of errors occurred */
    protected final AtomicLong totalErrorCounter;
    /** Last exception that occurred during event processing */
    protected volatile Exception lastException;
    /** Last event that failed during processing */
    protected volatile Event<?> lastFailedEvent;

    /**
     * Creates a new ProcessingListEventErrorSubscriber with empty handler maps
     * and initialized error tracking.
     */
    protected ProcessingListEventErrorSubscriber() {
        super();
        this.errorHandlers = new ConcurrentHashMap<>();
        this.totalErrorCounter = new AtomicLong(0);
        LOGGER.debug("Created new ProcessingListEventErrorSubscriber: {}",
                this.getClass().getSimpleName());
    }

    /**
     * Registers both normal and error handlers for a specific event type.
     *
     * @param <T> the type of events to handle
     * @param eventType the class object representing the event type
     * @param handler the handler for processing lists of events of the specified type
     * @param errorHandler the handler for processing errors during event handling
     */
    public <T extends Event<?>> void subscribeEvent(Class<T> eventType, ListEventsHandler<T> handler, EventErrorHandler errorHandler) {
        if (errorHandler != null) {
            LOGGER.debug("Registering error handler for event type {} in {}",
                    eventType.getSimpleName(), this.getClass().getSimpleName());
            errorHandlers.put(eventType, errorHandler);
        }
        super.subscribeEvent(eventType, handler);
    }

    /**
     * Processes a ListEvents instance with error handling.
     * If an exception occurs during processing, it's handled by the appropriate error handler.
     *
     * @param <T> the type of events in the list
     * @param event the ListEvents instance containing the events to process
     */
    @Override
    protected <T extends Event<?>> void processEvent(ListEvents<T> event) {
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
     * @param <T> the type of events in the list
     * @param event the ListEvents instance that caused the exception
     * @param exception the exception that occurred
     */
    protected <T extends Event<?>> void handleException(ListEvents<T> event, Exception exception) {
        LOGGER.error("Error processing list event {} in {}: {}",
                event.getEventType().getSimpleName(),
                this.getClass().getSimpleName(),
                exception.getMessage());

        totalErrorCounter.incrementAndGet();
        lastException = exception;
        lastFailedEvent = event;

        Class<T> eventType = event.getEventType();
        EventErrorHandler errorHandler = errorHandlers.get(eventType);

        if (errorHandler != null) {
            try {
                errorHandler.handle(event, exception);
            } catch (Exception e) {
                LOGGER.error("Error handler failed for list event {} in {}: {}",
                        eventType.getSimpleName(),
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
     * Returns the last exception that occurred during event processing.
     *
     * @return the last exception, or null if no errors have occurred
     */
    public Exception getLastException() {
        return lastException;
    }

    /**
     * Clears all error statistics, including error count and last error information.
     */
    public void clearErrorStatistics() {
        totalErrorCounter.set(0);
        lastException = null;
        lastFailedEvent = null;
    }

    /**
     * Stops this subscriber, clearing all error statistics and handlers.
     */
    @Override
    public void stop() {
        LOGGER.debug("Stopping ProcessingListEventErrorSubscriber: {}",
                this.getClass().getSimpleName());
        clearErrorStatistics();
        clearSubscribeEvents();
        super.stop();
    }
}
