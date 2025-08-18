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

package io.github.kushnirvladyslav.memory.util;

import org.lwjgl.opencl.CL12;

public enum HostMemoryAccess {
    READ_WRIGHT (0),
    READ_ONLY (CL12.CL_MEM_HOST_READ_ONLY),
    WRIGHT_ONLY (CL12.CL_MEM_HOST_WRITE_ONLY),
    NO_ACCESS (CL12.CL_MEM_HOST_NO_ACCESS);

    HostMemoryAccess(int flag) {
        this.flag = flag;
    }

    private final int flag;

    public int getFlag() {
        return flag;
    }
}
