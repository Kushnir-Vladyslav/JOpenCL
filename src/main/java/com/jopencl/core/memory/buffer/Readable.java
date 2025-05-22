package com.jopencl.core.memory.buffer;

import com.jopencl.core.memory.data.ConvertFromByteBuffer;
import com.jopencl.core.memory.data.Data;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Interface that provides reading capabilities for OpenCL buffers.
 * Implements various methods for reading data from OpenCL buffer to host memory.
 *
 * <p>This interface provides a complete implementation of read operations without requiring
 * additional implementation from implementing classes. It supports both object-based
 * and byte-based reading operations with various options for offset and length.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Read entire buffer
 * Object data = buffer.read();
 *
 * // Read with specific offset and length
 * Object partialData = buffer.read(offset, length);
 *
 * // Read as bytes for dynamic buffers
 * ByteBuffer byteData = buffer.readBytes();
 * </pre>
 *
 * @param <T> The type of buffer that implements this interface, must extend AbstractGlobalBuffer
 *            and implement Readable interface
 * @author Vladyslav Kushnir
 * @since 1.0
 */
public interface Readable<T extends AbstractGlobalBuffer & Readable<T>> {
    Logger logger = LoggerFactory.getLogger(Readable.class);

    /**
     * Reads all data from the buffer using default configuration.
     * If copyHostBuffer is enabled, returns the host buffer directly,
     * otherwise creates a new array and copies the data.
     *
     * @return Object containing the read data
     */
    default Object read() {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        logger.debug("Reading entire buffer '{}'", buffer.getBufferName());

        Object targetArray;
        if (buffer.copyHostBuffer) {
            targetArray = buffer.hostBuffer;
            logger.trace("Using host buffer directly for '{}'", buffer.getBufferName());
        } else {
            ConvertFromByteBuffer converter = (ConvertFromByteBuffer) buffer.dataObject;
            targetArray = converter.createArr(buffer.size);

            logger.trace("Created new array for buffer '{}' with size {}",
                    buffer.getBufferName(), buffer.size);
        }

        return read(0, buffer.size, targetArray);
    }

