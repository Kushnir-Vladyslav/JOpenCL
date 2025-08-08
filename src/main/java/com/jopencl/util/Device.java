package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Device {
    private final long deviceID;
    private final Platform platform;
    private final CLVersion versionCL;

    // Cached basic info
    private final String name;
    private final String vendor;
    private final String driverVersion;
    private final String deviceVersion;
    private final String profile;
    private final String[] extensions;
    private final String deviceType;

    // Cached capabilities
    private final boolean isAvailable;
    private final boolean isCompilerAvailable;
    private final boolean isImageSupport;
    private final int vendorID;

    // Cached compute info
    private final int maxComputeUnits;
    private final int maxClockFrequency;
    private final int maxWorkItemDimensions;
    private final long[] maxWorkItemSizes;
    private final long maxWorkGroupSize;

    // Cached memory info
    private final long maxGlobalBufferSize;
    private final long maxMemAllocSize;
    private final long maxLocalMemSize;
    private final long maxConstantBufferSize;

    public Device(Platform platform, long id) {
        this.deviceID = id;
        this.platform = platform;

        // Cache basic info
        this.name = getString(CL_DEVICE_NAME);
        this.vendor = getString(CL_DEVICE_VENDOR);
        this.driverVersion = getString(CL_DRIVER_VERSION);
        this.deviceVersion = getString(CL_DEVICE_VERSION);
        this.profile = getString(CL_DEVICE_PROFILE);

        // Parse OpenCL version
        if(this.deviceVersion == null){
            //log
            versionCL = CLVersion.UNKNOWN;
        } else {
            versionCL = CLVersion.getOpenCLVersion(this.deviceVersion);
        }

        // Cache extensions
        String extString = getString(CL_DEVICE_EXTENSIONS);
        if (extString == null || extString.trim().isEmpty()) {
            this.extensions = new String[0];
        } else {
            this.extensions = extString.trim().split("\\s+");
        }

        // Cache device type
        long type = getLong(CL_DEVICE_TYPE);
        List<String> types = new ArrayList<>();
        if ((type & CL_DEVICE_TYPE_CPU) != 0) types.add("CPU");
        if ((type & CL_DEVICE_TYPE_GPU) != 0) types.add("GPU");
        if ((type & CL_DEVICE_TYPE_ACCELERATOR) != 0) types.add("Accelerator");
        this.deviceType = String.join(", ", types);

        // Cache capabilities
        this.isAvailable = getBool(CL_DEVICE_AVAILABLE);
        this.isCompilerAvailable = getBool(CL_DEVICE_COMPILER_AVAILABLE);
        this.isImageSupport = getBool(CL_DEVICE_IMAGE_SUPPORT);
        this.vendorID = getInt(CL_DEVICE_VENDOR_ID);

        // Cache compute info
        this.maxComputeUnits = getInt(CL_DEVICE_MAX_COMPUTE_UNITS);
        this.maxClockFrequency = getInt(CL_DEVICE_MAX_CLOCK_FREQUENCY);
        this.maxWorkItemDimensions = getInt(CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS);
        this.maxWorkItemSizes = getSizeArray(CL_DEVICE_MAX_WORK_ITEM_SIZES, this.maxWorkItemDimensions);
        this.maxWorkGroupSize = getLong(CL_DEVICE_MAX_WORK_GROUP_SIZE);

        // Cache memory info
        this.maxGlobalBufferSize = getLong(CL_DEVICE_GLOBAL_MEM_SIZE);
        this.maxMemAllocSize = getLong(CL_DEVICE_MAX_MEM_ALLOC_SIZE);
        this.maxLocalMemSize = getLong(CL_DEVICE_LOCAL_MEM_SIZE);
        this.maxConstantBufferSize = getLong(CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE);
    }

    // ---------- PRIVATE HELPER METHODS ----------

    private int getInt(int param) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer buf = stack.mallocInt(1);
            int result = clGetDeviceInfo(deviceID, param, buf, null);
            if (result != CL_SUCCESS) {
                //log
                return -1;
            }
            return buf.get(0);
        }
    }

    private long getLong(int param) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer buf = stack.mallocLong(1);
            int result = clGetDeviceInfo(deviceID, param, buf, null);
            if (result != CL_SUCCESS) {
                //log
                return -1;
            }
            return buf.get(0);
        }
    }

    private boolean getBool(int param) {
        return getInt(param) == CL_TRUE;
    }

    private String getString(int param) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer sizeBuf = stack.mallocPointer(1);
            int result = clGetDeviceInfo(deviceID, param, (ByteBuffer) null, sizeBuf);
            if (result != CL_SUCCESS) {
                //log
                return null;
            }
            int size = (int) sizeBuf.get(0);
            ByteBuffer buf = stack.malloc(size);
            result = clGetDeviceInfo(deviceID, param, buf, null);
            if (result != CL_SUCCESS) {
                //log
                return null;
            }
            return memUTF8(buf, size - 1);
        }
    }

    private long[] getSizeArray(int param, int dim) {
        if (dim <= 0) return new long[0];

        try (MemoryStack stack = stackPush()) {
            PointerBuffer buf = stack.mallocPointer(dim);
            int result = clGetDeviceInfo(deviceID, param, buf, null);
            if (result != CL_SUCCESS) {
                //log
                return new long[0];
            }
            long[] results = new long[dim];
            for (int i = 0; i < dim; i++) {
                results[i] = buf.get(i);
            }
            return results;
        }
    }

    // ========== PUBLIC GETTERS (now return cached values) ==========

    public String getName() {
        return name;
    }

    public String getVendor() {
        return vendor;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public String getDeviceVersion() {
        return deviceVersion;
    }

    public CLVersion getOpenCLVersion() {
        return versionCL;
    }

    public String getProfile() {
        return profile;
    }

    public String[] getExtensions() {
        return extensions.clone(); // Return copy to prevent modification
    }

    public String getDeviceType() {
        return deviceType;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public boolean isCompilerAvailable() {
        return isCompilerAvailable;
    }

    public boolean isImageSupport() {
        return isImageSupport;
    }

    public int getVendorID() {
        return vendorID;
    }

    public int getMaxComputeUnits() {
        return maxComputeUnits;
    }

    public int getMaxClockFrequency() {
        return maxClockFrequency;
    }

    public int getMaxWorkItemDimensions() {
        return maxWorkItemDimensions;
    }

    public long[] getMaxWorkItemSizes() {
        return maxWorkItemSizes.clone(); // Return copy to prevent modification
    }

    public long getMaxWorkGroupSize() {
        return maxWorkGroupSize;
    }

    public long getMaxGlobalBufferSize() {
        return maxGlobalBufferSize;
    }

    public long getMaxMemAllocSize() {
        return maxMemAllocSize;
    }

    public long getMaxLocalMemSize() {
        return maxLocalMemSize;
    }

    public long getMaxConstantBufferSize() {
        return maxConstantBufferSize;
    }

    public long getDeviceID() {
        return deviceID;
    }

    public Platform getPlatform() {
        return platform;
    }

    // ========== UTILITY METHODS ==========

    public boolean supportsExtension(String extensionName) {
        for (String ext : extensions) {
            if (ext.equals(extensionName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCPU() {
        return deviceType.contains("CPU");
    }

    public boolean isGPU() {
        return deviceType.contains("GPU");
    }

    public boolean isAccelerator() {
        return deviceType.contains("Accelerator");
    }

    public boolean belongTo(Platform platform) {
        return this.platform.equals(platform);
    }

    // ========== INFORMATION DISPLAY ==========

    public String getMemorySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory Information:\n");
        sb.append("  Global memory: ").append(getFormattedGlobalMemSize()).append("\n");
        sb.append("  Local memory: ").append(formatBytes(maxLocalMemSize)).append("\n");
        sb.append("  Max allocation: ").append(formatBytes(maxMemAllocSize)).append("\n");
        sb.append("  Constant buffer: ").append(formatBytes(maxConstantBufferSize)).append("\n");
        return sb.toString();
    }

    public String getComputeSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Compute Information:\n");
        sb.append("  Compute units: ").append(maxComputeUnits).append("\n");
        sb.append("  Clock frequency: ").append(maxClockFrequency).append(" MHz\n");
        sb.append("  Max work group size: ").append(maxWorkGroupSize).append("\n");

        if (maxWorkItemSizes.length > 0) {
            sb.append("  Max work item sizes: [");
            for (int i = 0; i < maxWorkItemSizes.length; i++) {
                sb.append(maxWorkItemSizes[i]);
                if (i < maxWorkItemSizes.length - 1) sb.append(", ");
            }
            sb.append("]\n");
        }

        return sb.toString();
    }

    public String getCapabilitiesSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Capabilities:\n");
        sb.append("  Device type: ").append(deviceType).append("\n");
        sb.append("  Available: ").append(isAvailable ? "Yes" : "No").append("\n");
        sb.append("  Compiler available: ").append(isCompilerAvailable ? "Yes" : "No").append("\n");
        sb.append("  Image support: ").append(isImageSupport ? "Yes" : "No").append("\n");
        return sb.toString();
    }

    public String getExtensionSummary() {
        if (extensions.length == 0) {
            return "Extensions: none";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Extensions (").append(extensions.length).append("):\n");
        for (String ext : extensions) {
            sb.append("  - ").append(ext).append("\n");
        }
        return sb.toString();
    }

    public String getFormattedGlobalMemSize() {
        return formatBytes(maxGlobalBufferSize);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== OpenCL Device ===\n");
        sb.append("Name: ").append(getName()).append("\n");
        sb.append("Vendor: ").append(getVendor()).append("\n");
        sb.append("Driver version: ").append(getDriverVersion()).append("\n");
        sb.append("OpenCL version: ").append(getOpenCLVersion()).append("\n");

        sb.append("\n").append(getCapabilitiesSummary());
        sb.append("\n").append(getComputeSummary());
        sb.append("\n").append(getMemorySummary());
        sb.append("\n").append(getExtensionSummary());

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Device device = (Device) obj;
        return deviceID == device.deviceID;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(deviceID);
    }
}
