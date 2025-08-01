package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

public class OpenCLOld {
    OpenClContext context = new OpenClContext();

    public OpenCLOld() {

        org.lwjgl.system.Configuration.OPENCL_EXPLICIT_INIT.set(true);
        CL.create();

        try (MemoryStack stack = MemoryStack.stackPush()) {

            IntBuffer platformCount = stack.mallocInt(1);
            CL10.clGetPlatformIDs(null, platformCount);

            PointerBuffer platforms = stack.mallocPointer(platformCount.get(0));
            CL10.clGetPlatformIDs(platforms, (IntBuffer) null);

//            platformInfo (platformCount,  platforms);

            long platform = platforms.get(0);

            IntBuffer deviceCount = stack.mallocInt(1);
            CL10.clGetDeviceIDs(platform, CL10.CL_DEVICE_TYPE_GPU, null, deviceCount);

            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            CL10.clGetDeviceIDs(platform, CL10.CL_DEVICE_TYPE_GPU, devices, (IntBuffer) null);

            context.device = devices.get(0);

//            deviceDiagnostic ();

            PointerBuffer contextProperties = stack.mallocPointer(3)
                    .put(CL10.CL_CONTEXT_PLATFORM)
                    .put(platform)
                    .put(0)
                    .rewind();

            context.context = CL10.clCreateContext(contextProperties, context.device, null, 0, null);
            context.commandQueue = CL10.clCreateCommandQueue(context.context, context.device, CL10.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE, (IntBuffer) null);

            if (context.context == 0 || context.commandQueue == 0) {
                throw new IllegalStateException("Failed to create OpenCL context or command queue.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public OpenClContext getContext () {
        return context;
    }

    public void destroy () {
        context.destroy();

        CL.destroy();
    }

}
