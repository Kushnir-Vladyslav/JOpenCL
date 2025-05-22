package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Readable;
import com.jopencl.core.memory.buffer.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a dynamic read-write global OpenCL buffer.
 * This buffer type supports both reading and writing operations while maintaining
 * dynamic resizing capabilities.
 *
 * <p>Dynamic read-write global buffers:
 * <ul>
 *   <li>Support both reading and writing operations</li>
 *   <li>Provide dynamic resizing when needed</li>
 *   <li>Use CL_MEM_READ_WRITE flag for full access</li>
 *   <li>Ensure thread-safe operations</li>
 * </ul></p>
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public class GlobalDynamicReadWriteBuffer
        extends GlobalDynamicBuffer
        implements Readable<GlobalDynamicReadWriteBuffer>,
        Writable<GlobalDynamicReadWriteBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDynamicReadWriteBuffer.class);

    /**
     * Creates a new GlobalDynamicReadWriteBuffer with default configuration.
     * Sets OpenCL flags for read-write access.
     */
    public GlobalDynamicReadWriteBuffer() {
        logger.debug("Creating new GlobalDynamicReadWriteBuffer instance");
    }

    @Override
    public String toString() {
        return String.format("GlobalDynamicReadWriteBuffer{name='%s', capacity=%d, dataClass=%s}",
                getBufferName(), capacity, dataObject.getClass().getSimpleName());
    }
}
