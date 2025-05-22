package com.jopencl.core.memory.util;

import com.jopencl.core.memory.buffer.AbstractGlobalBuffer;
import com.jopencl.util.OpenClContext;
import org.lwjgl.opencl.CL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for copying data between OpenCL buffers.
 * Provides functionality to safely copy data from one OpenCL buffer to another.
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public class CopyDataBufferToBuffer {
    private static final Logger logger = LoggerFactory.getLogger(CopyDataBufferToBuffer.class);

    /**
     * Copies data between two OpenCL buffers.
     *
     * @param openClContext the OpenCL context
     * @param src the source buffer
     * @param dst the destination buffer
     * @param size the number of bytes to copy
     * @return OpenCL error code
     * @throws IllegalArgumentException if any parameters are invalid
     */
    public static int copyData (OpenClContext openClContext, AbstractGlobalBuffer src, AbstractGlobalBuffer dst, long size) {
        return copyData(openClContext, src, dst, 0, 0, size);
    }

    /**
     * Copies data between two OpenCL buffers with offset support.
     *
     * @param openClContext the OpenCL context
     * @param src the source buffer
     * @param dst the destination buffer
     * @param srcOffset offset in the source buffer
     * @param dstOffset offset in the destination buffer
     * @param size the number of bytes to copy
     * @return OpenCL error code
     * @throws IllegalArgumentException if any parameters are invalid
     */
    public static int copyData (OpenClContext openClContext, AbstractGlobalBuffer src, AbstractGlobalBuffer dst, long srcOffset, long dstOffset, long size) {
        if (openClContext == null || src == null || dst == null) {
            String message = "OpenCL context, source buffer, and destination buffer cannot be null";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (size <= 0) {
            String message = String.format("Copy size must be positive, got %d", size);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (srcOffset < 0 || dstOffset < 0) {
            String message = String.format("Offsets must be non-negative, got src=%d, dst=%d",
                    srcOffset, dstOffset);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (src.getDataClass().equals(dst.getDataClass())) {
            logger.warn("Unsafe copy operation: Data type mismatch between buffers '{}' and '{}'",
                    src.getBufferName(), dst.getBufferName());
        }

        logger.debug("Copying {} bytes from buffer '{}' to '{}' (offsets: src={}, dst={})",
                size, src.getBufferName(), dst.getBufferName(), srcOffset, dstOffset);

        return CL10.clEnqueueCopyBuffer(
                openClContext.commandQueue,
                src.getClBuffer(),
                dst.getClBuffer(),
                srcOffset,
                dstOffset,
                size,
                null,
                null
        );
    }

    /**
     * Copies data between two OpenCL memory objects using their handles.
     *
     * @param openClContext the OpenCL context
     * @param src source memory object handle
     * @param dst destination memory object handle
     * @param size the number of bytes to copy
     * @return OpenCL error code
     * @throws IllegalArgumentException if any parameters are invalid
     */
    public static int copyData (OpenClContext openClContext, long src, long dst, int size) {
        return copyData(openClContext, src, dst, 0, 0, size);
    }

    /**
     * Copies data between two OpenCL memory objects using their handles with offset support.
     *
     * @param openClContext the OpenCL context
     * @param src source memory object handle
     * @param dst destination memory object handle
     * @param srcOffset offset in the source buffer
     * @param dstOffset offset in the destination buffer
     * @param size the number of bytes to copy
     * @return OpenCL error code
     * @throws IllegalArgumentException if any parameters are invalid
     */
    public static int copyData (OpenClContext openClContext, long src, long dst, int srcOffset, long dstOffset, long size) {
        if (openClContext == null) {
            String message = "OpenCL context cannot be null";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (src == 0 || dst == 0) {
            String message = String.format("Invalid memory object handles: src=%d, dst=%d", src, dst);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (size <= 0) {
            String message = String.format("Copy size must be positive, got %d", size);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (srcOffset < 0 || dstOffset < 0) {
            String message = String.format("Offsets must be non-negative, got src=%d, dst=%d",
                    srcOffset, dstOffset);
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        logger.debug("Copying {} bytes between memory objects (src={}, dst={}, offsets: src={}, dst={})",
                size, src, dst, srcOffset, dstOffset);

        return CL10.clEnqueueCopyBuffer(
                openClContext.commandQueue,
                src,
                dst,
                srcOffset,
                dstOffset,
                size,
                null,
                null
        );
    }
}
