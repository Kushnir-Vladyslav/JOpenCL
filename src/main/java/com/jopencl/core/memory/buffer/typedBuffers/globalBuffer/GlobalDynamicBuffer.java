package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.AbstractGlobalBuffer;
import com.jopencl.core.memory.buffer.Dynamical;

public class GlobalDynamicBuffer extends AbstractGlobalBuffer implements Dynamical<GlobalDynamicBuffer> {
    public GlobalDynamicBuffer () {
        setDynamic(true);
        setInitSize(10);
    }

}
