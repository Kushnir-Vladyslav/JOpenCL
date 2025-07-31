package com.jopencl.Event;

/**
 * Enumeration representing the possible states of event system components.
 * Used by various components to track their lifecycle state.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-07-31
 */
public enum Status {
    /**
     * Initial state after instantiation
     */
    CREATED,

    /**
     * Component is actively processing events
     */
    RUNNING,

    /**
     * Component is temporarily suspended but can be resumed
     */
    PAUSED,

    /**
     * Component has been stopped but can be restarted
     */
    STOPPED,

    /**
     * Component has been permanently disabled
     */
    SHUTDOWN
}
