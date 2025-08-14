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

package io.github.kushnirvladyslav.memory.buffer;

/**
 * Interface defining additional lifecycle operations for OpenCL buffers.
 * Implementations can provide custom initialization and cleanup logic
 * that will be called during the standard buffer lifecycle.
 *
 * @since 1.0
 * @author Vladyslav Kushnir
 */
public interface AdditionalLifecycle {

    /**
     * Additional initialization logic to be executed after base initialization.
     * This method is called at the end of the {@link AbstractBuffer#init()} method
     * if the buffer implements this interface.
     *
     * @throws IllegalStateException if initialization fails
     */
    void additionalInit();

    /**
     * Additional cleanup logic to be executed during buffer destruction.
     * This method is called before the base cleanup in {@link AbstractBuffer#destroy()}
     * if the buffer implements this interface.
     */
    void additionalCleanup();
}
