package com.jopencl.core.memory.buffer;

public interface AdditionalInitiation <T extends AbstractBuffer & AdditionalInitiation<T>> {
    void addInit();
}
