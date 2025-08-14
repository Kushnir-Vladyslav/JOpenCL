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

package io.github.kushnirvladyslav.util;

/**
 * Enumeration representing the possible states of OpenCL components.
 * Used by OpenCL contexts and devices to track their lifecycle state.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-08-13
 */
public enum StatusCL {
    /**
     * Component is initialized and ready for operations
     */
    READY,

    /**
     * Component is actively executing computations or commands
     */
    RUNNING,

    /**
     * Component has been closed and resources have been released
     */
    CLOSED;
}
