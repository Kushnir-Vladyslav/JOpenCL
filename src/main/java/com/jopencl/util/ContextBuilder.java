package com.jopencl.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL20;
import org.lwjgl.opencl.KHRPriorityHints;
import org.lwjgl.opencl.KHRThrottleHints;
import org.lwjgl.opencl.KHRInitializeMemory;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.jopencl.util.OpenCLErrorUtils.*;

//Options for interacting with other APIs (OpenGL, DirectX, etc.) will be added in future versions.
//Multi-device context is not supported, to simplify high-level usage. This functionality can be implemented separately in cross-buffer and kernel implementations.
public class ContextBuilder {
    //context
    private Platform platform = null;
    private Device device = null;

    private boolean isInitialize = false;

    //queue
    private boolean outOfOrder = false;
    private boolean profiling = false;

    private long priority = KHRPriorityHints.CL_QUEUE_PRIORITY_MED_KHR;
    private long energyConsumption = KHRThrottleHints.CL_QUEUE_THROTTLE_MED_KHR;

    private boolean deviceQueue = false;
    private long deviceQueueSize = 0L;

    public ContextBuilder withMemoryInitialization (boolean isInitialize) {
        this.isInitialize = isInitialize;
        return this;
    }

    public ContextBuilder withBestDevice (Platform platform) {
        this.platform = platform;
        this.device = platform.getBestDevice();
        return this;
    }

    public ContextBuilder withBestCPUDevice (Platform platform) {
        this.platform = platform;
        List<Device> devices = platform.getCPUDevices();
        if (devices == null || devices.isEmpty()) {
            // log
            // throw
        }
        this.device = devices.get(0);
        return this;
    }

    public ContextBuilder withBestGPUDevice (Platform platform) {
        this.platform = platform;
        List<Device> devices = platform.getGPUDevices();
        if (devices == null || devices.isEmpty()) {
            // log
            // throw
        }
        this.device = devices.get(0);
        return this;
    }

    public ContextBuilder withBestAcceleratorDevice (Platform platform) {
        this.platform = platform;
        List<Device> devices = platform.getAcceleratorDevices();
        if (devices == null || devices.isEmpty()) {
            // log
            // throw
        }
        this.device = devices.get(0);
        return this;
    }

    public ContextBuilder withDevice (Device device) {
        this.device = device;
        this.platform = device.getPlatform();

        return this;
    }

    public ContextBuilder withOutOfOrderQueue (boolean outOfOrder) {
        this.outOfOrder = outOfOrder;
        return this;
    }

    public ContextBuilder withProfiling (boolean profiling) {
        this.profiling = profiling;
        return this;
    }

    public ContextBuilder withPriority(Level level) {
        switch (level) {
            case LOW:
                this.priority = KHRPriorityHints.CL_QUEUE_PRIORITY_LOW_KHR;
                break;
            case MED:
                this.priority = KHRPriorityHints.CL_QUEUE_PRIORITY_MED_KHR;
                break;
            case HIGH:
                this.priority = KHRPriorityHints.CL_QUEUE_PRIORITY_HIGH_KHR;
                break;
        }
        return this;
    }

    public ContextBuilder withEnergyConsumption(Level level) {
        switch (level) {
            case LOW:
                this.energyConsumption = KHRThrottleHints.CL_QUEUE_THROTTLE_LOW_KHR;
                break;
            case MED:
                this.energyConsumption = KHRThrottleHints.CL_QUEUE_THROTTLE_MED_KHR;
                break;
            case HIGH:
                this.energyConsumption = KHRThrottleHints.CL_QUEUE_THROTTLE_HIGH_KHR;
                break;
        }
        return this;
    }

    public ContextBuilder withAdditionalDeviceQueue (boolean additionalDeviceQueue, long deviceQueueSize) {
        if(deviceQueueSize <= 0) {
            //throw
        }

        this.deviceQueue = additionalDeviceQueue;
        this.deviceQueueSize = deviceQueueSize;
        return this;
    }

