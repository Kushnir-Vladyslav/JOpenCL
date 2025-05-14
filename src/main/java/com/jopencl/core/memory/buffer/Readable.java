package com.jopencl.core.memory.buffer;

import com.jopencl.core.memory.data.ConvertFromByteBuffer;
import com.jopencl.core.memory.data.Data;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public interface Readable <T extends AbstractGlobalBuffer & Readable<T>> {
    default Object read() {
        T buffer = (T) this;

        Object targetArray;

        if (buffer.copyHostBuffer) {
            targetArray = buffer.hostBuffer;
        } else {
            ConvertFromByteBuffer converter = (ConvertFromByteBuffer) buffer.dataObject;

            targetArray = converter.createArr(buffer.size);
        }

        return read(0, buffer.size, targetArray);
    }

    default Object read (Object targetArray) {
        T buffer = (T) this;

        return read(0, buffer.size, targetArray);
    }

    default Object read (int offset) {
        T buffer = (T) this;
        ConvertFromByteBuffer converter = (ConvertFromByteBuffer) buffer.dataObject;

        Object targetArray = converter.createArr(buffer.size - offset);

        return read(offset, buffer.size, targetArray);
    }

    default Object read (int offset, Object targetArray) {
        T buffer = (T) this;

        return read(offset, buffer.size, targetArray);
    }

    default Object read (int offset, int len) {
        T buffer = (T) this;
        ConvertFromByteBuffer converter = (ConvertFromByteBuffer) buffer.dataObject;

        Object targetArray = converter.createArr(len - offset);

        return read(offset, len, targetArray);
    }

    default Object read(int offset, int len, Object targetArray) {
         T buffer = (T) this;

        if (offset + len > buffer.capacity) {
            throw new ArrayStoreException("Attempt to read data outside the OpenCl buffer.");
        }

        if (offset + len > buffer.size) {
            System.err.println("The data read is outside the initialized volume.");
        }


         Data data = buffer.dataObject;

         ConvertFromByteBuffer converter = (ConvertFromByteBuffer) data;

         ByteBuffer tempNativeBuffer;

         if (buffer.copyHostBuffer) {
             tempNativeBuffer = buffer.nativeBuffer.rewind().limit(len);
         } else {
             tempNativeBuffer = MemoryUtil.memAlloc(len * data.getSizeStruct());
         }

        CL10.clEnqueueReadBuffer(
                buffer.openClContext.commandQueue,
                buffer.clBuffer,
                true,
                offset * data.getSizeStruct(),
                tempNativeBuffer,
                null,
                null
        );

        converter.convertFromByteBuffer(tempNativeBuffer.rewind(), targetArray);

        if (buffer.copyHostBuffer) {
            buffer.nativeBuffer.clear();
        } else {
            MemoryUtil.memFree(tempNativeBuffer);
        }

        return targetArray;
    }

    default ByteBuffer readBytes () {
        T buffer = (T) this;
        if (!buffer.dynamic) {
            throw new IllegalStateException("For this operation need ByteBuffer.");
        }

        return readBytes(0, buffer.nativeBuffer);
    }

    default ByteBuffer readBytes (ByteBuffer tempNativeBuffer) {
        return readBytes(0, tempNativeBuffer);
    }

    default ByteBuffer readBytes (int offset) {
        T buffer = (T) this;
        if (!buffer.dynamic) {
            throw new IllegalStateException("For this operation need ByteBuffer.");
        }

        int len = (buffer.size - offset)
                        * buffer.dataObject.getSizeStruct();

        ByteBuffer tempNativeBuffer = buffer
                .nativeBuffer
                .slice(0, len);

        return readBytes(offset, tempNativeBuffer);
    }

    default ByteBuffer readBytes (int offset, ByteBuffer tempNativeBuffer) {
        T buffer = (T) this;
        Data data = buffer.dataObject;

        if (offset + tempNativeBuffer.capacity() / data.getSizeStruct() > buffer.capacity) {
            throw new ArrayStoreException("Attempt to read data outside the OpenCl buffer.");
        }

        if (offset + tempNativeBuffer.capacity() / data.getSizeStruct() > buffer.size) {
            System.err.println("The data read is outside the initialized volume.");
        }

        CL10.clEnqueueReadBuffer(
                buffer.openClContext.commandQueue,
                buffer.clBuffer,
                true,
                offset * data.getSizeStruct(),
                tempNativeBuffer.rewind(),
                null,
                null
        );

        return tempNativeBuffer;
    }

}
