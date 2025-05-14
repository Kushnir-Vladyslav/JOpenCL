package com.jopencl.core.memory.buffer.typedBuffers;

import com.jopencl.core.memory.buffer.KernelAwareBuffer;
import com.jopencl.core.memory.data.Data;
import com.jopencl.util.OpenClContext;
import org.lwjgl.opencl.CL10;

public class LocalBuffer
        extends KernelAwareBuffer {

    @Override
    public void addInit() {
        super.addInit();

        if (copyNativeBuffer) {
            System.err.println("LocalBuffer cannot transfer data to the host, so there is no point in creating projections.");
        }
    }

    public void setup (Class<Data> clazz, OpenClContext context, int initSize) {
        setDataClass(clazz);
        setInitSize(initSize);
        setOpenClContext(context);
        init();
    }

    public void setup (String bufferName, Class<Data> clazz, OpenClContext context, int initSize) {
        setBufferName(bufferName);
        setDataClass(clazz);
        setInitSize(initSize);
        setOpenClContext(context);
        init();
    }

    @Override
    protected void setKernelArg(long targetKernel, int argIndex) {
        CL10.clSetKernelArg(targetKernel, argIndex, capacity);
    }
}
