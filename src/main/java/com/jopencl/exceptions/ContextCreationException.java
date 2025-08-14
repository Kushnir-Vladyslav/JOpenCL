package com.jopencl.exceptions;

/**
 * Thrown when an OpenCL context creation fails. This can happen due to
 * resource limitations, invalid device selection, or platform-specific issues.
 * Includes the original OpenCL error code for detailed error handling.
 */
public class ContextCreationException extends OpenCLException {
    public ContextCreationException(String message) {
        super(message);
    }

    public ContextCreationException(String message, int errorCode) {
        super(message, errorCode);
    }

    public ContextCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextCreationException(String message, int errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
