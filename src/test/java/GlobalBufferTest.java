import com.jopencl.core.memory.buffer.typedBuffers.globalBuffer.GlobalStaticReadWriteBuffer;
import com.jopencl.core.memory.data.typical.FloatData;
import com.jopencl.util.OpenCL;
import com.jopencl.util.OpenClContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GlobalBufferTest {

    @Test
    void test () {
        OpenCL openCL = new OpenCL();
        OpenClContext context = openCL.getContext();

        int size = 10;

        GlobalStaticReadWriteBuffer buffer = new GlobalStaticReadWriteBuffer();
        buffer.setup("a", FloatData.class, context, true, true, size);

        float[] arr = new float[size];

        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }

        buffer.write(arr);

        float[] nArr = (float[]) buffer.read();

        assertEquals(size, nArr.length);

        for (int i = 0; i < size; i++) {
            assertEquals(i, nArr[i]);
        }

    }

}
