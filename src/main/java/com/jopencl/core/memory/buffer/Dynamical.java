package com.jopencl.core.memory.buffer;

import com.jopencl.core.memory.util.CopyDataBufferToBuffer;
import org.lwjgl.opencl.CL10;

public interface Dynamical <T extends AbstractGlobalBuffer & Dynamical<T>> {

    default void resize (int newSize) {
        T buffer = (T) this;

        if (buffer.capacity < newSize) {
            increaseTo(newSize);
        } else {
            reduceTo(newSize);
        }
    }

    default void reduceTo (int newSize) {
        T buffer = (T) this;

        if (newSize < buffer.capacity) {
            long oldClBuffer = buffer.clBuffer;

            CopyDataBufferToBuffer.copyData(
                    buffer.openClContext,
                    oldClBuffer,
                    buffer.clBuffer,
                    newSize);

            buffer.capacity = newSize;
            buffer.clBuffer = buffer.createClBuffer();


            if (oldClBuffer != 0) {
                CL10.clReleaseMemObject(oldClBuffer);
            }

            buffer.setAllKernelArg();
        }
    }

    default void compact () {
        T buffer = (T) this;
        reduceTo(buffer.size);
    }

    default void increaseTo (int newSize) {
        T buffer = (T) this;

        if (newSize > buffer.capacity) {
            long oldClBuffer = buffer.clBuffer;
            int oldClBufferSize = buffer.capacity;

            CopyDataBufferToBuffer.copyData(
                    buffer.openClContext,
                    oldClBuffer,
                    buffer.clBuffer,
                    oldClBufferSize);

            buffer.capacity = newSize;
            buffer.clBuffer = buffer.createClBuffer();


            if (oldClBuffer != 0) {
                CL10.clReleaseMemObject(oldClBuffer);
            }

            buffer.setAllKernelArg();
        }
    }
}
