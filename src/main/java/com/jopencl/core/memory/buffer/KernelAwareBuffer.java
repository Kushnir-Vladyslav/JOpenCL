package com.jopencl.core.memory.buffer;

import com.jopencl.core.kernel.Kernel;

import java.util.ArrayList;
import java.util.List;

public abstract class KernelAwareBuffer
        extends AbstractBuffer
        implements AdditionalInitiation<KernelAwareBuffer> {
    private final List<KernelDependency> kernels = new ArrayList<>();

    public void bindKernel(Kernel targetKernel, int numberArg) {
        kernels.add(new KernelDependency(targetKernel.getKernel(), numberArg));
        setKernelArg(targetKernel.getKernel(), numberArg);
    }

    public void bindKernel(long targetKernel, int numberArg) {
        kernels.add(new KernelDependency(targetKernel, numberArg));
        setKernelArg(targetKernel, numberArg);
    }

    protected void removeKernel(Kernel kernel) {
        kernels.removeIf(value -> value.targetKernel == kernel.getKernel());
    }

    protected void removeKernel(long kernel) {
        kernels.removeIf(value -> value.targetKernel == kernel);
    }

    protected void removeKernelAndClose (long kernel) {
        kernels.removeIf(value -> value.targetKernel == kernel);

        if (kernels.isEmpty()) {
            destroy();
        }
    }
    protected void setAllKernelArg () {
        for (KernelDependency kernelDependency : kernels) {
            setKernelArg(kernelDependency.targetKernel, kernelDependency.argIndex);
        }
    }

    @Override
    public void addInit () {
        setAllKernelArg();
    }

    @Override
    public void destroy () {
        kernels.clear();

        super.destroy();
    }

    protected static class KernelDependency {
        public long targetKernel;
        public int argIndex;

        public KernelDependency (long targetKernel, int argIndex) {
            this.targetKernel = targetKernel;
            this.argIndex = argIndex;
        }
    }
}
