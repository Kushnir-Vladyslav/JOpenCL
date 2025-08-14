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
