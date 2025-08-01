package com.jopencl.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum OpenCLVersion {
    OPENCL_1_0(1, 0, "OpenCL 1.0"),
    OPENCL_1_1(1, 1, "OpenCL 1.1"),
    OPENCL_1_2(1, 2, "OpenCL 1.2"),
    OPENCL_2_0(2, 0, "OpenCL 2.0"),
    OPENCL_2_1(2, 1, "OpenCL 2.1"),
    OPENCL_2_2(2, 2, "OpenCL 2.2"),
    OPENCL_3_0(3, 0, "OpenCL 3.0"),
    UNKNOWN(-1, -1, "Unknown");

    private final int major;
    private final int minor;
    private final String displayName;

    OpenCLVersion(int major, int minor, String displayName) {
        this.major = major;
        this.minor = minor;
        this.displayName = displayName;
    }

    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public String getDisplayName() { return displayName; }

    public boolean isAtLeast(OpenCLVersion other) {
        if (this.major > other.major) return true;
        if (this.major < other.major) return false;
        return this.minor >= other.minor;
    }

    public static OpenCLVersion getOpenCLVersion(String versionPlatform) {
        int major;
        int minor;

        Pattern pattern = Pattern.compile("OpenCL\\s+(\\d+)\\.(\\d+)\\s*(.*)");
        Matcher matcher = pattern.matcher(versionPlatform);

        if (matcher.find()) {
            major = Integer.parseInt(matcher.group(1));
            minor = Integer.parseInt(matcher.group(2));

            for (OpenCLVersion version : OpenCLVersion.values()) {
                if (version.major == major && version.minor == minor) {
                    return version;
                }
            }
        }
        return UNKNOWN;
    }

    public static OpenCLVersion getOpenCLVersion(int major, int minor) {
        for (OpenCLVersion version : OpenCLVersion.values()) {
            if (version.major == major && version.minor == minor) {
                return version;
            }
        }
        return UNKNOWN;
    }

    public int getVersionCode() {
        return major * 100 + minor * 10;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
