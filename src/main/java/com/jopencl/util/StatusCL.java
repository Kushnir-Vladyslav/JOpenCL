package com.jopencl.util;

/**
 * Enumeration representing the possible states of OpenCL components.
 * Used by OpenCL contexts and devices to track their lifecycle state.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-08-13
 */
public enum StatusCL {
    /**
     * Component is initialized and ready for operations
     */
    READY,

    /**
     * Component is actively executing computations or commands
     */
    RUNNING,

    /**
     * Component has been closed and resources have been released
     */
    CLOSED;
}
