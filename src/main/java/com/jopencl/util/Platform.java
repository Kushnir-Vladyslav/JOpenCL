package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.*;
import org.lwjgl.BufferUtils;

import java.nio.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL21.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;


public class Platform {
    private final long platformID;
    private final CLVersion versionCL;

    // Cached device lists
    private final List<Device> allDevices;
    private final List<Device> cpuDevices;
    private final List<Device> gpuDevices;
    private final List<Device> acceleratorDevices;

    // Cached platform info
    private final String name;
    private final String vendor;
    private final String platformVersion;
    private final String profile;
    private final String[] extensions;
    private final long hostTimerResolution;

    // Device counts (cached)
    private final int totalDeviceCount;
    private final int cpuDeviceCount;
    private final int gpuDeviceCount;
    private final int acceleratorDeviceCount;

    public Platform(long id) {
        long TimerResolution;
        this.platformID = id;

        // Cache all platform information immediately
        this.name = getString(CL_PLATFORM_NAME);
        this.vendor = getString(CL_PLATFORM_VENDOR);
        this.platformVersion = getString(CL_PLATFORM_VERSION);
        this.profile = getString(CL_PLATFORM_PROFILE);

        // Parse OpenCL version
        if(this.platformVersion == null){
            //log
            versionCL = CLVersion.UNKNOWN;
        } else {
            versionCL = CLVersion.getOpenCLVersion(this.platformVersion);
        }

        // Cache extensions
        String extString = getString(CL_PLATFORM_EXTENSIONS);
        if (extString == null || extString.trim().isEmpty()) {
            this.extensions = new String[0];
        } else {
            this.extensions = extString.trim().split("\\s+");
        }

        // Cache host timer resolution
        if(versionCL.isAtLeast(versionCL.OPENCL_2_1)) {
            try {
                TimerResolution = getLong(CL_PLATFORM_HOST_TIMER_RESOLUTION);
            } catch (Exception e) {
                TimerResolution = -1;
            }
        } else {
            TimerResolution = -1;
        }

        // Cache device counts
        this.hostTimerResolution = TimerResolution;
        this.totalDeviceCount = getDeviceCountDirect(CL_DEVICE_TYPE_ALL);
        this.cpuDeviceCount = getDeviceCountDirect(CL_DEVICE_TYPE_CPU);
        this.gpuDeviceCount = getDeviceCountDirect(CL_DEVICE_TYPE_GPU);
        this.acceleratorDeviceCount = getDeviceCountDirect(CL_DEVICE_TYPE_ACCELERATOR);

        // Cache device lists
        this.allDevices = getDeviceDirect(getDeviceIDs(CL_DEVICE_TYPE_ALL));
        this.cpuDevices = getDeviceDirect(getDeviceIDs(CL_DEVICE_TYPE_CPU));
        this.gpuDevices = getDeviceDirect(getDeviceIDs(CL_DEVICE_TYPE_GPU));
        this.acceleratorDevices = getDeviceDirect(getDeviceIDs(CL_DEVICE_TYPE_ACCELERATOR));
    }

    // ---------- PRIVATE HELPER METHODS ----------

