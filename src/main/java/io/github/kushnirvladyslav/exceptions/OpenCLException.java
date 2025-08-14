/*
 * Copyright 2025 Kushnir Vladyslav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kushnirvladyslav.exceptions;

import io.github.kushnirvladyslav.util.OpenCLErrorUtils;

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
                OpenCLErrorUtils.getCLErrorString(errorCode)));
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
