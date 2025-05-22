package com.jopencl.core.memory.data;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Interface for converting data structures to ByteBuffer format for OpenCL operations.
 * This interface is used in conjunction with the Data interface to provide
 * serialization capabilities for OpenCL buffer operations.
 *
 * <p>Implementations of this interface should handle the conversion of their specific
 * data type to a ByteBuffer format that can be used directly with OpenCL. The conversion
 * should maintain data integrity and handle proper byte ordering.</p>
 *
 * <p>Example implementation for float arrays:</p>
 * <pre>
 * public class FloatData implements Data, ConvertToByteBuffer {
 *     {@literal @}Override
 *     public void convertToByteBuffer(ByteBuffer buffer, Object source) {
 *         if (!(source instanceof float[])) {
 *             throw new IllegalArgumentException("Source must be float[]");
 *         }
 *         float[] data = (float[]) source;
 *         buffer.asFloatBuffer().put(data);
 *     }
 * }
 * </pre>
 *
 * <p>Important considerations for implementations:</p>
 * <ul>
 *   <li>The ByteBuffer should be properly positioned before writing</li>
 *   <li>The implementation should handle boundary checks</li>
 *   <li>The byte order should match the OpenCL device requirements</li>
 *   <li>The conversion should be efficient for large data sets</li>
 * </ul>
 *
 * @see java.nio.ByteBuffer
 * @see com.jopencl.core.memory.data.Data
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public interface ConvertToByteBuffer {

    /**
     * Converts the source data structure to bytes and writes them to the provided ByteBuffer.
     * This method is called during OpenCL buffer write operations to prepare data
     * for transfer to the device.
     *
     * <p>The implementation should:</p>
     * <ul>
     *   <li>Validate the source object type</li>
     *   <li>Ensure the buffer has sufficient remaining capacity</li>
     *   <li>Handle the conversion efficiently</li>
     *   <li>Maintain proper byte ordering</li>
     * </ul>
     *
     * @param buffer the destination ByteBuffer to write the converted data into
     * @param source the source object to convert (typically an array)
     * @throws IllegalArgumentException if the source object is null or of invalid type
     * @throws BufferOverflowException if the buffer's remaining capacity is insufficient
     */
    void convertToByteBuffer(ByteBuffer buffer, Object source);
}
