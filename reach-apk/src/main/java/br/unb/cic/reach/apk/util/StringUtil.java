package br.unb.cic.reach.apk.util;

/**
 * Utility class for string operations in APK processing.
 *
 * This class provides utility methods for string manipulation operations
 * commonly needed during APK analysis, including file name extraction
 * and path processing for Android applications.
 *
 * ### Architectural Decisions:
 * - Static utility methods for string operations
 * - Cross-platform path handling for different operating systems
 * - File extension processing for application type detection
 * - Clean API for common string manipulation needs
 *
 * ### Role in the System:
 * - String processing foundation for APK analysis
 * - File name and path manipulation utilities
 * - Application label generation from file paths
 * - Cross-platform compatibility for path operations
 */
public class StringUtil {

    /**
     * Converts APK file path to application label.
     *
     * Extracts a clean application label from the APK file path by removing
     * directory paths and file extensions, providing a user-friendly name
     * for application identification and logging.
     *
     * ### Implementation Notes:
     * - Handles both Windows (\) and Unix (/) path separators
     * - Removes .apk extension for clean labeling
     * - Returns empty string for invalid inputs
     *
     * @param apkPath The full path to the APK file
     * @return Clean application label without path and extension
     */
    public static String convertToLabel(String apkPath) {
        if (apkPath == null || apkPath.isEmpty()) {
            return "";
        }

        // Handle both Windows and Unix path separators
        String[] nameArray = apkPath.replace('\\', '/').split("/");
        String appFullName = nameArray[nameArray.length - 1];

        // Remove .apk extension
        int length = appFullName.length();
        if (appFullName.toLowerCase().endsWith(".apk") && length > 4) {
            return appFullName.substring(0, length - 4);
        }

        return appFullName;
    }

    /**
     * Extracts file name from full path.
     *
     * Utility method for extracting just the file name portion from
     * a full file path, handling cross-platform path separators.
     *
     * @param filePath The full file path
     * @return File name without directory path
     */
    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        String[] pathParts = filePath.replace('\\', '/').split("/");
        return pathParts[pathParts.length - 1];
    }

    /**
     * Removes file extension from file name.
     *
     * Utility method for removing file extensions while preserving
     * the base file name for labeling and identification purposes.
     *
     * @param fileName The file name with extension
     * @return File name without extension
     */
    public static String removeExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }

    /**
     * Gets file extension from file name.
     *
     * Extracts the file extension for type detection and validation
     * during APK processing operations.
     *
     * @param fileName The file name
     * @return File extension without the dot, or empty string if no extension
     */
    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 && lastDot < fileName.length() - 1
                ? fileName.substring(lastDot + 1).toLowerCase()
                : "";
    }

    /**
     * Checks if a string is null or empty.
     *
     * Convenience method for string validation in APK processing
     * operations and configuration validation.
     *
     * @param str The string to check
     * @return true if string is null or empty, false otherwise
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Safely trims a string, handling null values.
     *
     * Utility method for safe string trimming during manifest
     * attribute processing and configuration parsing.
     *
     * @param str The string to trim
     * @return Trimmed string or null if input was null
     */
    public static String safeTrim(String str) {
        return str != null ? str.trim() : null;
    }
}