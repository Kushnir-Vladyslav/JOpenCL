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
