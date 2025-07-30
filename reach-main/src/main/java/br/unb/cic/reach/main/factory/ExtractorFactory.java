package br.unb.cic.reach.main.factory;

import br.unb.cic.reach.mop.AndroidExtractor;
import br.unb.cic.reach.common.extractor.ApplicationExtractor;
import br.unb.cic.reach.common.extractor.UnsupportedFormatException;
import br.unb.cic.reach.common.model.ApplicationType;
import br.unb.cic.reach.jar.JarExtractor;

/**
 * Factory for creating application extractors based on file type.
 * <p>
 * This factory implements automatic detection of application types and
 * creates appropriate extractor instances, enabling transparent support
 * for multiple application formats through a unified interface.
 * <p>
 * ### Architectural Decisions:
 * - Automatic type detection based on file extensions
 * - Factory pattern for extractor instantiation
 * - Direct instantiation instead of reflection for better performance
 * - Clear error handling for unsupported formats
 * <p>
 * ### Role in the System:
 * - Central point for extractor creation and type detection
 * - Abstraction enabling transparent multi-format support
 * - Extension point for adding new application format processors
 * - Interface between CLI processing and analysis execution
 */
public class ExtractorFactory {

    /**
     * Creates an appropriate extractor for the given input file.
     * <p>
     * Analyzes the input file path to determine the application type and
     * instantiates the corresponding extractor implementation. Supports
     * automatic format detection and validation.
     * <p>
     * ### Implementation Notes:
     * - Uses ApplicationType.fromPath() for automatic detection
     * - Instantiates extractors using direct construction for better performance
     * - Throws descriptive exceptions for unsupported formats
     *
     * @param inputPath Path to the application file to be analyzed
     * @return ApplicationExtractor instance appropriate for the file type
     * @throws UnsupportedFormatException if the file format is not supported
     */
    public static ApplicationExtractor create(String inputPath) {
        ApplicationType type = ApplicationType.fromPath(inputPath);

        return switch (type) {
            case ANDROID -> new AndroidExtractor();
            case JAR -> new JarExtractor();
            default -> throw new UnsupportedFormatException(
                    "Unsupported application format: " + inputPath +
                            ". Supported formats: .apk, .jar");
        };
    }
}