    public OpenClContext create () {
        validate();

        OpenCL.start();

        long context = 0L;
        long commandQueue = 0L;
        long deviceCommandQueue = 0L;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer contextProperties = createContextProperties(stack);

            IntBuffer errCode = stack.mallocInt(1);

            context = CL10.clCreateContext(contextProperties, device.getDeviceID(), null, 0L, errCode);

            if (errCode.get(0) != CL10.CL_SUCCESS) {
                OpenCL.destroy();
                // log
                throw new RuntimeException("clCreateContext failed with error: " + getCLErrorString(errCode.get(0)));
            }

            if(device.getOpenCLVersion().isAtLeast(CLVersion.OPENCL_2_0)) {
                LongBuffer queueProperties = createQueueProperties(stack);

                commandQueue = CL20.clCreateCommandQueueWithProperties(context, device.getDeviceID(), queueProperties, errCode);

                if (errCode.get(0) != CL10.CL_SUCCESS) {
                    OpenCL.destroy();
                    // log
                    throw new RuntimeException("clCreateContext failed with error: " + getCLErrorString(errCode.get(0)));
                }
            } else {
                long queueProperties = 0L;
                if (outOfOrder) {
                    queueProperties |= CL10.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
                }
                if (profiling) {
                    queueProperties |= CL10.CL_QUEUE_PROFILING_ENABLE;
                }

                commandQueue = CL10.clCreateCommandQueue(context, device.getDeviceID(), queueProperties, errCode);
            }

            if (errCode.get(0) != CL10.CL_SUCCESS) {
                CL10.clReleaseContext(context);
                OpenCL.destroy();
                // log
                throw new RuntimeException("clCreateCommandQueue failed with error: " + getCLErrorString(errCode.get(0)));
            }

            if (deviceQueue && device.getOpenCLVersion().isAtLeast(CLVersion.OPENCL_2_0)) {
                LongBuffer queueProperties = createDeviceQueueProperties(stack);

                deviceCommandQueue = CL20.clCreateCommandQueueWithProperties(context, device.getDeviceID(), queueProperties, errCode);

                if (errCode.get(0) != CL10.CL_SUCCESS || deviceCommandQueue == 0) {
                    CL10.clReleaseCommandQueue(commandQueue);
                    CL10.clReleaseContext(context);
                    OpenCL.destroy();
                    // log
                    throw new RuntimeException("clCreateContext failed with error: " + getCLErrorString(errCode.get(0)));
                }
            }
        }

        if (context == 0 || commandQueue == 0) {
            OpenCL.destroy();
            throw new IllegalStateException("Failed to create OpenCL context or command queue.");
        }

        OpenClContext contextCL = new OpenClContext(
                platform,
                device,
                outOfOrder,
                context,
                commandQueue,
                deviceCommandQueue,
                deviceQueueSize
        );

        OpenCL.registrationContext(contextCL);

        return contextCL;
    }

    private void validate() {
        if (device == null || !device.isAvailable()) {
            throw new IllegalStateException("No OpenCL device specified for context creation.");
        }
    }

    private PointerBuffer createContextProperties (MemoryStack stack) {
        List<Long> properties = new ArrayList<>();

        properties.add((long) CL10.CL_CONTEXT_PLATFORM);
        properties.add(platform.getPlatformID());

        if(isInitialize) {
            properties.add((long) KHRInitializeMemory.CL_CONTEXT_MEMORY_INITIALIZE_KHR);
            properties.add((long) CL10.CL_TRUE);
        }

        properties.add(0L);

        PointerBuffer propertiesBuffer = stack.mallocPointer(properties.size());
        for (long property : properties) {
            propertiesBuffer.put(property);
        }

        return propertiesBuffer.flip();
    }

    private LongBuffer createQueueProperties (MemoryStack stack) {
        List<Long> properties = new ArrayList<>();

        long queueProperties = 0L;
        if (outOfOrder) {
            queueProperties |= CL10.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
        }
        if (profiling) {
            queueProperties |= CL10.CL_QUEUE_PROFILING_ENABLE;
        }

        if (queueProperties != 0L) {
            properties.add((long) CL20.CL_QUEUE_PROPERTIES);
            properties.add(queueProperties);
        }

        if (device.supportsExtension("cl_khr_priority_hints")) {
            properties.add((long) KHRPriorityHints.CL_QUEUE_PRIORITY_KHR);
            properties.add(priority);
        }

        if (device.supportsExtension("cl_khr_throttle_hints")) {
            properties.add((long) KHRThrottleHints.CL_QUEUE_THROTTLE_KHR);
            properties.add(energyConsumption);
        }

        properties.add(0L);

        LongBuffer propertiesBuffer = stack.mallocLong(properties.size());
        for (long property : properties) {
            propertiesBuffer.put(property);
        }

        return propertiesBuffer.flip();
    }

    private LongBuffer createDeviceQueueProperties (MemoryStack stack) {
        List<Long> properties = new ArrayList<>();

        long queueProperties = CL20.CL_QUEUE_ON_DEVICE;
        if (outOfOrder) {
            queueProperties |= CL10.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE;
        }
        if (profiling) {
            queueProperties |= CL10.CL_QUEUE_PROFILING_ENABLE;
        }

        properties.add((long) CL20.CL_QUEUE_PROPERTIES);
        properties.add(queueProperties);

        if (device.supportsExtension("cl_khr_priority_hints")) {
            properties.add((long) KHRPriorityHints.CL_QUEUE_PRIORITY_KHR);
            properties.add(priority);
        }

        if (device.supportsExtension("cl_khr_throttle_hints")) {
            properties.add((long) KHRThrottleHints.CL_QUEUE_THROTTLE_KHR);
            properties.add(energyConsumption);
        }

        properties.add((long) CL20.CL_QUEUE_SIZE);
        properties.add(deviceQueueSize);
        properties.add((long) CL20.CL_QUEUE_ON_DEVICE_DEFAULT);

        properties.add(0L);

        LongBuffer propertiesBuffer = stack.mallocLong(properties.size());
        for (long property : properties) {
            propertiesBuffer.put(property);
        }

        return propertiesBuffer.flip();
    }
}
