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

package io.github.kushnirvladyslav.util.fileManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading files from various sources including classpath resources and filesystem.
 * This class provides methods to search and load files from both the application's resources
 * and the file system.
 *
 * @author Vladyslav Kushnir
 * @version 1.0
 * @since 1.0
 */
public class FileLoader {
    private static final Logger logger = LoggerFactory.getLogger(FileLoader.class);

    /**
     * Loads a file from the classpath resources.
     *
     * @param fileName the name of the file to load from resources
     * @return an InputStream of the file if found, or null if the file doesn't exist
     */
    public static InputStream fileResource(String fileName) {
        validateFileName(fileName);
        logger.debug("Attempting to load resource file: {}", fileName);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            logger.debug("Thread context ClassLoader is null, using FileLoader's ClassLoader");
            FileLoader fileLoader = new FileLoader();
            classLoader = fileLoader.getClass().getClassLoader();
        }

        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            logger.warn("Resource file not found: {}", fileName);
        } else {
            logger.debug("Successfully loaded resource file: {}", fileName);
        }

        return inputStream;
    }

    /**
     * Searches for and loads a file from a specific directory in the file system.
     *
     * @param path the directory path to search in
     * @param fileName the name of the file to find
     * @return an InputStream of the found file
     * @throws IOException if the file is not found or cannot be read
     */
    public static InputStream fileInFileSystem(String path, String fileName) throws IOException {
        validateFileName(fileName);
        validatePath(path);
        logger.debug("Searching for file '{}' in path: {}", fileName, path);

        try (Stream<Path> paths = Files.walk(Paths.get(path))) {
            Path found = paths
                    .filter(Files::isRegularFile)
                    .filter(pathTo -> pathTo.getFileName().toString().equals(fileName))
                    .findFirst()
                    .orElse(null);

            if (found == null) {
                logger.error("File '{}' not found in path: {}", fileName, path);
                throw new IOException("File \"" + fileName + "\" in folder/subfolder \"" + path + "\" not found.");
            }

            logger.debug("Found file '{}' at: {}", fileName, found);
            return Files.newInputStream(found);
        }
    }

    /**
     * Searches for and loads a file from multiple paths in the file system.
     *
     * @param paths list of directory paths to search in
     * @param fileName the name of the file to find
     * @return an InputStream of the first found file
     * @throws IOException if the file is not found in any of the paths or cannot be read
     */
    public static InputStream fileInFileSystem(List<String> paths, String fileName) throws IOException {
        if (paths == null) {
            logger.error("Paths list cannot be null");
            throw new IllegalArgumentException("Paths list cannot be null");
        }
        if (paths.isEmpty()) {
            logger.error("Paths list cannot be empty");
            throw new IllegalArgumentException("Paths list cannot be empty");
        }
        validateFileName(fileName);
        logger.debug("Searching for file '{}' in multiple paths: {}", fileName, paths);

        for (String path : paths) {
            try {
                InputStream result = fileInFileSystem(path, fileName);
                logger.debug("Found file '{}' in path: {}", fileName, path);
                return result;
            } catch (Exception e) {
                logger.debug("File '{}' not found in path: {}", fileName, path);
            }
        }

        logger.error("File '{}' not found in any of the specified paths", fileName);
        throw new IOException("File \"" + fileName + "\" not found.");
    }

    /**
     * Validates path parameter.
     *
     * @param path the path to validate
     * @throws IllegalArgumentException if the path is invalid
     */
    private static void validatePath(String path) {
        if (path == null) {
            logger.error("Path cannot be null");
            throw new IllegalArgumentException("Path cannot be null");
        }
        if (path.trim().isEmpty()) {
            logger.error("Path cannot be empty");
            throw new IllegalArgumentException("Path cannot be empty");
        }
    }

    /**
     * Validates file name parameter.
     *
     * @param fileName the name of the file to validate
     * @throws IllegalArgumentException if the file name is invalid
     */
    private static void validateFileName(String fileName) {
        if (fileName == null) {
            logger.error("File name cannot be null");
            throw new IllegalArgumentException("File name cannot be null");
        }
        if (fileName.trim().isEmpty()) {
            logger.error("File name cannot be empty");
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (fileName.contains("..")) {
            logger.error("File name contains invalid sequence: {}", fileName);
            throw new IllegalArgumentException("File name contains invalid sequence: " + fileName);
        }
    }
}
