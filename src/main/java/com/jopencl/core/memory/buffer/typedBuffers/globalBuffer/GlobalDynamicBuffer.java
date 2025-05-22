package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.AbstractBuffer;
import com.jopencl.core.memory.buffer.AbstractGlobalBuffer;
import com.jopencl.core.memory.buffer.Dynamical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a dynamic global OpenCL buffer.
 * This buffer type provides automatic resizing capabilities for global memory buffers.
 *
 * <p>Dynamic global buffers:
 * <ul>
 *   <li>Support automatic resizing when needed</li>
 *   <li>Maintain a minimum capacity</li>
 *   <li>Increase capacity by 50% when resizing</li>
 *   <li>Ensure thread-safe resizing operations</li>
 * </ul></p>
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public class GlobalDynamicBuffer
        extends AbstractGlobalBuffer
        implements Dynamical<GlobalDynamicBuffer> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDynamicBuffer.class);

    /**
     * Creates a new GlobalDynamicBuffer with minimum capacity.
     */
    public GlobalDynamicBuffer () {
        logger.debug("Creating new GlobalDynamicBuffer instance with minimum capacity");
        setInitSize(getMinCapacity());
    }

    @Override
    public AbstractBuffer setInitSize(int newSize) {
        initCheck();
        if (newSize < getMinCapacity()) {
            String message = String.format("Buffer size must be positive, got %d for buffer '%s'", newSize, getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Setting initial size to {} for buffer '{}'", newSize, getBufferName());
        this.capacity = newSize;
        return this;
    }

    @Override
    public String toString() {
        return String.format("GlobalDynamicBuffer{name='%s', capacity=%d, dataClass=%s}",
                getBufferName(), capacity, dataObject.getClass().getSimpleName());
    }
}
