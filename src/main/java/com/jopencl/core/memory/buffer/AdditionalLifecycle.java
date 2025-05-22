package com.jopencl.core.memory.buffer;

/**
 * Interface defining additional lifecycle operations for OpenCL buffers.
 * Implementations can provide custom initialization and cleanup logic
 * that will be called during the standard buffer lifecycle.
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public interface AdditionalLifecycle {

    /**
     * Additional initialization logic to be executed after base initialization.
     * This method is called at the end of the {@link AbstractBuffer#init()} method
     * if the buffer implements this interface.
     *
     * @throws IllegalStateException if initialization fails
     */
    void additionalInit();

    /**
     * Additional cleanup logic to be executed during buffer destruction.
     * This method is called before the base cleanup in {@link AbstractBuffer#destroy()}
     * if the buffer implements this interface.
     */
    void additionalCleanup();
}
