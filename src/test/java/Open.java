import com.jopencl.util.Device;
import com.jopencl.util.OpenCL;
import com.jopencl.util.Platform;
import org.junit.jupiter.api.Test;

import java.util.List;

public class Open {
    List<Platform> platformList = OpenCL.getPlatforms();

    @Test
    void testInfo() {
        System.out.println("Number of platforms: " + platformList.size());

        System.out.println("=======================================================");

        for(Platform platform : platformList) {

            green("======================="+platform.getName()+"==========================");
            red(platform.toString());

            for (Device device : platform.getAllDevices()) {
                yellow("======================="+device.getName()+"==========================");
                blue(device.toString());
            }
        }
        System.out.println("=======================================================");
    }


    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_YELLOW = "\u001B[34m";

    void red(String text) {
        System.out.println(ANSI_RED +  text + ANSI_RESET);
    }

    void green(String text) {
        System.out.println(ANSI_GREEN +  text + ANSI_RESET);
    }

    void blue(String text) {
        System.out.println(ANSI_BLUE +  text + ANSI_RESET);
    }

    void yellow(String text) {
        System.out.println(ANSI_YELLOW +  text + ANSI_RESET);
    }
}
