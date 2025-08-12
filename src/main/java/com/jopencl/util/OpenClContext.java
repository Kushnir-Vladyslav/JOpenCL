package com.jopencl.util;

import com.jopencl.core.memory.buffer.BufferManager;
import org.lwjgl.opencl.CL10;

public class OpenClContext {
    private final Platform platform;
    private final Device device;
    private final boolean isOutOfOrder;
    private long context = 0;
    private long commandQueue = 0;
    private long deviceCommandQueue = 0;
    private long sizeDeviceCommandQueue = 0;

    private BufferManager bufferManager = new BufferManager();

    private volatile StatusCL status = StatusCL.READY;

    OpenClContext (Platform platform, Device device, boolean isOutOfOrder, long context, long commandQueue, long deviceCommandQueue, long sizeDeviceCommandQueue) {
        this.platform = platform;
        this.device = device;
        this.isOutOfOrder = isOutOfOrder;
        this.context = context;
        this.commandQueue = commandQueue;
        this.deviceCommandQueue = deviceCommandQueue;
        if (sizeDeviceCommandQueue <= 0) {
            destroy();
            //log
            //throw
        }
        this.sizeDeviceCommandQueue = sizeDeviceCommandQueue;

        status = StatusCL.RUNNING;
    }

    public Platform getPlatform() {
        checkNotClosed();
        return platform;
    }

    public Device getDevice() {
        checkNotClosed();
        return device;
    }

    public boolean isOutOfOrder() {
        checkNotClosed();
        return isOutOfOrder;
    }

    public long getContext() {
        checkNotClosed();
        return context;
    }

    public long getCommandQueue() {
        checkNotClosed();
        return commandQueue;
    }

    public boolean hasDeviceCommandQueue() {
        checkNotClosed();
        return deviceCommandQueue != 0;
    }

    public long getSizeDeviceCommandQueue() {
        checkNotClosed();
        return sizeDeviceCommandQueue;
    }

    public BufferManager getBufferManager() {
        checkNotClosed();
        return bufferManager;
    }

    public boolean isRunning() {
        checkNotClosed();
        return status == StatusCL.RUNNING;
    }

    public boolean isClosed() {
        checkNotClosed();
        return status == StatusCL.CLOSED;
    }

    private void checkNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("OpenCL context has been closed");
        }
    }

    void destroy () {
        synchronized (this) {
            if (status != StatusCL.RUNNING) {
                // log Already closed or in process of closing
                return;
            }
            status = StatusCL.CLOSED;
        }

        int result;

        if (bufferManager != null) {
            bufferManager.releaseAll();
            bufferManager = null;
        }

        if (deviceCommandQueue != 0) {
            result = CL10.clReleaseCommandQueue(deviceCommandQueue);
            if (result != CL10.CL_SUCCESS) {
                //log
            }
            commandQueue = 0;
        }

        if (commandQueue != 0) {
            result = CL10.clReleaseCommandQueue(commandQueue);
            if (result != CL10.CL_SUCCESS) {
                //log
            }
            commandQueue = 0;
        }

        if (context != 0) {
            result = CL10.clReleaseContext(context);
            if (result != CL10.CL_SUCCESS) {
                //log
            }
            context = 0;
        }
    }

    @Override
    public String toString() {
        return String.format("OpenClContext{platform=%s, device=%s, status=%s, outOfOrder=%s, hasDeviceQueue=%s}",
                platform != null ? platform.getName() : "null",
                device != null ? device.getName() : "null",
                status.name(),
                isOutOfOrder,
                hasDeviceCommandQueue());
    }
}
