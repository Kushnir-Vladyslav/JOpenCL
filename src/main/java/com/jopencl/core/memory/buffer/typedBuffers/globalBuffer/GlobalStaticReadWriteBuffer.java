package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Readable;
import com.jopencl.core.memory.buffer.Writable;

public class GlobalStaticReadWriteBuffer
        extends GlobalStaticBuffer
        implements Readable<GlobalStaticReadWriteBuffer>,
        Writable<GlobalStaticReadWriteBuffer> {

    public GlobalStaticReadWriteBuffer() {
        setReadable(true);
        setWritable(true);
    }
}
