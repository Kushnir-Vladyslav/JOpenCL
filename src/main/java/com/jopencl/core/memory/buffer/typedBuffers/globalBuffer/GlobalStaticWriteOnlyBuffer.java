package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Writable;

public class GlobalStaticWriteOnlyBuffer
        extends GlobalStaticBuffer implements Writable<GlobalStaticWriteOnlyBuffer> {

    public GlobalStaticWriteOnlyBuffer() {
        setWritable(true);
    }
}
