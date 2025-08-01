package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class DeviceInfo {
    private final long deviceID;

    public DeviceInfo(long id) {
        this.deviceID = id;
    }

    // ---------- INT ----------
    private int getInt(int param) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer buf = stack.mallocInt(1);
            clGetDeviceInfo(deviceID, param, buf, null);
            return buf.get(0);
        }
    }

    // ---------- LONG ----------
    private long getLong(int param) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer buf = stack.mallocLong(1);
            clGetDeviceInfo(deviceID, param, buf, null);
            return buf.get(0);
        }
    }

    // ---------- BOOL ----------
    private boolean getBool(int param) {
        return getInt(param) != 0;
    }

    // ---------- STRING ----------
    private String getString(int param) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer sizeBuf = stack.mallocPointer(1);
            clGetDeviceInfo(deviceID, param, (ByteBuffer) null, sizeBuf);
            int size = (int) sizeBuf.get(0);
            ByteBuffer buf = stack.malloc(size);
            clGetDeviceInfo(deviceID, param, buf, null);
            return memUTF8(buf, size - 1);
        }
    }

    // ---------- LONG ARRAY ----------
    private long[] getSizeArray(int param, int dim) {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer buf = stack.malloc(dim * Long.BYTES);
            clGetDeviceInfo(deviceID, param, buf, null);
            long[] result = new long[dim];
            for (int i = 0; i < dim; i++) {
                result[i] = buf.getLong(i * Long.BYTES);
            }
            return result;
        }
    }

    // ========== PUBLIC GETTERS ==========

    public String getName() {
        return getString(CL_DEVICE_NAME);
    }

    public String getVendor() {
        return getString(CL_DEVICE_VENDOR);
    }

    public String getDriverVersion() {
        return getString(CL_DRIVER_VERSION);
    }

    public String getOpenCLVersion() {
        return getString(CL_DEVICE_VERSION);
    }

    public int getMaxComputeUnits() {
        return getInt(CL_DEVICE_MAX_COMPUTE_UNITS);
    }

    public int getMaxClockFrequency() {
        return getInt(CL_DEVICE_MAX_CLOCK_FREQUENCY);
    }

    public long getMaxGlobalBufferSize() {
        return getLong(CL_DEVICE_GLOBAL_MEM_SIZE);
    }

    public long getMaxMemAllocSize() {
        return getLong(CL_DEVICE_MAX_MEM_ALLOC_SIZE);
    }

    public long getMaxLocalMemSize() {
        return getLong(CL_DEVICE_LOCAL_MEM_SIZE);
    }

    public long getMaxConstantBufferSize() {
        return getLong(CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
    }

    public boolean isImageSupport() {
        return getBool(CL_DEVICE_IMAGE_SUPPORT);
    }

    public int getVendorID() {
        return getInt(CL_DEVICE_VENDOR_ID);
    }

    public long[] getMaxWorkItemSizes() {
        int dim = getInt(CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
        return getSizeArray(CL_DEVICE_MAX_WORK_ITEM_SIZES, dim);
    }

    public long getMaxWorkGroupSize() {
        return getLong(CL_DEVICE_MAX_WORK_GROUP_SIZE);
    }

    public long getDeviceID() {
        return deviceID;
    }
}
