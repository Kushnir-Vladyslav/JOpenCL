package com.jopencl.core.memory.buffer.typedBuffers.globalBuffer;

import com.jopencl.core.memory.buffer.Readable;

public class GlobalDynamicReadOnlyBuffer
        extends GlobalDynamicBuffer
        implements Readable<GlobalDynamicReadOnlyBuffer> {

    public GlobalDynamicReadOnlyBuffer () {
        setReadable(true);
    }

}
