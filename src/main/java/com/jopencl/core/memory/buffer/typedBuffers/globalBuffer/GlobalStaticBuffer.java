package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.AbstractGlobalBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a static global OpenCL buffer.
 * This buffer type provides basic global memory functionality with a fixed size.
 * Unlike dynamic buffers, static buffers maintain a constant size throughout their lifecycle.
 *
 * <p>Static global buffers:
 * <ul>
 *   <li>Have fixed size after initialization</li>
 *   <li>Use CL_MEM_READ_WRITE flag by default</li>
 *   <li>Provide basic memory operations</li>
 *   <li>Optimized for fixed-size data structures</li>
 * </ul></p>
 *
 * @since 1.0
 * @author Kushnir-Vladyslav
 */
public class GlobalStaticBuffer extends AbstractGlobalBuffer {
    private static final Logger logger = LoggerFactory.getLogger(GlobalStaticBuffer.class);

    /**
     * Creates a new GlobalStaticBuffer with default configuration.
     * Sets OpenCL flags for read-write access.
     */
    public GlobalStaticBuffer() {
        logger.debug("Creating new GlobalStaticBuffer instance");
    }

    @Override
    public String toString() {
        return String.format("GlobalStaticBuffer{name='%s', capacity=%d, dataClass=%s}",
                getBufferName(), capacity, dataObject.getClass().getSimpleName());
    }
}
