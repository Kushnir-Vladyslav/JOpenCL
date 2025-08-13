package OpenCl;

import com.jopencl.util.CLVersion;
import com.jopencl.util.Device;
import com.jopencl.util.OpenCL;
import com.jopencl.util.Platform;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for the Platform class functionality with real OpenCL platforms.
 */
class PlatformTest {
    private List<Platform> platforms;

    @BeforeEach
    void setUp() {
        platforms = OpenCL.getPlatforms();
        assumeTrue(!platforms.isEmpty(), "No OpenCL platforms available for testing");
    }

    @Test
    void basicPlatformInfo_ShouldNotBeEmpty() {
        Platform platform = platforms.get(0);

        assertNotNull(platform.getName(), "Platform name should not be null");
        assertNotNull(platform.getVendor(), "Platform vendor should not be null");
        assertNotNull(platform.getPlatformVersion(), "Platform version should not be null");
        assertNotNull(platform.getProfile(), "Platform profile should not be null");

        assertTrue(platform.getPlatformID() != 0, "Platform ID should not be 0");
    }

    @Test
    void platformVersion_ShouldBeValid() {
        Platform platform = platforms.get(0);
        CLVersion version = platform.getOpenCLVersion();

        assertNotEquals(CLVersion.UNKNOWN, version,
                "Platform should have a valid OpenCL version");
    }

    @Test
    void deviceLists_ShouldBeConsistent() {
        Platform platform = platforms.get(0);

        int totalDevices = platform.getTotalDeviceCount();
        int sumDevices = platform.getCPUDeviceCount() +
                platform.getGPUDeviceCount() +
                platform.getAcceleratorDeviceCount();

        assertEquals(totalDevices, platform.getAllDevices().size(),
                "Total device count should match getAllDevices size");
        assertEquals(totalDevices, sumDevices,
                "Sum of device types should equal total devices");
    }

    @Test
    void getBestDevice_ShouldFollowPriority() {
        Platform platform = platforms.get(0);
        Device bestDevice = platform.getBestDevice();

        if (platform.hasGPUDevices()) {
            assertTrue(bestDevice.isGPU(),
                    "Best device should be GPU when GPU is available");
        } else if (platform.hasCPUDevices()) {
            assertTrue(bestDevice.isCPU(),
                    "Best device should be CPU when no GPU but CPU is available");
        } else if (platform.hasAcceleratorDevices()) {
            assertTrue(bestDevice.isAccelerator(),
                    "Best device should be Accelerator when only Accelerator is available");
        } else {
            assertNull(bestDevice,
                    "Best device should be null when no devices available");
        }
    }

    @Test
    void deviceOwnership_ShouldBeCorrect() {
        Platform platform = platforms.get(0);
        List<Device> devices = platform.getAllDevices();

        for (Device device : devices) {
            assertTrue(platform.owns(device),
                    "Platform should own its devices");
            assertEquals(platform, device.getPlatform(),
                    "Device should reference its platform");
        }
    }
}
