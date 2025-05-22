package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Readable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a static read-only global OpenCL buffer.
 * This buffer type combines fixed size with read-only access,
 * optimized for constant data that needs to be read from the kernel but never written to.
 *
 * <p>Static read-only global buffers:
 * <ul>
 *   <li>Support reading data from the device</li>
 *   <li>Prevent writing from the device</li>
 *   <li>Use CL_MEM_READ_WRITE flag for full access</li>
 *   <li>Have fixed size after initialization</li>
 * </ul></p>
 *
 * @since 1.0
 * @author Kushnir-Vladyslav
 */
public class GlobalStaticReadOnlyBuffer
        extends GlobalStaticBuffer
        implements Readable<GlobalStaticReadOnlyBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalStaticReadOnlyBuffer.class);

    /**
     * Creates a new GlobalStaticReadOnlyBuffer with default configuration.
     * Sets OpenCL flags for read-only access.
     */
    public GlobalStaticReadOnlyBuffer() {
        logger.debug("Creating new GlobalStaticReadOnlyBuffer instance");
    }

    @Override
    public String toString() {
        return String.format("GlobalStaticReadOnlyBuffer{name='%s', capacity=%d, dataClass=%s}",
                getBufferName(), capacity, dataObject.getClass().getSimpleName());
    }
}