    private String getString(int param) {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer sizeBuf = stack.mallocPointer(1);
            int result = clGetPlatformInfo(platformID, param, (ByteBuffer) null, sizeBuf);
            if (result != CL_SUCCESS) {
                //log
                return null;
            }
            int size = (int) sizeBuf.get(0);
            ByteBuffer buf = stack.malloc(size);
            result = clGetPlatformInfo(platformID, param, buf, null);
            if (result != CL_SUCCESS) {
                //log
                return null;
            }
            return memUTF8(buf, size - 1);
        }
    }

    private long getLong(int param) {
        try (MemoryStack stack = stackPush()) {
            LongBuffer buf = stack.mallocLong(1);
            int result = clGetPlatformInfo(platformID, param, buf, null);
            if (result != CL_SUCCESS) {
                //log
                return -1;
            }
            return buf.get(0);
        }
    }

    private List<Device> getDeviceDirect(List<Long> devicesID) {
        List<Device> deviceList = new ArrayList<>();
        for (Long deviceID : devicesID) {
            deviceList.add(new Device(this, deviceID));
        }
        return Collections.unmodifiableList(deviceList);
    }

    private List<Long> getDeviceIDs(int deviceType) {
        IntBuffer numDevices = BufferUtils.createIntBuffer(1);
        int result = clGetDeviceIDs(platformID, deviceType, null, numDevices);

        if (result != CL_SUCCESS || numDevices.get(0) == 0) {
            return new ArrayList<>();
        }

        PointerBuffer devices = BufferUtils.createPointerBuffer(numDevices.get(0));
        result = clGetDeviceIDs(platformID, deviceType, devices, (IntBuffer) null);

        if (result != CL_SUCCESS) {
            return new ArrayList<>();
        }

        List<Long> deviceList = new ArrayList<>();
        for (int i = 0; i < devices.capacity(); i++) {
            deviceList.add(devices.get(i));
        }
        return deviceList;
    }

    private int getDeviceCountDirect(int deviceType) {
        IntBuffer numDevices = BufferUtils.createIntBuffer(1);
        int result = clGetDeviceIDs(platformID, deviceType, null, numDevices);
        return (result == CL_SUCCESS) ? numDevices.get(0) : 0;
    }

    // ========== PUBLIC GETTERS (now return cached values) ==========

    public String getName() {
        return name;
    }

    public String getVendor() {
        return vendor;
    }

    public String getPlatformVersion() {
        return platformVersion;
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

    public long getHostTimerResolution() {
        return hostTimerResolution;
    }

    public long getPlatformID() {
        return platformID;
    }

    // ========== DEVICE METHODS (now return cached values) ==========

    public List<Device> getAllDevices() {
        return new ArrayList<>(allDevices);
    }

    public List<Device> getCPUDevices() {
        return new ArrayList<>(cpuDevices);
    }

    public List<Device> getGPUDevices() {
        return new ArrayList<>(gpuDevices);
    }

    public List<Device> getAcceleratorDevices() {
        return new ArrayList<>(acceleratorDevices);
    }

    // Device counts (cached)
    public int getTotalDeviceCount() {
        return totalDeviceCount;
    }

    public int getCPUDeviceCount() {
        return cpuDeviceCount;
    }

    public int getGPUDeviceCount() {
        return gpuDeviceCount;
    }

    public int getAcceleratorDeviceCount() {
        return acceleratorDeviceCount;
    }

    // ========== UTILITY METHODS ==========

    public boolean hasGPUDevices() {
        return gpuDeviceCount > 0;
    }

    public boolean hasCPUDevices() {
        return cpuDeviceCount > 0;
    }

    public boolean hasAcceleratorDevices() {
        return acceleratorDeviceCount > 0;
    }

    public boolean supportsOpenCL21() {
        return versionCL.isAtLeast(versionCL.OPENCL_2_1);
    }

    public boolean supportsExtension(String extensionName) {
        for (String ext : extensions) {
            if (ext.equals(extensionName)) {
                return true;
            }
        }
        return false;
    }

    public Device getBestGPUDevice() {
        return gpuDevices.isEmpty() ? null : gpuDevices.get(0);
    }

    public Device getBestDevice() {
        // Priority: GPU > CPU > Accelerator
        if (!gpuDevices.isEmpty()) return gpuDevices.get(0);
        if (!cpuDevices.isEmpty()) return cpuDevices.get(0);
        if (!acceleratorDevices.isEmpty()) return acceleratorDevices.get(0);
        return null;
    }

    public boolean owns(Device device) {
        return allDevices.contains(device);
    }

    // ========== INFORMATION DISPLAY ==========

    public String getDeviceSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Devices on platform:\n");

        if (cpuDeviceCount > 0) sb.append("  CPU: ").append(cpuDeviceCount).append(" device(s)\n");
        if (gpuDeviceCount > 0) sb.append("  GPU: ").append(gpuDeviceCount).append(" device(s)\n");
        if (acceleratorDeviceCount > 0) sb.append("  Accelerator: ").append(acceleratorDeviceCount).append(" device(s)\n");

        if (totalDeviceCount == 0) {
            sb.append("  No available devices\n");
        }

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== OpenCL Platform ===\n");
        sb.append("Name: ").append(getName()).append("\n");
        sb.append("Vendor: ").append(getVendor()).append("\n");
        sb.append("Version: ").append(getPlatformVersion()).append("\n");
        sb.append("Profile: ").append(getProfile()).append("\n");

        if (hostTimerResolution != -1) {
            sb.append("Host timer resolution: ").append(hostTimerResolution).append(" ns\n");
        }

        sb.append("\n").append(getDeviceSummary());
        sb.append("\n").append(getExtensionSummary());

        return sb.toString();
    }
}
