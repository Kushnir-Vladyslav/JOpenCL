package com.jopencl.exceptions;

import com.jopencl.util.OpenCLErrorUtils;

/**
 * Base exception for all OpenCL-related errors in the JOpenCL wrapper.
 * This exception serves as the parent for more specific OpenCL exceptions
 * and provides basic error handling functionality.
 */
public class OpenCLException extends RuntimeException {
    protected final int errorCode;

    public OpenCLException(String message) {
        super(message);
        this.errorCode = 0;
    }

    public OpenCLException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 0;
    }

    public OpenCLException(String message, int errorCode) {
        super(String.format("%s (Error code: %s)", message,
                com.jopencl.util.OpenCLErrorUtils.getCLErrorString(errorCode)));
        this.errorCode = errorCode;
    }

    public OpenCLException(String message, int errorCode, Throwable cause) {
        super(formatMessage(message, errorCode), cause);
        this.errorCode = errorCode;
    }

    private static String formatMessage(String message, int errorCode) {
        return String.format("%s (Error code: %s)",
                message,
                OpenCLErrorUtils.getCLErrorString(errorCode));
    }

    /**
     * Returns the OpenCL error code associated with this exception.
     *
     * @return the OpenCL error code, or 0 if no specific code was provided
     */
    public int getErrorCode() {
        return errorCode;
    }
}
