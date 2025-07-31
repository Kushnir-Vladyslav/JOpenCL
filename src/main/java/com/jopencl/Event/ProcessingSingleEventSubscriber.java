package com.jopencl.Event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for subscribers that process individual events using type-specific handlers.
 * Provides a mechanism for registering and managing handlers for different event types.
 *
 * <p>This class extends {@link EventSubscriber} and adds support for type-safe event handling
 * through registered {@link SingleEventHandler} instances. Events are processed based on their
 * specific type, with each type potentially having its own handler.
 *
 * <p>Handlers are stored in a thread-safe map, allowing concurrent registration and processing
 * of different event types.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see EventSubscriber
 * @see SingleEventHandler
 */
public abstract class ProcessingSingleEventSubscriber extends EventSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingSingleEventSubscriber.class);

    /** Thread-safe map storing event handlers for each event type */
    protected Map<Class<? extends Event<?>>, SingleEventHandler<? extends Event<?>>> handlers = new ConcurrentHashMap<>();

    /**
     * Creates a new ProcessingSingleEventSubscriber with an empty handler map.
     */
    protected ProcessingSingleEventSubscriber() {
        super();
        this.handlers = new ConcurrentHashMap<>();
        LOGGER.debug("Created new ProcessingSingleEventSubscriber: {}", this.getClass().getSimpleName());
    }

    /**
     * Registers a handler for a specific event type.
     * Multiple calls with the same event type will override the previous handler.
     *
     * @param <T> the type of event to handle
     * @param eventType the class object representing the event type
     * @param handler the handler for processing events of the specified type
     * @throws IllegalArgumentException if either eventType or handler is null
     */
    public <T extends Event<?>> void subscribeEvent(Class<T> eventType, SingleEventHandler<T> handler) {
        if (eventType == null || handler == null) {
            LOGGER.error("Attempted to subscribe with null event type or handler in {}",
                    this.getClass().getSimpleName());
            throw new IllegalArgumentException("Event type and handler cannot be null");
        }

        LOGGER.debug("Registering handler for event type {} in {}",
                eventType.getSimpleName(), this.getClass().getSimpleName());
        handlers.put(eventType, handler);
    }

    /**
     * Processes an event by delegating to its registered handler if one exists.
     *
     * @param <T> the type of event to process
     * @param event the event to process
     */
    @SuppressWarnings("unchecked")
    protected <T extends Event<?>> void processEvent(T event) {
        Class<T> eventType = (Class<T>) event.getClass();
        SingleEventHandler<T> handler = (SingleEventHandler<T>) handlers.get(eventType);

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
            LOGGER.debug("Unregistering handler for event type {} in {}",
                    eventType.getSimpleName(), this.getClass().getSimpleName());
            handlers.remove(eventType);
        }
    }

    /**
     * Removes all registered event handlers.
     */
    public void clearSubscribeEvents() {
        LOGGER.debug("Clearing all event handlers in {}", this.getClass().getSimpleName());
        handlers.clear();
    }

    /**
     * Stops this subscriber by clearing all handlers and performing parent cleanup.
     */
    @Override
    public void stop() {
        LOGGER.debug("Stopping ProcessingSingleEventSubscriber: {}", this.getClass().getSimpleName());
        clearSubscribeEvents();
        super.stop();
    }
}
