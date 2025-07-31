package com.jopencl.Event;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstract base class for all events in the event management system.
 * Each event contains creation timestamp, priority level, and associated data.
 *
 * <p>Events are comparable by priority using {@link #priorityComparator(Event, Event)}
 * with higher priority events being processed first.
 *
 * @param <T> the type of data associated with this event
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-06-11
 */
public abstract class Event <T> {
    /** The exact time when this event was created */
    protected final Instant createdTime = Instant.now();
    /** The priority level of this event, defaults to MEDIUM */
    protected EventPriority priority = EventPriority.MEDIUM;
    /** The data payload associated with this event */
    protected T data;

    /**
     * Returns the data associated with this event.
     *
     * @return the event data, may be null
     */
    public T getData() {
        return data;
    }

    /**
     * Returns the priority level of this event.
     *
     * @return the event priority, never null
     */
    public EventPriority getPriority () {
        return priority;
    }

    /**
     * Returns the exact time when this event was created.
     *
     * @return the creation timestamp, never null
     */
    public Instant getCreatedTime() {
        return createdTime;
    }

    /**
     * Returns the creation time as milliseconds since epoch.
     *
     * @return the creation time in milliseconds since January 1, 1970 GMT
     */
    public long getCreatedTimeMillis() {
        return createdTime.toEpochMilli();
    }

    /**
     * Calculates how long this event has existed since creation.
     *
     * @return the duration in milliseconds since this event was created
     */
    public long getExistingTimeMillis() {
        return Duration.between(createdTime, Instant.now()).toMillis();
    }

    /**
     * Returns a string representation of this event including creation time,
     * priority, and data.
     *
     * @return a string representation of this event
     */
    @Override
    public String toString() {
        return "Event{" +
                "createdTime=" + createdTime +
                ", priority=" + priority +
                ", data=" + data +
                '}';
    }

    /**
     * Compares two events by priority for ordering in priority queues.
     * Events with higher priority values are considered "smaller" and will
     * be processed first in a priority queue.
     *
     * <p>If priorities are equal, events are considered equal for ordering purposes.
     *
     * @param event1 the first event to compare, must not be null
     * @param event2 the second event to compare, must not be null
     * @return a negative integer if event1 has higher priority than event2,
     *         zero if they have equal priority,
     *         a positive integer if event1 has lower priority than event2
     * @throws NullPointerException if either event is null
     * @see EventPriority
     */
    static public int priorityComparator (Event<?> event1, Event<?> event2) {
        if (event1 == null || event2 == null) {
            throw new NullPointerException("Events cannot be null for comparison");
        }

        EventPriority priority1 = event1.getPriority();
        EventPriority priority2 = event2.getPriority();

        // Higher priority values should come first (reverse order)
        return priority2.getValue() - priority1.getValue();
    }
}
