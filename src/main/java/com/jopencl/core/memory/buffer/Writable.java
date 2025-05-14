package com.jopencl.core.memory.buffer;

import com.jopencl.core.memory.data.ConvertToByteBuffer;
import com.jopencl.core.memory.util.CopyDataBufferToBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface Writable <T extends AbstractGlobalBuffer & Writable<T>> {
    default void write(Object arr) {
        write(arr, 0);
    }

    default void write(Object arr, int offset) {
         T buffer = (T) this;

         ConvertToByteBuffer converter = (ConvertToByteBuffer) buffer.dataObject;

         int arrSize = buffer.dataObject.getSizeArray(arr);
         int dataSize = buffer.dataObject.getSizeStruct();

         if (arrSize + offset > buffer.capacity) {
             if (buffer.dynamic) {
                 Dynamical<?> dynamical = (Dynamical<?>) buffer;
                 dynamical.resize((int) ((arrSize + offset) * 1.5));
             } else {
                 throw new IllegalStateException("Size of data array is bigger than size of static OpenCL buffer.");
             }
         }

        ByteBuffer tempNativeBuffer;

         if (buffer.dynamic) {
             tempNativeBuffer = buffer.nativeBuffer.rewind().limit(arrSize * dataSize);
         } else {
             tempNativeBuffer = MemoryUtil.memAlloc(arrSize * dataSize);
         }

         converter.convertToByteBuffer(tempNativeBuffer, arr);

         CL10.clEnqueueWriteBuffer(
                 buffer.openClContext.commandQueue,
                 buffer.clBuffer,
                 true,
                 offset * dataSize,
                 tempNativeBuffer,
                 null,
                 null
         );

         buffer.size += arrSize;

        if (buffer.dynamic) {
            buffer.nativeBuffer.clear();
        } else {
            MemoryUtil.memFree(tempNativeBuffer);
        }
    }

    default void add (Object arr) {
        T buffer = (T) this;
        write(arr, buffer.size);
    }

    default void remove (int index) {
        remove(index, 1);
    }

    default void remove (int index, int num) {
        T buffer = (T) this;
        int dataSize = buffer.dataObject.getSizeStruct();

        CopyDataBufferToBuffer.copyData(
                buffer.openClContext,
                buffer.clBuffer,
                buffer.clBuffer,
                (index) * dataSize,
                (index + num) * dataSize,
                (buffer.size - index - num) * dataSize);

        buffer.size -= num;
    }
}
