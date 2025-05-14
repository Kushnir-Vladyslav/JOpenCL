package com.jopencl.core.memory.data;

import java.nio.ByteBuffer;

public interface ConvertFromByteBuffer {
    void convertFromByteBuffer (ByteBuffer nativeBuffer, Object arr);

    Object createArr (int len);
}
