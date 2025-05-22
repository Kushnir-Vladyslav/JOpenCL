package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a static write-only global OpenCL buffer.
 * This buffer type provides write-only access with a fixed size,
 * optimized for scenarios where data only needs to be written to the device.
 *
 * <p>Static write-only global buffers:
 * <ul>
 *   <li>Support only writing operations</li>
 *   <li>Have fixed size after initialization</li>
 *   <li>Use CL_MEM_READ_WRITE flag for full access</li>
 *   <li>Prevent reading operations for better performance</li>
 * </ul></p>
 *
 * @since 1.0
 * @author Kushnir-Vladyslav
 */
public class GlobalStaticWriteOnlyBuffer
        extends GlobalStaticBuffer implements Writable<GlobalStaticWriteOnlyBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalStaticWriteOnlyBuffer.class);

    /**
     * Creates a new GlobalStaticWriteOnlyBuffer with default configuration.
     * Sets OpenCL flags for write-only access.
     */
    public GlobalStaticWriteOnlyBuffer() {
        logger.debug("Creating new GlobalStaticWriteOnlyBuffer instance");
    }

    @Override
    public String toString() {
        return String.format("GlobalStaticWriteOnlyBuffer{name='%s', capacity=%d, dataClass=%s}",
                getBufferName(), capacity, dataObject.getClass().getSimpleName());
    }
}
