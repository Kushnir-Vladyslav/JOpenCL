package com.jopencl.core.memory.data;

import java.nio.ByteBuffer;

public interface ConvertToByteBuffer {
    void convertToByteBuffer(ByteBuffer nativeBuffer, Object arr);
}
