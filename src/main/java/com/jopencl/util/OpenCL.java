package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class OpenCL {
    private static final List<Platform> platforms;
    private static final Object lock = new Object();

    private static OpenClContext defaultContext = null;
    private static final AtomicInteger numDefaultContext = new AtomicInteger(0);
    private static final List<OpenClContext> contextList = new CopyOnWriteArrayList<>();

    private static volatile StatusCL status = StatusCL.CLOSED;

    static {
        org.lwjgl.system.Configuration.OPENCL_EXPLICIT_INIT.set(true);

        start();

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
        } finally {
            destroy();
        }

        platforms = platformsList;
    }

    static public OpenClContext getDefaultContext() {
        synchronized (lock) {
            if (numDefaultContext.get() == 0) {
                defaultContext = createDefaultContext();
            }
        }
        numDefaultContext.incrementAndGet();
        return defaultContext;
    }

    static public OpenClContext createDefaultContext() {
        if (platforms.isEmpty()) {
            throw new IllegalStateException("No OpenCL platforms available");
        }

        for (Platform platform : platforms) {
            Device device = platform.getBestDevice();
            if (device != null && device.isAvailable()) {
                ContextBuilder contextBuilder = new ContextBuilder();
                contextBuilder.withDevice(device);
                return contextBuilder.create();
            }
        }

        throw new IllegalStateException("No OpenCL devices available");
    }

    static public ContextBuilder createContext() {
        return new ContextBuilder();
    }

    static void registrationContext(OpenClContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        contextList.add(context);
        //log
    }

    static void destroyContext(OpenClContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        if (!contextList.contains(context)) {
            //log
            //throw
        }

        if(defaultContext != null && context == defaultContext) {
            if (numDefaultContext.decrementAndGet() == 0) {
                if (contextList.remove(context)) {
                    defaultContext.destroy();
                    defaultContext = null;
                    //log
                }
                destroy();
            }
        } else {
            if(contextList.remove(context)) {
                context.destroy();
                //log
            }
            destroy();
        }
    }

    static void start() {
        synchronized (lock) {
            if (!isRunning()) {
                CL.create();
                setStatus(StatusCL.RUNNING);
            }
        }
    }

    static void destroy() {
        synchronized (lock) {
            if (contextList.isEmpty() && isRunning()) {
                setStatus(StatusCL.CLOSED);
                CL.destroy();
            }
        }
    }

    public void shutdown() {
        synchronized (lock) {
            for (OpenClContext context : contextList) {
                context.destroy();
            }

            contextList.clear();

            defaultContext = null;
            numDefaultContext.set(0);
            destroy();
        }
    }

    public static List<Platform> getPlatforms() {
        return new ArrayList<>(platforms);
    }

    private static boolean isRunning () {
        return status == StatusCL.RUNNING;
    }

    private static void setStatus(StatusCL statusCL) {
        status = statusCL;
    }
}
