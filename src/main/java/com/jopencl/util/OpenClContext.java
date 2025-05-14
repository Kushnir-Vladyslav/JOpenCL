package com.jopencl.util;

import com.jopencl.core.memory.buffer.BufferManager;
import org.lwjgl.opencl.CL10;

public class OpenClContext {
    public long device;
    public long context;
    public long commandQueue;

    public BufferManager bufferManager = new BufferManager();



    public void destroy () {
        bufferManager.releaseAll();



        if(commandQueue != 0) {
            CL10.clReleaseCommandQueue(commandQueue);
            commandQueue = 0;
        }

        if(context != 0) {
            CL10.clReleaseContext(context);
            context = 0;
        }
    }
}
