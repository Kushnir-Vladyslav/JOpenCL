package com.jopencl.exceptions;

/**
 * Thrown when the OpenCL runtime initialization fails.
 * This can occur during platform discovery, library loading,
 * or initial OpenCL context creation.
 */
public class OpenCLInitializationException extends OpenCLException {
    public OpenCLInitializationException(String message) {
        super(message);
    }

    public OpenCLInitializationException(String message, int errorCode) {
        super(message, errorCode);
    }

    public OpenCLInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenCLInitializationException(String message, int errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
