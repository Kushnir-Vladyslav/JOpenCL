package com.jopencl.exceptions;

/**
 * Thrown when no suitable OpenCL device can be found or when
 * a specified device becomes unavailable. This typically occurs
 * during context creation or device selection.
 */
public class DeviceNotFoundException extends OpenCLException {
    public DeviceNotFoundException(String message) {
        super(message);
    }

    public DeviceNotFoundException(String message, int errorCode) {
        super(message, errorCode);
    }

    public DeviceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceNotFoundException(String message, int errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
