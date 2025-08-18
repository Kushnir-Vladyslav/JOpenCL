/*
 * Copyright 2025 Kushnir Vladyslav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kushnirvladyslav.memory.buffer;

import io.github.kushnirvladyslav.exceptions.BufferOperationException;
import io.github.kushnirvladyslav.memory.util.CopyDataBufferToBuffer;
import io.github.kushnirvladyslav.util.OpenCLErrorUtils;
import org.lwjgl.opencl.CL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for OpenCL buffers that support dynamic memory operations.
 * Provides methods for automatic buffer resizing based on capacity requirements.
 *
 * <p>This interface implements dynamic resizing functionality for OpenCL buffers
 * by working in conjunction with {@link AbstractGlobalBuffer}. It provides complete
 * implementation of resize operations without requiring additional implementation
 * from implementing classes.
 *
 * @param <T> the concrete buffer type that implements this interface
 * @author Vladyslav Kushnir
 * @since 1.0
 */
public interface Dynamical<T extends AbstractGlobalBuffer & Dynamical<T>> {
    Logger logger = LoggerFactory.getLogger(Dynamical.class);

    /**
     * Gets the current capacity multiplier used for buffer resizing.
     *
     * @return the capacity multiplier
     */
    default double getCapacityMultiplier() {
        return 1.5;
    }

    /**
     * Gets the minimum capacity for the buffer.
     *
     * @return the minimum capacity
     */
    default int getMinCapacity() {
        return 10;
    }

    /**
     * Gets the shrink factor threshold for buffer resizing.
     * When capacity/size exceeds this factor, the buffer will be considered for shrinking.
     * Default implementation returns 2.0, meaning the buffer will be shrunk when
     * it is less than half full.
     *
     * @return the shrink factor threshold
     */
    default double getShrinkFactor() {
        return 4.0;
    }

    /**
     * Automatically resizes the buffer based on the required capacity.
     * Determines whether to increase or decrease the buffer size.
     *
     * @param newCapacity the required capacity for the buffer
     * @throws IllegalArgumentException if requiredCapacity is negative
     */
    default void resize(int newCapacity) {
        if (newCapacity < 0) {
            String message = String.format(
                    "Required capacity cannot be negative: %d",
                    newCapacity
            );
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        @SuppressWarnings("unchecked")
        T buffer = (T) this;

        if (buffer.capacity < newCapacity) {
            increase(newCapacity);
        } else {
            decrease(newCapacity);
        }
    }

    /**
     * Increases buffer capacity to accommodate the required size.
     * Uses the capacity multiplier to determine the new size.
     *
     * @param newCapacity the minimum required capacity
     * @throws BufferOperationException memory allocation error on device
     */
    default void increase(int newCapacity) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        int currentCapacity = buffer.getCapacity();

        if (newCapacity > currentCapacity) {
            long oldClBuffer = buffer.clBuffer;

            try {
                buffer.capacity = newCapacity;
                buffer.clBuffer = buffer.createClBuffer();

                int errorCode = CopyDataBufferToBuffer.copyData(
                        buffer.context,
                        oldClBuffer,
                        buffer.getClBuffer(),
                        currentCapacity
                );

                if (errorCode != CL10.CL_SUCCESS) {
                    String message = String.format(
                            "Failed to copy data during buffer increase: OpenCL error - %s",
                            OpenCLErrorUtils.getCLErrorString(errorCode));
                    logger.error(message);
                    throw new BufferOperationException(message, errorCode);
                }

                errorCode = CL10.clReleaseMemObject(oldClBuffer);
                if (errorCode != CL10.CL_SUCCESS) {
                    logger.warn("Failed to release old buffer memory object: OpenCL error - {}",
                            OpenCLErrorUtils.getCLErrorString(errorCode));
                }

                buffer.setAllKernelArgs();
            } catch (Exception e) {
                CL10.clReleaseMemObject(buffer.clBuffer);
                buffer.clBuffer = oldClBuffer;
                buffer.capacity = currentCapacity;
                throw new BufferOperationException("Failed to increase buffer size", e);
            }
        }
    }

    /**
     * Decreases buffer capacity to the specified size.
     * Only decreases if the requested capacity is smaller than current.
     *
     * @param newCapacity the new capacity for the buffer
     * @throws BufferOperationException memory allocation error on device
     */
    default void decrease(int newCapacity) {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;
        int currentCapacity = buffer.getCapacity();

        if (newCapacity < getMinCapacity()) {
            newCapacity = getMinCapacity();
        }

        if (newCapacity < currentCapacity) {
            logger.debug("Decreasing buffer capacity from {} to {}", currentCapacity, newCapacity);

            long oldClBuffer = buffer.clBuffer;

            try {
                buffer.capacity = newCapacity;
                buffer.clBuffer = buffer.createClBuffer();

                int errorCode = CopyDataBufferToBuffer.copyData(
                        buffer.context,
                        oldClBuffer,
                        buffer.clBuffer,
                        newCapacity);

                if (errorCode != CL10.CL_SUCCESS) {
                    String message = String.format(
                            "Failed to copy data during buffer decrease: OpenCL error - %d",
                            OpenCLErrorUtils.getCLErrorString(errorCode));
                    logger.error(message);
                    throw new BufferOperationException(message);
                }

                errorCode = CL10.clReleaseMemObject(oldClBuffer);
                if (errorCode != CL10.CL_SUCCESS) {
                    logger.warn("Failed to release old buffer memory object: OpenCL error - {}",
                            OpenCLErrorUtils.getCLErrorString(errorCode));
                }

                buffer.setAllKernelArgs();

            } catch (Exception e) {
                CL10.clReleaseMemObject(buffer.clBuffer);
                buffer.clBuffer = oldClBuffer;
                buffer.capacity = currentCapacity;
                throw new BufferOperationException("Failed to decrease buffer size", e);
            }
        }
    }

    /**
     * Compacts the buffer by reducing its size to match the current data size.
     * This is useful when the amount of data has significantly decreased and
     * is expected to stay small, helping to free unused memory.
     *
     * @throws BufferOperationException memory allocation error on device
     */
    default void compact() {
        @SuppressWarnings("unchecked")
        T buffer = (T) this;

        logger.debug("Compacting buffer: capacity={}, size={}",
                buffer.capacity, buffer.size);
        decrease(buffer.size);
    }
}
