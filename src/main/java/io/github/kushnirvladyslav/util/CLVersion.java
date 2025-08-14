/*
 * Copyright 2025 Kushnir Vladyslav
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kushnirvladyslav.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enumeration representing supported OpenCL versions.
 * Provides version comparison, parsing, and conversion utilities for OpenCL version management.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 2025-08-13
 */
public enum CLVersion {
    /**
     * OpenCL version 1.0
     */
    OPENCL_1_0(1, 0, "OpenCL 1.0"),

    /**
     * OpenCL version 1.1
     */
    OPENCL_1_1(1, 1, "OpenCL 1.1"),

    /**
     * OpenCL version 1.2
     */
    OPENCL_1_2(1, 2, "OpenCL 1.2"),

    /**
     * OpenCL version 2.0
     */
    OPENCL_2_0(2, 0, "OpenCL 2.0"),

    /**
     * OpenCL version 2.1
     */
    OPENCL_2_1(2, 1, "OpenCL 2.1"),

    /**
     * OpenCL version 2.2
     */
    OPENCL_2_2(2, 2, "OpenCL 2.2"),

    /**
     * OpenCL version 3.0
     */
    OPENCL_3_0(3, 0, "OpenCL 3.0"),

    /**
     * Unknown or unsupported OpenCL version
     */
    UNKNOWN(-1, -1, "Unknown");

    private final int major;
    private final int minor;
    private final String displayName;

    /**
     * Constructs a CLVersion with specified major, minor version numbers and display name.
     *
     * @param major       the major version number
     * @param minor       the minor version number
     * @param displayName the human-readable display name
     */
    CLVersion(int major, int minor, String displayName) {
        this.major = major;
        this.minor = minor;
        this.displayName = displayName;
    }

    /**
     * Returns the major version number.
     *
     * @return the major version number
     */
    public int getMajor() {
        return major;
    }

    /**
     * Returns the minor version number.
     *
     * @return the minor version number
     */
    public int getMinor() {
        return minor;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this version is at least as recent as the specified version.
     *
     * @param other the version to compare against
     * @return true if this version is greater than or equal to the other version
     */
    public boolean isAtLeast(CLVersion other) {
        if (this.major > other.major) return true;
        if (this.major < other.major) return false;
        return this.minor >= other.minor;
    }

    /**
     * Parses an OpenCL version string and returns the corresponding CLVersion.
     *
     * @param versionPlatform the version string to parse (e.g., "OpenCL 2.1 AMD-APP...")
     * @return the corresponding CLVersion, or UNKNOWN if parsing fails
     */
    public static CLVersion getOpenCLVersion(String versionPlatform) {
        int major;
        int minor;

        Pattern pattern = Pattern.compile("OpenCL\\s+(\\d+)\\.(\\d+)\\s*(.*)");
        Matcher matcher = pattern.matcher(versionPlatform);

        if (matcher.find()) {
            major = Integer.parseInt(matcher.group(1));
            minor = Integer.parseInt(matcher.group(2));

            for (CLVersion version : CLVersion.values()) {
                if (version.major == major && version.minor == minor) {
                    return version;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the CLVersion corresponding to the specified major and minor version numbers.
     *
     * @param major the major version number
     * @param minor the minor version number
     * @return the corresponding CLVersion, or UNKNOWN if no match is found
     */
    public static CLVersion getOpenCLVersion(int major, int minor) {
        for (CLVersion version : CLVersion.values()) {
            if (version.major == major && version.minor == minor) {
                return version;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns a numeric version code for comparison purposes.
     * Major version is multiplied by 100, minor by 10.
     *
     * @return the version code (e.g., 210 for version 2.1)
     */
    public int getVersionCode() {
        return major * 100 + minor * 10;
    }

    /**
     * Returns the display name of this version.
     *
     * @return the display name
     */
    @Override
    public String toString() {
        return displayName;
    }
}
