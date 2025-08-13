package OpenCl;
import com.jopencl.util.Device;
import com.jopencl.util.OpenCL;
import com.jopencl.util.Platform;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for the Device class functionality with real OpenCL devices.
 */
class DeviceTest {
    private List<Device> devices;

    @BeforeEach
    void setUp() {
        List<Platform> platforms = OpenCL.getPlatforms();
        assumeTrue(!platforms.isEmpty(), "No OpenCL platforms available for testing");

        Platform platform = platforms.get(0);
        devices = platform.getAllDevices();
        assumeTrue(!devices.isEmpty(), "No OpenCL devices available for testing");
    }

    @Test
    void basicDeviceInfo_ShouldNotBeEmpty() {
        Device device = devices.get(0);

        assertNotNull(device.getName(), "Device name should not be null");
        assertNotNull(device.getVendor(), "Device vendor should not be null");
        assertNotNull(device.getDriverVersion(), "Driver version should not be null");
        assertNotNull(device.getDeviceVersion(), "Device version should not be null");
        assertNotNull(device.getProfile(), "Device profile should not be null");

        assertTrue(device.getDeviceID() != 0, "Device ID should not be 0");
    }

    @Test
    void computeCapabilities_ShouldBeValid() {
        Device device = devices.get(0);

        assertTrue(device.getMaxComputeUnits() > 0,
                "Device should have at least one compute unit");
        assertTrue(device.getMaxClockFrequency() > 0,
                "Device should have valid clock frequency");
        assertTrue(device.getMaxWorkItemDimensions() > 0,
                "Device should support at least one work item dimension");

        long[] workItemSizes = device.getMaxWorkItemSizes();
        assertNotNull(workItemSizes, "Work item sizes should not be null");
        assertTrue(workItemSizes.length > 0,
                "Device should support work items");
    }

    @Test
    void memoryCapabilities_ShouldBeValid() {
        Device device = devices.get(0);

        assertTrue(device.getMaxGlobalBufferSize() > 0,
                "Device should have global memory");
        assertTrue(device.getMaxMemAllocSize() > 0,
                "Device should support memory allocation");
        assertTrue(device.getMaxLocalMemSize() > 0,
                "Device should have local memory");
        assertTrue(device.getMaxConstantBufferSize() > 0,
                "Device should support constant buffers");
    }

    @Test
    void deviceType_ShouldBeValid() {
        Device device = devices.get(0);
        String type = device.getDeviceType();

        assertNotNull(type, "Device type should not be null");
        assertTrue(device.isCPU() || device.isGPU() || device.isAccelerator(),
                "Device should be either CPU, GPU, or Accelerator");
    }

    @Test
    void formattedMemorySize_ShouldBeReadable() {
        Device device = devices.get(0);
        String memSize = device.getFormattedGlobalMemSize();

        assertNotNull(memSize, "Formatted memory size should not be null");
        assertTrue(memSize.matches("\\d+(\\,\\d+)? [KMGT]B"),
                "Memory size should be formatted correctly");
    }

    @Test
    void deviceSummaries_ShouldBeFormatted() {
        Device device = devices.get(0);

        assertNotNull(device.getMemorySummary(),
                "Memory summary should not be null");
        assertNotNull(device.getComputeSummary(),
                "Compute summary should not be null");
        assertNotNull(device.getCapabilitiesSummary(),
                "Capabilities summary should not be null");
        assertNotNull(device.getExtensionSummary(),
                "Extension summary should not be null");
    }
}
