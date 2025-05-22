package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Readable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a dynamic read-only global OpenCL buffer.
 * This buffer type combines dynamic resizing capabilities with read-only access,
 * optimized for data that needs to be read from the kernel but never written to.
 *
 * <p>Dynamic read-only global buffers:
 * <ul>
 *   <li>Support reading data from the device</li>
 *   <li>Prevent writing from the device</li>
 *   <li>Use CL_MEM_READ_WRITE flag for full access</li>
 *   <li>Support automatic resizing</li>
 * </ul></p>
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public class GlobalDynamicReadOnlyBuffer
        extends GlobalDynamicBuffer
        implements Readable<GlobalDynamicReadOnlyBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDynamicReadOnlyBuffer.class);

    /**
     * Creates a new GlobalDynamicReadOnlyBuffer with default configuration.
     * Sets OpenCL flags for read-only access.
     */
    public GlobalDynamicReadOnlyBuffer () {
        logger.debug("Creating new GlobalDynamicReadOnlyBuffer instance");
    }

    @Override
    public String toString() {
        return String.format("GlobalDynamicReadOnlyBuffer{name='%s', capacity=%d, dataClass=%s}",
                getBufferName(), capacity, dataObject.getClass().getSimpleName());
    }
}
