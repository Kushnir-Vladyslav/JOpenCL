package com.jopencl.util;

import com.jopencl.exceptions.ContextCreationException;
import com.jopencl.exceptions.DeviceNotFoundException;
import com.jopencl.exceptions.OpenCLException;
import com.jopencl.exceptions.ResourceAllocationException;
import org.lwjgl.opencl.*;

/**
 * Utility class for OpenCL error handling and error code translation.
 * Provides human-readable descriptions for OpenCL error codes from different OpenCL versions
 * and extensions.
 */
public class OpenCLErrorUtils {
    /**
     * Converts an OpenCL error code to its corresponding string representation.
     * Supports error codes from OpenCL 1.0 through 2.2 and some KHR extensions.
     *
     * @param errorCode the OpenCL error code to convert
     * @return a human-readable string describing the error
     */
    public static String getCLErrorString(int errorCode) {
        switch (errorCode) {
            // CL10
            case CL10.CL_SUCCESS: return "CL_SUCCESS";
            case CL10.CL_DEVICE_NOT_FOUND: return "CL_DEVICE_NOT_FOUND";
            case CL10.CL_DEVICE_NOT_AVAILABLE: return "CL_DEVICE_NOT_AVAILABLE";
            case CL10.CL_COMPILER_NOT_AVAILABLE: return "CL_COMPILER_NOT_AVAILABLE";
            case CL10.CL_MEM_OBJECT_ALLOCATION_FAILURE: return "CL_MEM_OBJECT_ALLOCATION_FAILURE";
            case CL10.CL_OUT_OF_RESOURCES: return "CL_OUT_OF_RESOURCES";
            case CL10.CL_OUT_OF_HOST_MEMORY: return "CL_OUT_OF_HOST_MEMORY";
            case CL10.CL_PROFILING_INFO_NOT_AVAILABLE: return "CL_PROFILING_INFO_NOT_AVAILABLE";
            case CL10.CL_MEM_COPY_OVERLAP: return "CL_MEM_COPY_OVERLAP";
            case CL10.CL_IMAGE_FORMAT_MISMATCH: return "CL_IMAGE_FORMAT_MISMATCH";
            case CL10.CL_IMAGE_FORMAT_NOT_SUPPORTED: return "CL_IMAGE_FORMAT_NOT_SUPPORTED";
            case CL10.CL_BUILD_PROGRAM_FAILURE: return "CL_BUILD_PROGRAM_FAILURE";
            case CL10.CL_MAP_FAILURE: return "CL_MAP_FAILURE";
            case CL10.CL_INVALID_VALUE: return "CL_INVALID_VALUE";
            case CL10.CL_INVALID_DEVICE_TYPE: return "CL_INVALID_DEVICE_TYPE";
            case CL10.CL_INVALID_PLATFORM: return "CL_INVALID_PLATFORM";
            case CL10.CL_INVALID_DEVICE: return "CL_INVALID_DEVICE";
            case CL10.CL_INVALID_CONTEXT: return "CL_INVALID_CONTEXT";
            case CL10.CL_INVALID_QUEUE_PROPERTIES: return "CL_INVALID_QUEUE_PROPERTIES";
            case CL10.CL_INVALID_COMMAND_QUEUE: return "CL_INVALID_COMMAND_QUEUE";
            case CL10.CL_INVALID_HOST_PTR: return "CL_INVALID_HOST_PTR";
            case CL10.CL_INVALID_MEM_OBJECT: return "CL_INVALID_MEM_OBJECT";
            case CL10.CL_INVALID_IMAGE_FORMAT_DESCRIPTOR: return "CL_INVALID_IMAGE_FORMAT_DESCRIPTOR";
            case CL10.CL_INVALID_IMAGE_SIZE: return "CL_INVALID_IMAGE_SIZE";
            case CL10.CL_INVALID_SAMPLER: return "CL_INVALID_SAMPLER";
            case CL10.CL_INVALID_BINARY: return "CL_INVALID_BINARY";
            case CL10.CL_INVALID_BUILD_OPTIONS: return "CL_INVALID_BUILD_OPTIONS";
            case CL10.CL_INVALID_PROGRAM: return "CL_INVALID_PROGRAM";
            case CL10.CL_INVALID_PROGRAM_EXECUTABLE: return "CL_INVALID_PROGRAM_EXECUTABLE";
            case CL10.CL_INVALID_KERNEL_NAME: return "CL_INVALID_KERNEL_NAME";
            case CL10.CL_INVALID_KERNEL_DEFINITION: return "CL_INVALID_KERNEL_DEFINITION";
            case CL10.CL_INVALID_KERNEL: return "CL_INVALID_KERNEL";
            case CL10.CL_INVALID_ARG_INDEX: return "CL_INVALID_ARG_INDEX";
            case CL10.CL_INVALID_ARG_VALUE: return "CL_INVALID_ARG_VALUE";
            case CL10.CL_INVALID_ARG_SIZE: return "CL_INVALID_ARG_SIZE";
            case CL10.CL_INVALID_KERNEL_ARGS: return "CL_INVALID_KERNEL_ARGS";
            case CL10.CL_INVALID_WORK_DIMENSION: return "CL_INVALID_WORK_DIMENSION";
            case CL10.CL_INVALID_WORK_GROUP_SIZE: return "CL_INVALID_WORK_GROUP_SIZE";
            case CL10.CL_INVALID_WORK_ITEM_SIZE: return "CL_INVALID_WORK_ITEM_SIZE";
            case CL10.CL_INVALID_GLOBAL_OFFSET: return "CL_INVALID_GLOBAL_OFFSET";
            case CL10.CL_INVALID_EVENT_WAIT_LIST: return "CL_INVALID_EVENT_WAIT_LIST";
            case CL10.CL_INVALID_EVENT: return "CL_INVALID_EVENT";
            case CL10.CL_INVALID_OPERATION: return "CL_INVALID_OPERATION";
            case CL10GL.CL_INVALID_GL_OBJECT: return "CL_INVALID_GL_OBJECT";
            case CL10.CL_INVALID_BUFFER_SIZE: return "CL_INVALID_BUFFER_SIZE";
            case CL10GL.CL_INVALID_MIP_LEVEL: return "CL_INVALID_MIP_LEVEL";
            case CL10.CL_INVALID_GLOBAL_WORK_SIZE: return "CL_INVALID_GLOBAL_WORK_SIZE";

            // CL11
            case CL11.CL_INVALID_PROPERTY: return "CL_INVALID_PROPERTY";
            case CL11.CL_MISALIGNED_SUB_BUFFER_OFFSET: return "CL_MISALIGNED_SUB_BUFFER_OFFSET";
            case CL11.CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST: return "CL_EXEC_STATUS_ERROR_FOR_EVENTS_IN_WAIT_LIST";


            // CL12
            case CL12.CL_INVALID_IMAGE_DESCRIPTOR: return "CL_INVALID_IMAGE_DESCRIPTOR";
            case CL12.CL_INVALID_COMPILER_OPTIONS: return "CL_INVALID_COMPILER_OPTIONS";
            case CL12.CL_INVALID_LINKER_OPTIONS: return "CL_INVALID_LINKER_OPTIONS";
            case CL12.CL_INVALID_DEVICE_PARTITION_COUNT: return "CL_INVALID_DEVICE_PARTITION_COUNT";
            case CL12.CL_LINK_PROGRAM_FAILURE: return "CL_LINK_PROGRAM_FAILURE";

            // CL20
            case CL20.CL_COMPILE_PROGRAM_FAILURE: return "CL_COMPILE_PROGRAM_FAILURE";
            case CL20.CL_INVALID_PIPE_SIZE: return "CL_INVALID_PIPE_SIZE";
            case CL20.CL_INVALID_DEVICE_QUEUE: return "CL_INVALID_DEVICE_QUEUE";

            // CL22
            case CL22.CL_INVALID_SPEC_ID: return "CL_INVALID_SPEC_ID";
            case CL22.CL_MAX_SIZE_RESTRICTION_EXCEEDED: return "CL_MAX_SIZE_RESTRICTION_EXCEEDED";

            // KHR
            case KHRGLSharing.CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR: return "CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR";

            default: return "Unknown OpenCL error code: " + errorCode;
        }
    }