    /**
     * Reads all data from the buffer into a provided target array.
     *
     * @param targetArray The array where the data will be stored
     * @return The target array filled with data from the buffer
     * @throws IllegalArgumentException if targetArray is null
     */
    default Object read(Object targetArray) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        if (targetArray == null) {
            String message = String.format("Target array cannot be null for buffer '%s'",
                    buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        logger.debug("Reading buffer '{}' into provided array", buffer.getBufferName());
        return read(0, buffer.size, targetArray);
    }

    /**
     * Reads data from the buffer starting at specified offset.
     * Creates a new array to store the data.
     *
     * @param offset Starting position in the buffer to read from
     * @return New array containing the read data
     * @throws IllegalArgumentException if offset is negative or beyond buffer size
     */
    default Object read(int offset) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        if (offset < 0) {
            String message = String.format("Offset cannot be negative: %d for buffer '%s'",
                    offset, buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        ConvertFromByteBuffer converter = (ConvertFromByteBuffer) buffer.dataObject;
        Object targetArray = converter.createArr(buffer.size - offset);

        logger.debug("Reading buffer '{}' from offset {} into new array",
                buffer.getBufferName(), offset);

        return read(offset, buffer.size, targetArray);
    }

    /**
     * Reads data from the buffer starting at specified offset into provided target array.
     *
     * @param offset      Starting position in the buffer to read from
     * @param targetArray The array where the data will be stored
     * @return The target array filled with data from the buffer
     * @throws IllegalArgumentException if offset is negative or targetArray is null
     */
    default Object read(int offset, Object targetArray) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        if (targetArray == null) {
            String message = String.format("Target array cannot be null for buffer '%s'",
                    buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        if (offset < 0) {
            String message = String.format("Offset cannot be negative: %d for buffer '%s'",
                    offset, buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        logger.debug("Reading buffer '{}' from offset {} into provided array",
                buffer.getBufferName(), offset);
        return read(offset, buffer.size, targetArray);
    }

    /**
     * Reads specified amount of data from the buffer starting at given offset.
     * Creates a new array to store the data.
     *
     * @param offset Starting position in the buffer to read from
     * @param len    Number of elements to read
     * @return New array containing the read data
     * @throws IllegalArgumentException if offset or length parameters are invalid
     */
    default Object read(int offset, int len) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        if (offset < 0) {
            String message = String.format("Offset cannot be negative: %d for buffer '%s'",
                    offset, buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        if (len <= 0) {
            String message = String.format("Length must be positive: %d for buffer '%s'",
                    len, buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        ConvertFromByteBuffer converter = (ConvertFromByteBuffer) buffer.dataObject;
        Object targetArray = converter.createArr(len - offset);

        logger.debug("Reading {} elements from buffer '{}' starting at offset {}",
                len, buffer.getBufferName(), offset);
        return read(offset, len, targetArray);
    }

    /**
     * Main implementation of read operation. Reads specified amount of data
     * from the buffer starting at given offset into provided target array.
     *
     * @param offset      Starting position in the buffer to read from
     * @param len         Number of elements to read
     * @param targetArray The array where the data will be stored
     * @return The target array filled with data from the buffer
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException    if the read operation fails
     */
    default Object read(int offset, int len, Object targetArray) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;

        if (offset + len > buffer.capacity) {
            String message = String.format(
                    "Attempt to read outside buffer bounds: offset=%d, length=%d, capacity=%d for buffer '%s'",
                    offset, len, buffer.capacity, buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (offset + len > buffer.size) {
            logger.warn("Reading uninitialized data: offset={}, length={}, size={} for buffer '{}'",
                    offset, len, buffer.size, buffer.getBufferName());
        }

        Data data = buffer.dataObject;
        ConvertFromByteBuffer converter = (ConvertFromByteBuffer) data;
        ByteBuffer tempNativeBuffer = null;

        try {
            if (buffer.copyHostBuffer) {
                tempNativeBuffer = buffer.nativeBuffer.rewind().limit(len);
            } else {
                tempNativeBuffer = MemoryUtil.memAlloc(len * data.getSizeStruct());
                if (tempNativeBuffer == null) {
                    throw new IllegalStateException("Failed to allocate temporary native buffer");
                }
            }

            logger.debug("Reading {} elements from OpenCL buffer '{}' at offset {}",
                    len, buffer.getBufferName(), offset);

            int errorCode = CL10.clEnqueueReadBuffer(
                    buffer.openClContext.commandQueue,
                    buffer.clBuffer,
                    true,
                    offset * data.getSizeStruct(),
                    tempNativeBuffer,
                    null,
                    null
            );

            if (errorCode != CL10.CL_SUCCESS) {
                String message = String.format(
                        "OpenCL read buffer failed for buffer '%s': error code %d",
                        buffer.getBufferName(), errorCode);
                logger.error(message);
                throw new IllegalStateException(message);
            }

            converter.convertFromByteBuffer(tempNativeBuffer.rewind(), targetArray);

            if (buffer.copyHostBuffer) {
                buffer.nativeBuffer.clear();
            }

            return targetArray;

        } catch (Exception e) {
            String message = String.format("Failed to read from buffer '%s'", buffer.getBufferName());
            logger.error(message, e);
            throw new IllegalStateException(message, e);
        } finally {
            if (!buffer.copyHostBuffer && tempNativeBuffer != null) {
                MemoryUtil.memFree(tempNativeBuffer);
            }
        }
    }

    /**
     * Reads all data from the buffer as bytes.
     * This operation is only available for dynamic buffers.
     *
     * @return ByteBuffer containing the read data
     * @throws IllegalStateException if the buffer is not dynamic
     */
    default ByteBuffer readBytes() {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        if (!(buffer instanceof Dynamical<?>)) {
            String message = String.format(
                    "Buffer '%s' is not dynamic, cannot perform byte-based read operation",
                    buffer.getBufferName());
            logger.error(message);
            throw new IllegalStateException(message);
        }

        logger.debug("Reading entire buffer '{}' as bytes", buffer.getBufferName());
        return readBytes(0, buffer.nativeBuffer);
    }

    /**
     * Reads all data from the buffer into provided ByteBuffer.
     *
     * @param tempNativeBuffer The ByteBuffer where the data will be stored
     * @return The provided ByteBuffer filled with data from the buffer
     * @throws IllegalArgumentException if tempNativeBuffer is null
     */
    default ByteBuffer readBytes(ByteBuffer tempNativeBuffer) {
        if (tempNativeBuffer == null) {
            String message = "Temporary native buffer cannot be null";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        logger.debug("Reading buffer into provided ByteBuffer");
        return readBytes(0, tempNativeBuffer);
    }

    /**
     * Reads data from the buffer starting at specified offset as bytes.
     *
     * @param offset Starting position in the buffer to read from
     * @return ByteBuffer containing the read data
     * @throws IllegalStateException    if the buffer is not dynamic
     * @throws IllegalArgumentException if offset is invalid
     */
    default ByteBuffer readBytes(int offset) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        if (!(buffer instanceof Dynamical<?>)) {
            String message = String.format(
                    "Buffer '%s' is not dynamic, cannot perform byte-based read operation",
                    buffer.getBufferName());
            logger.error(message);
            throw new IllegalStateException(message);
        }

        if (offset < 0) {
            String message = String.format("Offset cannot be negative: %d for buffer '%s'",
                    offset, buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        int len = (buffer.size - offset)
                * buffer.dataObject.getSizeStruct();
        ByteBuffer tempNativeBuffer = buffer
                .nativeBuffer
                .slice(0, len);

        logger.debug("Reading buffer '{}' from offset {} as bytes", buffer.getBufferName(), offset);
        return readBytes(offset, tempNativeBuffer);
    }

    /**
     * Reads data from the buffer starting at specified offset into provided ByteBuffer.
     *
     * @param offset           Starting position in the buffer to read from
     * @param tempNativeBuffer The ByteBuffer where the data will be stored
     * @return The provided ByteBuffer filled with data from the buffer
     * @throws IllegalArgumentException if any parameters are invalid
     * @throws IllegalStateException    if the read operation fails
     */
    default ByteBuffer readBytes(int offset, ByteBuffer tempNativeBuffer) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        Data data = buffer.dataObject;

        if (tempNativeBuffer == null) {
            String message = String.format("Temporary native buffer cannot be null for buffer '%s'",
                    buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        int elementCount = tempNativeBuffer.capacity() / data.getSizeStruct();
        if (offset + elementCount > buffer.capacity) {
            String message = String.format(
                    "Attempt to read outside buffer bounds: offset=%d, elements=%d, capacity=%d for buffer '%s'",
                    offset, elementCount, buffer.capacity, buffer.getBufferName());
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (offset + elementCount > buffer.size) {
            logger.warn("Reading uninitialized data: offset={}, elements={}, size={} for buffer '{}'",
                    offset, elementCount, buffer.size, buffer.getBufferName());
        }

        try {
            logger.debug("Reading {} bytes from buffer '{}' at offset {}",
                    tempNativeBuffer.capacity(), buffer.getBufferName(), offset);

            int errorCode = CL10.clEnqueueReadBuffer(
                    buffer.openClContext.commandQueue,
                    buffer.clBuffer,
                    true,
                    offset * data.getSizeStruct(),
                    tempNativeBuffer.rewind(),
                    null,
                    null
            );

            if (errorCode != CL10.CL_SUCCESS) {
                String message = String.format(
                        "OpenCL read buffer failed for buffer '%s': error code %d",
                        buffer.getBufferName(), errorCode);
                logger.error(message);
                throw new IllegalStateException(message);
            }

            return tempNativeBuffer;
        } catch (Exception e) {
            String message = String.format("Failed to read bytes from buffer '%s'",
                    buffer.getBufferName());
            logger.error(message, e);
            throw new IllegalStateException(message, e);
        }
    }
}
