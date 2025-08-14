package com.jopencl.exceptions;

/**
 * Thrown when initialization of an OpenCL device fails.
 */
public class DeviceInitializationException extends OpenCLException {

    public DeviceInitializationException(String message) {
        super(message);
    }

    public DeviceInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceInitializationException(String message, int errorCode) {
        super(message, errorCode);
    }

    public DeviceInitializationException(String message, int errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
