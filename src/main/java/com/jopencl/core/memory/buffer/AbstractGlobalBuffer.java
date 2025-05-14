package com.jopencl.core.memory.buffer;

import com.jopencl.core.memory.data.ConvertFromByteBuffer;
import com.jopencl.core.memory.data.ConvertToByteBuffer;
import com.jopencl.core.memory.data.Data;
import com.jopencl.util.OpenClContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryUtil;

public abstract class AbstractGlobalBuffer
        extends KernelAwareBuffer {

    private boolean readable = false;
    private boolean writable = false;
    protected boolean dynamic = false;
    protected boolean copyHostBuffer = false;

    protected long flags = 0;
    protected long clBuffer = 0;

    protected Object hostBuffer = null;
    protected int size = -1;

    private PointerBuffer transmitter;

    public AbstractGlobalBuffer() {
        setFlags(CL10.CL_MEM_READ_WRITE);
    }

    public AbstractBuffer setFlags(long newFlags) {
        initCheck();
        flags = newFlags;

        return this;
    }

    public AbstractBuffer setCopyHostBuffer(boolean isProjection) {
        initCheck();
        copyHostBuffer = isProjection;

        return this;
    }

    protected AbstractBuffer setReadable(boolean isRead) {
        initCheck();
        readable = isRead;

        return this;
    }

    protected AbstractBuffer setWritable(boolean isRead) {
        initCheck();
        writable = isRead;

        return this;
    }

    protected AbstractBuffer setDynamic(boolean isDynamic) {
        initCheck();
        dynamic = isDynamic;

        return this;
    }

    @Override
    public void addInit() {
        super.addInit();

        transmitter = MemoryUtil.memAllocPointer(1);

        size = 0;

        if (readable) {
            if (!(this instanceof Readable)) {
                initErr("Doesn't extends of Readable interface.");
            }

            if (!(dataObject instanceof ConvertFromByteBuffer)) {
                initErr("Data class doesn't extends of \"ConvertFromByteBuffer\" interface.");
            }
        }

        if (writable) {
            if (!(this instanceof Writable)) {
                initErr("Doesn't extends of Writable interface.");
            }

            if (!(dataObject instanceof ConvertToByteBuffer)) {
                initErr("Data class doesn't extends of \"ConvertToByteBuffer\" interface.");
            }
        }

        if (dynamic) {
            capacity *= 1.5;
        }

        if (copyHostBuffer) {
            if (dataObject instanceof ConvertFromByteBuffer converter) {
                hostBuffer = converter.createArr(capacity);
            } else {
                initErr("Data class doesn't extends of \"ConvertFromByteBuffer\" interface.");
            }
        }

        if (clBuffer == 0) {
            clBuffer = createClBuffer();
        }
    }

    protected long createClBuffer() {
        if (capacity < 1) {
            throw new IllegalStateException("Length of OpenCl buffer must be positive.");
        }

        long newClBuffer = CL10.clCreateBuffer(
                openClContext.context,
                flags,
                capacity,
                null
        );

        if (newClBuffer == 0) {
            throw new IllegalStateException("Failed to create OpenCL memory buffers.");
        }

        return newClBuffer;
    }

    public <T extends Data> void setup (Class<T> clazz,
                       OpenClContext context,
                       boolean copyNativeBuffer,
                       boolean copyHostBuffer,
                       int initSize) {
        setDataClass(clazz);
        setOpenClContext(context);
        setCopyNativeBuffer(copyNativeBuffer);
        setCopyHostBuffer(copyHostBuffer);
        setInitSize(initSize);
        init();
    }

    public long getClBuffer() {
        return clBuffer;
    }

    public <T extends Data> void setup (String bufferName,
                       Class<T> clazz,
                       OpenClContext context,
                       boolean copyNativeBuffer,
                       boolean copyHostBuffer,
                       int initSize) {
        setBufferName(bufferName);
        setDataClass(clazz);
        setOpenClContext(context);
        setCopyNativeBuffer(copyNativeBuffer);
        setCopyHostBuffer(copyHostBuffer);
        setInitSize(initSize);
        init();
    }

    @Override
    protected void setKernelArg (long targetKernel, int argIndex) {
        CL10.clSetKernelArg(
                targetKernel,
                argIndex,
                transmitter.put(0, clBuffer).rewind()
        );
    }

    @Override
    public void destroy () {
        if (hostBuffer != null) {
            hostBuffer = null;
        }

        size = -1;

        flags = 0;

        if (clBuffer != 0) {
            CL10.clReleaseMemObject(clBuffer);
            clBuffer = 0;
        }

        if (transmitter != null) {
            MemoryUtil.memFree(transmitter);
            transmitter = null;
        }

        super.destroy();
    }
}
