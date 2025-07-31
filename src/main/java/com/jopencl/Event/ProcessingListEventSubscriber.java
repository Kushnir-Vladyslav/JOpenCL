package com.jopencl.Event;

import com.jopencl.Event.Events.ListEvents;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for subscribers that process lists of events using type-specific handlers.
 * This class extends EventSubscriber to provide functionality for handling batches of events
 * through {@link ListEvents} containers.
 *
 * <p>Unlike {@link ProcessingSingleEventSubscriber} which processes individual events,
 * this class is designed to handle multiple events of the same type in batch operations.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see EventSubscriber
 * @see ListEvents
 * @see ListEventsHandler
 */
public abstract class ProcessingListEventSubscriber extends EventSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingListEventSubscriber.class);

    /** Thread-safe map storing event handlers for each event type */
    protected final Map<Class<? extends Event<?>>, ListEventsHandler<? extends Event<?>>> handlers;

    /**
     * Creates a new ProcessingListEventSubscriber with an empty handler map.
     */
    protected ProcessingListEventSubscriber() {
        super();
        this.handlers = new ConcurrentHashMap<>();
        LOGGER.debug("Created new ProcessingListEventSubscriber: {}",
                this.getClass().getSimpleName());
    }

    /**
     * Registers a handler for a specific event type.
     * Multiple calls with the same event type will override the previous handler.
     *
     * @param <T> the type of events to handle
     * @param eventType the class object representing the event type
     * @param handler the handler for processing lists of events of the specified type
     * @throws IllegalArgumentException if either eventType or handler is null
     */
    public <T extends Event<?>> void subscribeEvent(Class<T> eventType, ListEventsHandler<T> handler) {
        if (eventType == null || handler == null) {
            LOGGER.error("Attempted to subscribe with null event type or handler in {}",
                    this.getClass().getSimpleName());
            throw new IllegalArgumentException("Event type and handler cannot be null");
        }

        LOGGER.debug("Registering list handler for event type {} in {}",
                eventType.getSimpleName(), this.getClass().getSimpleName());
        handlers.put(eventType, handler);
    }

    /**
     * Processes a ListEvents instance by delegating to its registered handler if one exists.
     *
     * @param <T> the type of events in the list
     * @param event the ListEvents instance containing the events to process
     */
    @SuppressWarnings("unchecked")
    protected <T extends Event<?>> void processEvent(ListEvents<T> event) {
        Class<T> eventType = event.getEventType();
        ListEventsHandler<T> handler = (ListEventsHandler<T>) handlers.get(eventType);

        if (handler != null) {
            handler.handle(event);
        }
    }

    /**
     * Unregisters handlers for the specified event types.
     * If no event types are specified or if the array is null, no action is taken.
     *
     * @param eventsType the event types to unregister
     */
    public void unsubscribeEvent(Class<?> ... eventsType) {
        if (eventsType == null) {
            return;
        }
        for (Class<?> eventType : eventsType) {
            LOGGER.debug("Unregistering list handler for event type {} in {}",
                    eventType.getSimpleName(), this.getClass().getSimpleName());
            handlers.remove(eventType);
        }
    }

    /**
     * Removes all registered event handlers.
     */
    public void clearSubscribeEvents() {
        LOGGER.debug("Clearing all list event handlers in {}", this.getClass().getSimpleName());
        handlers.clear();
    }

    /**
     * Stops this subscriber by clearing all handlers and performing parent cleanup.
     */
    @Override
    public void stop() {
        LOGGER.debug("Stopping ProcessingListEventSubscriber: {}", this.getClass().getSimpleName());
        clearSubscribeEvents();
        super.stop();
    }
}
