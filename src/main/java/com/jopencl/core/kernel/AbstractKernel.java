package com.jopencl.core.kernel;

import com.jopencl.core.memory.buffer.AbstractBuffer;
import com.jopencl.util.OpenClContext;
import org.lwjgl.PointerBuffer;

public abstract class AbstractKernel {
    protected long kernel;
    protected long program;

    protected OpenClContext context;

    protected PointerBuffer global;
    protected PointerBuffer local;

    private int dimension;

    protected AbstractBuffer[] buffers;

    private int numberBuffers;

    protected void createProgram () {

    }

    public void init () {

    }

    public void run () {

    }

    public void destroy () {

    }
}
