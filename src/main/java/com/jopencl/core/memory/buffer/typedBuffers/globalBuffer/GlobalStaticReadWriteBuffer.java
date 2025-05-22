package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Readable;
import com.jopencl.core.memory.buffer.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a static read-write global OpenCL buffer.
 * This buffer type provides both read and write operations with a fixed size.
 * Unlike dynamic buffers, static buffers maintain a constant size throughout their lifecycle.
 *
 * <p>Static read-write global buffers:
 * <ul>
 *   <li>Support both reading and writing operations</li>
 *   <li>Have fixed size after initialization</li>
 *   <li>Use CL_MEM_READ_WRITE flag for full access</li>
 *   <li>Optimized for fixed-size data structures</li>
 * </ul></p>
 *
 * @since 1.0
 * @author Kushnir-Vladyslav
 */
public class GlobalStaticReadWriteBuffer
        extends GlobalStaticBuffer
        implements Readable<GlobalStaticReadWriteBuffer>,
        Writable<GlobalStaticReadWriteBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalStaticReadWriteBuffer.class);

    /**
     * Creates a new GlobalStaticReadWriteBuffer with default configuration.
     * Sets OpenCL flags for read-write access.
     */
    public GlobalStaticReadWriteBuffer() {
        logger.debug("Creating new GlobalStaticReadWriteBuffer instance");
    }

    @Override
    public String toString() {
        return String.format("GlobalStaticReadWriteBuffer{name='%s', capacity=%d, dataClass=%s}",
                getBufferName(), capacity, dataObject.getClass().getSimpleName());
    }
}
