package com.jopencl.core.memory.buffer;

import com.jopencl.core.memory.data.Data;
import com.jopencl.util.OpenClContext;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class AbstractBuffer {
    private static final AtomicInteger counter = new AtomicInteger(0);

    private boolean initiated = false;

    private String bufferName = "UnnamedBuffer" + counter.getAndIncrement();
    protected boolean copyNativeBuffer = false;
    private Class<?> clazz = null;
    protected OpenClContext openClContext;

    protected Data dataObject;

    protected ByteBuffer nativeBuffer = null;

    protected int capacity = -1;

    protected void initCheck () {
        if (initiated) {
            System.err.println("Buffer " + bufferName + "has been already initiated.");
        }
    }

    public AbstractBuffer setBufferName(String name) {
        initCheck();
        bufferName = name;

        return this;
    }

    public AbstractBuffer setCopyNativeBuffer(boolean isProjection) {
        initCheck();
        copyNativeBuffer = isProjection;

        return this;
    }

    public AbstractBuffer setOpenClContext(OpenClContext clContext) {
        initCheck();
        openClContext = clContext;

        return this;
    }

    public AbstractBuffer setInitSize(int newSize) {
        initCheck();
        if (newSize <= 0) {
            throw new IllegalStateException("Buffer's size mast be positive.");
        }
        capacity = newSize;

        return this;
    }

    public <T extends Data> AbstractBuffer setDataClass(Class<T> newClass) {
        initCheck();
        clazz = newClass;

        return this;
    }

    protected void initErr(String message) {
        throw new IllegalStateException(
                "Initiated error.\n" +
                "Buffer's name: \"" + bufferName + "\"\n" +
                message);
    }

    public final void init () {
        initCheck();

        if (bufferName == null) {
            initErr("Name of buffer cannot be \"null\"");
        }

        if (clazz == null) {
            initErr("Data class is invalid.");
        }
        try {
            dataObject = (Data) clazz.getConstructor().newInstance();
        } catch (Exception e) {
            initErr("Data class could not be initialized.");
        }

        if (capacity < 1) {
            initErr("Initiate buffer's size, mast be positive.");
        }

        if (openClContext == null) {
            initErr("OpenCL context for buffer cannot be \"null\"");
        }

        openClContext.bufferManager.registerBuffer(this);

        if (copyNativeBuffer) {
            nativeBuffer = MemoryUtil.memAlloc(capacity);
        }


        if (this instanceof AdditionalInitiation additionalInitiation) {
            additionalInitiation.addInit();
        }

        initiated = true;
    }

    protected abstract void setKernelArg (long targetKernel, int argIndex);


    public String getBufferName () {
        return bufferName;
    }

    public Class<?> getDataClass () {
        return clazz;
    }

    public void destroy () {
        if (initiated) {
            if (nativeBuffer != null) {
                MemoryUtil.memFree(nativeBuffer);
                nativeBuffer = null;
            }

            openClContext.bufferManager.remove(this);

            capacity = -1;

            initiated = false;
        }
    }
}

