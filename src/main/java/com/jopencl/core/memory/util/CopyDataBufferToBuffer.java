package com.jopencl.core.memory.util;

import com.jopencl.core.memory.buffer.AbstractGlobalBuffer;
import com.jopencl.util.OpenClContext;
import org.lwjgl.opencl.CL10;

public class CopyDataBufferToBuffer {
    public static void copyData (OpenClContext openClContext, AbstractGlobalBuffer src, AbstractGlobalBuffer dst, long size) {
        copyData(openClContext, src, dst, 0, 0, size);
    }

    public static void copyData (OpenClContext openClContext, AbstractGlobalBuffer src, AbstractGlobalBuffer dst, long srcOffset, long dstOffset, long size) {
        if (src.getDataClass().equals(dst.getDataClass())) {
            System.err.println("Copying is not safe.");
            System.err.println("The data type of the " + src.getBufferName() + " buffer is different from the " + dst.getBufferName() + " buffer.");
        }

        CL10.clEnqueueCopyBuffer(
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

    public static void copyData (OpenClContext openClContext, long src, long dst, int size) {
        copyData(openClContext, src, dst, 0, 0, size);
    }

    public static void copyData (OpenClContext openClContext, long src, long dst, int srcOffset, long dstOffset, long size) {

        CL10.clEnqueueCopyBuffer(
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
