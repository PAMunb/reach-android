package br.unb.cic.reach.common.model;

/**
 * Represents the type of application being analyzed.
 *
 * This enumeration defines the different application formats supported
 * by the reachability analysis framework, enabling type-specific processing
 * and validation logic.
 *
 * ### Architectural Decisions:
 * - Simple enumeration for clear type identification
 * - Extensible design for future application format support
 * - File extension-based detection for automatic type inference
 *
 * ### Role in the System:
 * - Enables factory pattern for ApplicationExtractor creation
 * - Provides type-safe application format identification
 * - Supports conditional processing based on application type
 */
public enum ApplicationType {
    ANDROID("apk"),
    JAR("jar"),
    UNKNOWN("unknown");

    private final String extension;

    ApplicationType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }

    /**
     * Determines application type from file path extension.
     *
     * Analyzes the file extension to automatically detect the application
     * type, enabling factory pattern implementation for extractor selection.
     *
     * @param filePath Path to the application file
     * @return ApplicationType based on file extension, UNKNOWN if not recognized
     */
    public static ApplicationType fromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return UNKNOWN;
        }
        
        String lowerPath = filePath.toLowerCase();
        if (lowerPath.endsWith(".apk")) {
            return ANDROID;
        } else if (lowerPath.endsWith(".jar")) {
            return JAR;
        }
        
        return UNKNOWN;
    }
}