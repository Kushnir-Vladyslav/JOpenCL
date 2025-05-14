package com.jopencl.core.memory.data.typical;

import com.jopencl.core.memory.data.ConvertFromByteBuffer;
import com.jopencl.core.memory.data.ConvertToByteBuffer;
import com.jopencl.core.memory.data.Data;

import java.nio.ByteBuffer;

public class FloatData extends Data implements ConvertFromByteBuffer, ConvertToByteBuffer {
    @Override
    public void convertFromByteBuffer(ByteBuffer nativeBuffer, Object arr) {
        if (!(arr instanceof float[] casted)) {
            throw new IllegalArgumentException("Data cannot be casted to float[].");
        }

        if (casted.length < nativeBuffer.capacity() / Float.BYTES) {
            throw new IllegalStateException("ByteBuffer larger than target array.");
        }

        nativeBuffer.rewind();

        for (int i = 0; i < nativeBuffer.capacity() / Float.BYTES; i++) {
            casted[i] = nativeBuffer.getFloat();
        }

        nativeBuffer.rewind();
    }

    @Override
    public Object createArr(int len) {
        return new float[len];
    }

    @Override
    public void convertToByteBuffer(ByteBuffer nativeBuffer, Object arr) {
        if (!(arr instanceof float[] casted)) {
            throw new IllegalArgumentException("Data cannot be casted to float[].");
        }

        if (casted.length > nativeBuffer.capacity() / Float.BYTES) {
            throw new IllegalStateException("Data array larger than target ByteBuffer.");
        }

        nativeBuffer.rewind();

        for (float j : casted) {
            nativeBuffer.putFloat(j);
        }

        nativeBuffer.rewind();
    }

    @Override
    public int getSizeStruct() {
        return Float.BYTES;
    }

    @Override
    public int getSizeArray(Object arr) {
        if (!(arr instanceof float[] casted)) {
            throw new IllegalArgumentException("Data cannot be casted to float[].");
        }

        return casted.length;
    }
}
