package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenCL {
    private static AtomicInteger createdOpenCL = new AtomicInteger(0);
    private static final List<Platform> platforms = collectInformation();
    private static final Object lock = new Object();

    private StatusCL status = StatusCL.CLOSED;



    protected OpenCL() {
    }

    private void start() {
        if (!isRunning()) {
            setStatus(StatusCL.RUNNING);
            if (createdOpenCL.incrementAndGet() == 1) {
                org.lwjgl.system.Configuration.OPENCL_EXPLICIT_INIT.set(true);
                CL.create();
            }
        }
    }

    public void destroy () {
        if (isRunning()) {
            setStatus(StatusCL.CLOSED);
            if (createdOpenCL.decrementAndGet() == 0) {
                CL.destroy();
            }
        }
    }

    private static List<Platform> collectInformation() {
        org.lwjgl.system.Configuration.OPENCL_EXPLICIT_INIT.set(true);
        CL.create();
        List<Platform> platformsList;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numberPlatform = stack.mallocInt(1);
            int result = CL10.clGetPlatformIDs(null, numberPlatform);

            if(result != CL10.CL_SUCCESS) {
                //log
                //throw
            }

            PointerBuffer platformsBuffer = stack.mallocPointer(numberPlatform.get(0));
            result = CL10.clGetPlatformIDs(platformsBuffer, (IntBuffer) null);

            if(result != CL10.CL_SUCCESS) {
                //log
                //throw
            }

            platformsList = new ArrayList<>();
            for (int i = 0; i < numberPlatform.get(0); i++) {
                platformsList.add(new Platform(platformsBuffer.get(i)));
            }
        }

        CL.destroy();

        return platformsList;
    }

    public static List<Platform> getPlatforms() {
        return new ArrayList<>(platforms);
    }

    private boolean isRunning () {
        return status == StatusCL.RUNNING;
    }

    private void setStatus(StatusCL statusCL) {
        status = statusCL;
    }
}
