package com.jopencl.core.memory.buffer.typedBuffers;


import com.jopencl.core.memory.buffer.KernelAwareBuffer;
import com.jopencl.core.memory.data.ConvertToByteBuffer;
import com.jopencl.core.memory.data.Data;
import com.jopencl.util.OpenClContext;
import org.lwjgl.opencl.CL10;

public class ParameterBuffer
        extends KernelAwareBuffer {

    public ParameterBuffer () {
        setInitSize(1);
        setCopyNativeBuffer(true);
    }

    @Override
    public void addInit() {
        super.addInit();

        if (capacity != 1) {
            throw new IllegalStateException("ParameterBuffer can only have a unit size.");
        }

        if (!(dataObject instanceof ConvertToByteBuffer)) {
            initErr("Data class doesn't extends of \"ConvertToByteBuffer\" interface.");
        }
    }

    public void setup (Class<Data> clazz, OpenClContext context) {
        setDataClass(clazz);
        setOpenClContext(context);
        init();
    }

    public void setup (String bufferName, Class<Data> clazz, OpenClContext context) {
        setBufferName(bufferName);
        setDataClass(clazz);
        setOpenClContext(context);
        init();
    }

    public void setParameter (Object object) {
        ((ConvertToByteBuffer) dataObject).convertToByteBuffer(nativeBuffer, object);
    }


    @Override
    protected void setKernelArg(long targetKernel, int argIndex) {
        CL10.clSetKernelArg(
                targetKernel,
                argIndex,
                nativeBuffer
        );
    }
}
