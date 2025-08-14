package com.jopencl.exceptions;

/**
 * Thrown when initialization of an OpenCL platform fails.
 */
public class PlatformInitializationException extends OpenCLException {

    public PlatformInitializationException(String message) {
        super(message);
    }

    public PlatformInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PlatformInitializationException(String message, int errorCode) {
        super(message, errorCode);
    }

    public PlatformInitializationException(String message, int errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
