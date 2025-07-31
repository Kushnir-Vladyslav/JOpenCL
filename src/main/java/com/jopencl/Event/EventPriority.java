package com.jopencl.Event;

/**
 * Enumeration of event priority levels used for event processing order.
 * Higher numerical values indicate higher priority in the event queue.
 *
 * <p>Priority levels are used by the EventManager to determine the processing
 * order of events in the priority queue, with CRITICAL events processed first
 * and LOW events processed last.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-06-11
 * @see Event#priorityComparator(Event, Event)
 */
public enum EventPriority {
    /**
     * Lowest priority level - events are processed last.
     * Used for non-urgent background tasks and cleanup operations.
     */
    LOW(0),

    /**
     * Medium priority level - default priority for most events.
     * Used for regular application operations and user interactions.
     */
    MEDIUM(1),

    /**
     * High priority level - events are processed before medium and low priority events.
     * Used for important operations that should be handled promptly.
     */
    HIGH(2),

    /**
     * Critical priority level - events are processed first.
     * Used for system-critical operations, error handling, and shutdown procedures.
     */
    CRITICAL(3);

    /** The numerical value representing this priority level */
    private final int value;

    /**
     * Creates an EventPriority with the specified numerical value.
     * Higher values indicate higher priority.
     *
     * @param value the numerical priority value
     */
    EventPriority(int value) {
        this.value = value;
    }

    /**
     * Returns the numerical value of this priority level.
     * Higher values indicate higher priority and will be processed first.
     *
     * @return the numerical priority value (0-3)
     */
    public int getValue() {
        return value;
    }


    /**
     * Returns the EventPriority with the highest priority value.
     *
     * @return CRITICAL priority
     */
    public static EventPriority highest() {
        return CRITICAL;
    }

    /**
     * Returns the EventPriority with the lowest priority value.
     *
     * @return LOW priority
     */
    public static EventPriority lowest() {
        return LOW;
    }

    /**
     * Returns the default EventPriority used when no priority is specified.
     *
     * @return MEDIUM priority
     */
    public static EventPriority defaultPriority() {
        return MEDIUM;
    }

    /**
     * Determines if this priority is higher than the specified priority.
     *
     * @param other the priority to compare against
     * @return true if this priority is higher than the other priority
     * @throws NullPointerException if other is null
     */
    public boolean isHigherThan(EventPriority other) {
        if (other == null) {
            throw new NullPointerException("Priority cannot be null for comparison");
        }
        return this.value > other.value;
    }

    /**
     * Determines if this priority is lower than the specified priority.
     *
     * @param other the priority to compare against
     * @return true if this priority is lower than the other priority
     * @throws NullPointerException if other is null
     */
    public boolean isLowerThan(EventPriority other) {
        if (other == null) {
            throw new NullPointerException("Priority cannot be null for comparison");
        }
        return this.value < other.value;
    }

    /**
     * Returns a string representation including both name and value.
     *
     * @return string in format "PRIORITY_NAME(value)"
     */
    @Override
    public String toString() {
        return name() + "(" + value + ")";
    }
}
