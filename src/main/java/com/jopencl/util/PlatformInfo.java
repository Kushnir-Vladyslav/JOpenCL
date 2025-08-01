package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL21;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class PlatformInfo {
    final long platformID;

    String name;
    String vendor;
    String versionPlatform;
    String profile;
    String[] extension;
    OpenCLVersion openCLVersion;
    long timerResolution;


    PlatformInfo(long platformID) {
        if(platformID <= 0) {
            //log
            //throw
        }
        this.platformID = platformID;

        name = getPlatformInfo(platformID, CL10.CL_PLATFORM_NAME);
        if(name == null){
            //log
        }

        vendor = getPlatformInfo(platformID, CL10.CL_PLATFORM_VENDOR);
        if(vendor == null){
            //log
        }

        versionPlatform = getPlatformInfo(platformID, CL10.CL_PLATFORM_VERSION);
        if(versionPlatform == null){
            //log
            openCLVersion = OpenCLVersion.UNKNOWN;
        } else {
            openCLVersion = OpenCLVersion.getOpenCLVersion(versionPlatform);
        }

        profile = getPlatformInfo(platformID, CL10.CL_PLATFORM_PROFILE);
        if(profile == null){
            //log
        }

        String extensions = getPlatformInfo(platformID, CL10.CL_PLATFORM_EXTENSIONS);
        if(extensions == null) {
            //log
            extension = null;
        } else {
            extension = extensions.split(" ");
        }

        if(openCLVersion.isAtLeast(OpenCLVersion.OPENCL_2_1)) {
            try {
                timerResolution = getTimerResolution(platformID);
            } catch (Exception e) {
                //log
                timerResolution = -1;
            }
        } else {
            //log
            timerResolution = -1;
        }

        checkDevices();
    }

    private String getPlatformInfo(long platformID, int paramName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int result;

            PointerBuffer size = stack.mallocPointer(1);
            result = CL10.clGetPlatformInfo(platformID, paramName, (ByteBuffer) null, size);

            if (result != CL10.CL_SUCCESS) {
                return null;
            }

            ByteBuffer buffer = stack.malloc((int) size.get(0));
            result = CL10.clGetPlatformInfo(platformID, paramName, buffer, null);

            if (result != CL10.CL_SUCCESS) {
                return null;
            }

            return MemoryUtil.memUTF8(buffer);
        }
    }

    private long getTimerResolution(long platformID) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int result;

            PointerBuffer size = stack.mallocPointer(1);
            result = CL21.clGetPlatformInfo(platformID, CL21.CL_PLATFORM_HOST_TIMER_RESOLUTION, (ByteBuffer) null, size);

            if (result != CL10.CL_SUCCESS) {
                return -1;
            }

            ByteBuffer buffer = stack.malloc((int) size.get(0));
            result = CL21.clGetPlatformInfo(platformID, CL21.CL_PLATFORM_HOST_TIMER_RESOLUTION, buffer, null);

            if (result != CL10.CL_SUCCESS) {
                return -1;
            }

            return buffer.getLong();
        }
    }

    private void checkDevices() {

    }
}
