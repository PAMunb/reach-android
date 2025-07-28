package br.unb.cic.reach.apk.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for file system operations.
 *
 * This class provides utility methods for file and directory operations
 * commonly needed during APK processing, including directory cleanup
 * and temporary file management for decompilation operations.
 *
 * ### Architectural Decisions:
 * - Static utility methods for file system operations
 * - Comprehensive error handling and logging
 * - Safe directory deletion with recursive traversal
 * - Integration with decompilation workflows
 *
 * ### Role in the System:
 * - File system utility foundation for APK processing
 * - Temporary directory management for decompilation
 * - Safe cleanup operations for analysis workflows
 * - Error handling and logging for file operations
 */
public class FileUtil {
    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    /**
     * Recursively deletes a directory and all its contents.
     *
     * Safely removes directories and files using modern Java NIO APIs
     * with comprehensive error handling and logging for debugging
     * decompilation and cleanup operations.
     *
     * ### Implementation Notes:
     * - Uses Files.walk() for safe recursive traversal
     * - Deletes files before directories to avoid conflicts
     * - Handles non-existent directories gracefully
     * - Provides detailed logging for debugging
     *
     * @param dir The directory to delete
     * @throws IOException if deletion fails due to file system errors
     */
    public static void delete(File dir) throws IOException {
        if (dir == null) {
            log.debug("Delete called with null directory, skipping");
            return;
        }

        log.debug("Deleting directory: {}", dir.getAbsolutePath());

        if (!dir.exists()) {
            log.debug("Directory does not exist, skipping deletion: {}", dir.getAbsolutePath());
            return;
        }

        try {
            Files.walk(Path.of(dir.getAbsolutePath()))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(file -> {
                        if (!file.delete()) {
                            log.warn("Failed to delete file: {}", file.getAbsolutePath());
                        }
                    });
            log.debug("Directory deleted successfully: {}", dir.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error deleting directory: {} - {}", dir.getAbsolutePath(), e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a temporary directory with the given prefix.
     *
     * Creates temporary directories for APK decompilation and processing
     * operations, ensuring unique directory names and proper cleanup.
     *
     * @param prefix The prefix for the temporary directory name
     * @return File representing the created temporary directory
     * @throws IOException if directory creation fails
     */
    public static File createTempDirectory(String prefix) throws IOException {
        Path tempDir = Files.createTempDirectory(prefix);
        File dir = tempDir.toFile();
        log.debug("Created temporary directory: {}", dir.getAbsolutePath());
        return dir;
    }

    /**
     * Checks if a file exists and is readable.
     *
     * Validates file accessibility for APK processing operations,
     * providing clear error information for debugging.
     *
     * @param filePath The path to the file to check
     * @return true if file exists and is readable, false otherwise
     */
    public static boolean isFileReadable(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        boolean readable = file.exists() && file.canRead();

        if (!readable) {
            log.debug("File not readable: {}", filePath);
        }

        return readable;
    }

    /**
     * Gets file size in bytes.
     *
     * Utility method for file size information during APK processing
     * and validation operations.
     *
     * @param filePath The path to the file
     * @return File size in bytes, -1 if file doesn't exist or error
     */
    public static long getFileSize(String filePath) {
        if (!isFileReadable(filePath)) {
            return -1;
        }

        try {
            return Files.size(Path.of(filePath));
        } catch (IOException e) {
            log.debug("Error getting file size for: {} - {}", filePath, e.getMessage());
            return -1;
        }
    }
}