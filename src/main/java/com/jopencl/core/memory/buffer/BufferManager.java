package com.jopencl.core.memory.buffer;

import java.util.ArrayList;
import java.util.List;

public class BufferManager {
    private final List<AbstractBuffer> buffers = new ArrayList<>();

    public void registerBuffer(AbstractBuffer buffer) {
        buffers.add(buffer);
    }

    public void releaseAll() {
        for (AbstractBuffer buffer : buffers) {
            buffer.destroy();
        }
        buffers.clear();
    }

    public AbstractBuffer getBuffer(String bufferName) {
        for (AbstractBuffer buffer : buffers) {
            if (buffer.getBufferName().equals(bufferName)) {
                return buffer;
            }
        }
        return null;
    }

    public void remove (AbstractBuffer buffer) {
        buffers.remove(buffer);
    }

    public void remove (String bufferName) {
        AbstractBuffer buffer = getBuffer(bufferName);

        if (buffer != null) {
            buffers.remove(buffer);
        } else {
            System.err.println("The buffer is missing from the buffer manager list.");
        }
    }

    public void release(AbstractBuffer buffer) {
        buffers.remove(buffer);
        buffer.destroy();
    }


    public void release (String bufferName) {
        AbstractBuffer buffer = getBuffer(bufferName);

        if (buffer != null) {
            buffers.remove(buffer);
            buffer.destroy();
        } else {
            System.err.println("The buffer is missing from the buffer manager list.");
        }
    }
}
