package com.jopencl.core.memory.buffer;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class for OpenCL buffer objects.
 * Provides centralized management of buffer lifecycle and operations.
 *
 * <p>This class implements a registry pattern for OpenCL buffers, providing:
 * <ul>
 *   <li>Buffer registration and tracking</li>
 *   <li>Buffer lookup by name</li>
 *   <li>Resource cleanup and management</li>
 *   <li>Buffer removal and destruction</li>
 * </ul></p>
 *
 * <p>Example usage:</p>
 * <pre>
 * BufferManager manager = new BufferManager();
 * AbstractBuffer buffer = new GlobalDynamicBuffer()
 *     .setBufferName("MyBuffer")
 *     .setDataClass(FloatData.class)
 *     .init();
 * manager.registerBuffer(buffer);
 * </pre>
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public class BufferManager {
    private static final Logger logger = LoggerFactory.getLogger(BufferManager.class);
    private final List<AbstractBuffer> buffers = new ArrayList<>();

    /**
     * Registers a buffer with this manager.
     * The buffer will be tracked and managed by this manager instance.
     *
     * @param buffer the buffer to register
     * @throws IllegalArgumentException if the buffer is null
     */
    public void registerBuffer(AbstractBuffer buffer) {
        if (buffer == null) {
            String message = "Cannot register null buffer";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Registering buffer '{}'", buffer.getBufferName());
        buffers.add(buffer);
    }

    /**
     * Releases all buffers managed by this instance.
     * Each buffer is destroyed and removed from management.
     */
    public void releaseAll() {
        logger.debug("Releasing all buffers (count: {})", buffers.size());
        for (AbstractBuffer buffer : buffers) {
            logger.trace("Destroying buffer '{}'", buffer.getBufferName());
            buffer.destroy();
        }
        buffers.clear();
        logger.debug("All buffers released and cleared");
    }

    /**
     * Retrieves a buffer by its name.
     *
     * @param bufferName the name of the buffer to retrieve
     * @return the buffer with the specified name, or null if not found
     * @throws IllegalArgumentException if bufferName is null or empty
     */
    public AbstractBuffer getBuffer(String bufferName) {
        if (bufferName == null || bufferName.trim().isEmpty()) {
            String message = "Buffer name cannot be null or empty";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        logger.trace("Looking up buffer '{}'", bufferName);
        for (AbstractBuffer buffer : buffers) {
            if (buffer.getBufferName().equals(bufferName)) {
                logger.debug("Found buffer '{}'", bufferName);
                return buffer;
            }
        }
        logger.debug("Buffer '{}' not found", bufferName);
        return null;
    }

    /**
     * Removes a buffer from management without destroying it.
     *
     * @param buffer the buffer to remove
     * @throws IllegalArgumentException if the buffer is null
     */
    public void remove (AbstractBuffer buffer) {
        if (buffer == null) {
            String message = "Cannot remove null buffer";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Removing buffer '{}'", buffer.getBufferName());
        buffers.remove(buffer);
    }

    /**
     * Removes a buffer from management by its name without destroying it.
     *
     * @param bufferName the name of the buffer to remove
     * @throws IllegalArgumentException if bufferName is null or empty
     */
    public void remove (String bufferName) {
        if (bufferName == null || bufferName.trim().isEmpty()) {
            String message = "Buffer name cannot be null or empty";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        AbstractBuffer buffer = getBuffer(bufferName);
        if (buffer != null) {
            logger.debug("Removing buffer '{}'", bufferName);
            buffers.remove(buffer);
        } else {
            logger.warn("Cannot remove buffer '{}': not found in buffer manager", bufferName);
        }
    }

    /**
     * Releases and removes a buffer from management.
     * The buffer is destroyed before removal.
     *
     * @param buffer the buffer to release
     * @throws IllegalArgumentException if the buffer is null
     */
    public void release(AbstractBuffer buffer) {
        if (buffer == null) {
            String message = "Cannot release null buffer";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        logger.debug("Releasing buffer '{}'", buffer.getBufferName());
        buffers.remove(buffer);
        buffer.destroy();
    }

    /**
     * Releases and removes a buffer from management by its name.
     * The buffer is destroyed before removal if found.
     *
     * @param bufferName the name of the buffer to release
     * @throws IllegalArgumentException if bufferName is null or empty
     */
    public void release (String bufferName) {
        if (bufferName == null || bufferName.trim().isEmpty()) {
            String message = "Buffer name cannot be null or empty";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        AbstractBuffer buffer = getBuffer(bufferName);
        if (buffer != null) {
            logger.debug("Releasing buffer '{}'", bufferName);
            buffers.remove(buffer);
            buffer.destroy();
        } else {
            logger.warn("Cannot release buffer '{}': not found in buffer manager", bufferName);
        }
    }
}
