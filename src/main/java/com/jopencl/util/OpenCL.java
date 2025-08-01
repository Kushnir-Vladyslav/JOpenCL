package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenCL {
    private static AtomicInteger createdOpenCL = new AtomicInteger(0);
    private static volatile OpenCLInfo info;
    private static final Object lock = new Object();

    private StatusCL status = StatusCL.CLOSED;

    protected OpenCL() {
        synchronized (lock) {
            if (info == null) {
                createOpenClInfo();
            }
        }

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

    private void createOpenClInfo () {
        start();

        IntBuffer numberPlatform = MemoryUtil.memAllocInt(1);
        CL10.clGetPlatformIDs(null, numberPlatform);

        PointerBuffer platforms = MemoryUtil.memAllocPointer(numberPlatform.get(0));
        CL10.clGetPlatformIDs(platforms, (IntBuffer) null);

        for (int i = 0; i < numberPlatform.get(0); i++) {
            long platformID = platforms.get(i);

            String name = CL10.getPlatformString(platformID, CL10.CL_PLATFORM_NAME);
            String vendor = CL10.getPlatformString(platformID, CL10.CL_PLATFORM_VENDOR);
            String version = CL10.getPlatformString(platformID, CL10.CL_PLATFORM_VERSION);
        }
    }

    private boolean isRunning () {
        return status == StatusCL.RUNNING;
    }

    private void setStatus(StatusCL statusCL) {
        status = statusCL;
    }
}
