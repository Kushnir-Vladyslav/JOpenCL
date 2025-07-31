package com.jopencl.Event.EventPublishers;

import com.jopencl.Event.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * Synchronous event publisher that filters events based on a condition.
 * Extends {@link SyncEventPublisher} to provide conditional event publishing.
 *
 * <p>This publisher:
 * <ul>
 * <li>Evaluates each event against a predicate before publishing</li>
 * <li>Allows dynamic updates of the filtering condition</li>
 * <li>Maintains synchronous publishing behavior from parent class</li>
 * </ul>
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 * @see SyncEventPublisher
 */
public class ConditionalSyncEventPublisher extends SyncEventPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionalSyncEventPublisher.class);

    /** Predicate used to filter events */
    private Predicate<Event<?>> condition;

    /**
     * Creates a new ConditionalSyncEventPublisher with specified filtering condition.
     *
     * @param condition the predicate to test events against
     * @throws IllegalArgumentException if condition is null
     */
    public ConditionalSyncEventPublisher(Predicate<Event<?>> condition) {
        super();
        if (condition == null) {
            LOGGER.error("Attempted to create publisher with null condition");
            throw new IllegalArgumentException("Condition cannot be null");
        }
        this.condition = condition;
        LOGGER.debug("Created ConditionalSyncEventPublisher");
    }

    /**
     * Updates the filtering condition used to evaluate events.
     *
     * @param condition the new predicate to test events against
     * @throws IllegalArgumentException if condition is null
     * @throws IllegalStateException if publisher is shut down
     */
    public void setCondition(Predicate<Event<?>> condition) {
        if (condition == null) {
            LOGGER.error("Attempted to set null condition");
            throw new IllegalArgumentException("Condition cannot be null");
        }

        checkNotShutdown();

        LOGGER.debug("Updating event filtering condition");
        this.condition = condition;
    }

    /**
     * Publishes an event if it passes the filtering condition.
     *
     * @param event the event to evaluate and potentially publish
     * @throws IllegalArgumentException if event is null
     */
    public void publish(Event<?> event) {
        if (event == null) {
            LOGGER.error("Attempted to publish null event");
            throw new IllegalArgumentException("Event cannot be null");
        }


        if (condition.test(event)) {
            LOGGER.debug("Event {} passed condition, publishing", event.getClass().getSimpleName());
            super.publish(event);
        } else {
            LOGGER.debug("Event {} filtered out by condition", event.getClass().getSimpleName());
        }
    }
}
