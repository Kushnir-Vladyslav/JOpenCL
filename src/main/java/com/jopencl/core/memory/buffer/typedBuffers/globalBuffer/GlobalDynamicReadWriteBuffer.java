package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Readable;
import com.jopencl.core.memory.buffer.Writable;

public class GlobalDynamicReadWriteBuffer
        extends GlobalDynamicBuffer
        implements Readable<GlobalDynamicReadWriteBuffer>,
        Writable<GlobalDynamicReadWriteBuffer> {

    public GlobalDynamicReadWriteBuffer() {
        setReadable(true);
        setWritable(true);
    }
}
