import com.jopencl.core.memory.buffer.typedBuffers.globalBuffer.GlobalStaticBuffer;
import com.jopencl.core.memory.buffer.typedBuffers.globalBuffer.GlobalStaticReadWriteBuffer;
import com.jopencl.core.memory.buffer.typedBuffers.globalBuffer.GlobalStaticWriteOnlyBuffer;
import com.jopencl.core.memory.data.typical.FloatData;
import com.jopencl.util.OpenCL;
import com.jopencl.util.OpenClContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class BuffersTest {

    static OpenCL openCL;
    static OpenClContext context;

    @BeforeAll
    static void setUp () {
        openCL = new OpenCL();
        context = openCL.getContext();
    }

    @Test
    @Disabled
    void bufferTest() {
        long kernel;     // Ідентифікатор OpenCL ядра
        long program;

        String kernelSource = """
                __kernel void TestKernel(__global const float *a,
                                         __global const float *b,
                                         __global float *result,
                                         __local float *local_a,
                                         __local float *local_b,
                                         const int num)
                {
                    int gid = get_global_id(0);
                    int lid = get_local_id(0);
                    int group_size = get_local_size(0);
                
                    // "Завантаження даних в локальну пам'ять"
                    local_a[lid] = a[gid];
                    local_b[lid] = b[gid];
                
                    // "Синхронізація локальної групи"
                    barrier(CLK_LOCAL_MEM_FENCE);
                    // "Додавання з використанням локальної пам'яті"
                    result[gid] = local_a[lid] + local_b[lid] + num;
                }
                """;

        String kernelName = "TestKernel";

        program = CL10.clCreateProgramWithSource(context.context, kernelSource, null);
        if (program == 0) {
            throw new RuntimeException("Failed to create OpenCL program");
        }

        int buildStatus = CL10.clBuildProgram(program, context.device, "", null, 0);
        if (buildStatus != CL10.CL_SUCCESS) {
            // Отримання журналу компіляції
            PointerBuffer sizeBuffer = MemoryStack.stackMallocPointer(1);
            CL10.clGetProgramBuildInfo(program, context.device, CL10.CL_PROGRAM_BUILD_LOG, (ByteBuffer) null, sizeBuffer);

            ByteBuffer buildLogBuffer = MemoryStack.stackMalloc((int) sizeBuffer.get(0));
            CL10.clGetProgramBuildInfo(program, context.device, CL10.CL_PROGRAM_BUILD_LOG, buildLogBuffer, null);

            String buildLog = MemoryUtil.memUTF8(buildLogBuffer);
            System.err.println("Build log:\n" + buildLog);
            throw new RuntimeException("Failed to build OpenCL program.");
        }

        IntBuffer errorBuffer = MemoryUtil.memAllocInt(1);

        kernel = CL10.clCreateKernel(program, kernelName, errorBuffer);

        int error = errorBuffer.get(0);
        MemoryUtil.memFree(errorBuffer);

        // Перевірка чи правельно пройшла уомпіляція
        if (kernel == 0) {
            throw new RuntimeException("Failed to create kernel: " + kernelName + ".\n Error code: " + error);
        }

        final int VECTOR_SIZE = 16_000; // Збільшено розмір
        final int LOCAL_WORK_SIZE = 256; // Оптимальний розмір локальної групи

        FloatBuffer aBuffer = MemoryUtil.memAllocFloat(VECTOR_SIZE);
        FloatBuffer bBuffer = MemoryUtil.memAllocFloat(VECTOR_SIZE);
        FloatBuffer resultBuffer = MemoryUtil.memAllocFloat(VECTOR_SIZE);

        for (int i = 0; i < VECTOR_SIZE; i++) {
            aBuffer.put(i, (float) Math.random());
            bBuffer.put(i, (float) Math.random());
        }
        aBuffer.rewind();
        bBuffer.rewind();

        GlobalStaticReadWriteBuffer gsb = new GlobalStaticReadWriteBuffer();
        gsb.setBufferName("a").setOpenClContext(context).setDataClass(FloatData.class).setInitSize(VECTOR_SIZE).init();

        float[] aArr = new float[VECTOR_SIZE];

        for (int i = 0; i < VECTOR_SIZE; i++) {
            aArr[i] = aBuffer.get(i);
        }

        gsb.write(aArr);
        gsb.bindKernel(kernel, 0);

        float[] res = (float[]) gsb.read();

        System.out.println(res[0]);

        long clABuffer = CL10.clCreateBuffer(context.context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR,
                aBuffer, null);
        long clBBuffer = CL10.clCreateBuffer(context.context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_COPY_HOST_PTR,
                bBuffer, null);
        long clResultBuffer = CL10.clCreateBuffer(context.context, CL10.CL_MEM_WRITE_ONLY,
                VECTOR_SIZE * Float.BYTES, null);

        // Локальна пам'ять
        long localMemSize = LOCAL_WORK_SIZE * Float.BYTES * 2;

        PointerBuffer transmitter = MemoryUtil.memAllocPointer(1);

//        CL10.clSetKernelArg(kernel, 0, transmitter.put(0, clABuffer).rewind());
        CL10.clSetKernelArg(kernel, 1, transmitter.put(0, clBBuffer).rewind());
        CL10.clSetKernelArg(kernel, 2, transmitter.put(0, clResultBuffer).rewind());

        CL10.clSetKernelArg(kernel, 3, localMemSize);
        CL10.clSetKernelArg(kernel, 4, localMemSize);

        IntBuffer num = MemoryUtil.memAllocInt(1);
        num.put(5);

        CL10.clSetKernelArg(kernel, 5,num.rewind());

        // Виконання kernel з явним розміром локальної групи
        long globalWorkSize = (long) Math.ceil(VECTOR_SIZE / (float) LOCAL_WORK_SIZE) * LOCAL_WORK_SIZE;

        PointerBuffer global = MemoryUtil.memAllocPointer(1).put(globalWorkSize).rewind();
        PointerBuffer local = MemoryUtil.memAllocPointer(1).put(LOCAL_WORK_SIZE).rewind();



        CL10.clEnqueueNDRangeKernel(
                context.commandQueue, kernel, 1, null,
                global, local,
                null, null
        );

        CL10.clEnqueueReadBuffer(context.commandQueue, clResultBuffer, true, 0,
                resultBuffer, null, null);

        for (int i = 0; i < 10; i++) {
                System.out.printf("%.2f + %.2f = %.2f%n",
                        aBuffer.get(i), bBuffer.get(i), resultBuffer.get(i));
        }

    }

    @AfterAll
    static void destroy () {
        openCL.destroy();
    }

}