    /**
     * Checks if the given OpenCL error code indicates success.
     *
     * @param errorCode the OpenCL error code to check
     * @return true if the operation was successful, false otherwise
     */
    public static boolean isSuccess(int errorCode) {
        return errorCode == CL10.CL_SUCCESS;
    }

    /**
     * Throws appropriate exception based on the OpenCL error code if the operation failed.
     *
     * @param errorCode the OpenCL error code to check
     * @param operation description of the operation that was attempted
     * @throws OpenCLException if the operation failed
     */
    public static void checkError(int errorCode, String operation) {
        if (!isSuccess(errorCode)) {
            String errorString = getCLErrorString(errorCode);
            String message = String.format("%s failed: %s", operation, errorString);

            switch (errorCode) {
                case CL10.CL_DEVICE_NOT_FOUND:
                case CL10.CL_DEVICE_NOT_AVAILABLE:
                    throw new DeviceNotFoundException(message);

                case CL10.CL_INVALID_CONTEXT:
                case CL10.CL_INVALID_VALUE:
                case CL10.CL_INVALID_DEVICE:
                    throw new ContextCreationException(message, errorCode);

                case CL10.CL_MEM_OBJECT_ALLOCATION_FAILURE:
                case CL10.CL_OUT_OF_RESOURCES:
                case CL10.CL_OUT_OF_HOST_MEMORY:
                    throw new ResourceAllocationException(message, errorCode);

                default:
                    throw new OpenCLException(message);
            }
        }
    }
}

