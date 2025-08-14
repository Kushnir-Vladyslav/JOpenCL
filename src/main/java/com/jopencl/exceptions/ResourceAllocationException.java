package com.jopencl.exceptions;

/**
 * Thrown when OpenCL resource allocation fails. This includes failures
 * in memory allocation, buffer creation, queue creation, and other
 * OpenCL resource management operations.
 */
public class ResourceAllocationException extends OpenCLException {
    public ResourceAllocationException(String message) {
        super(message);
    }

    public ResourceAllocationException(String message, int errorCode) {
        super(message, errorCode);
    }

    public ResourceAllocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceAllocationException(String message, int errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
