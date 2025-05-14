package com.jopencl.core.memory.data.typical;

import com.jopencl.core.memory.data.ConvertFromByteBuffer;
import com.jopencl.core.memory.data.ConvertToByteBuffer;
import com.jopencl.core.memory.data.Data;

import java.nio.ByteBuffer;

public class IntData extends Data implements ConvertFromByteBuffer, ConvertToByteBuffer {
    @Override
    public void convertFromByteBuffer(ByteBuffer nativeBuffer, Object arr) {
        if (!(arr instanceof int[] casted)) {
            throw new IllegalArgumentException("Data cannot be casted to int[].");
        }

        if (casted.length < nativeBuffer.capacity() / Integer.BYTES) {
            throw new IllegalStateException("ByteBuffer larger than target array.");
        }

        nativeBuffer.rewind();

        for (int i = 0; i < nativeBuffer.capacity() / Integer.BYTES; i++) {
            casted[i] = nativeBuffer.getInt();
        }

        nativeBuffer.rewind();
    }

    @Override
    public Object createArr(int len) {
        return new int[len];
    }

    @Override
    public void convertToByteBuffer(ByteBuffer nativeBuffer, Object arr) {
        if (!(arr instanceof int[] casted)) {
            throw new IllegalArgumentException("Data cannot be casted to int[].");
        }

        if (casted.length > nativeBuffer.capacity() / Integer.BYTES) {
            throw new IllegalStateException("Data array larger than target ByteBuffer.");
        }

        nativeBuffer.rewind();

        for (int j : casted) {
            nativeBuffer.putInt(j);
        }

        nativeBuffer.rewind();
    }

    @Override
    public int getSizeStruct() {
        return Integer.BYTES;
    }

    @Override
    public int getSizeArray(Object arr) {
        if (!(arr instanceof int[] casted)) {
            throw new IllegalArgumentException("Data cannot be casted to int[].");
        }

        return casted.length;
    }
}
