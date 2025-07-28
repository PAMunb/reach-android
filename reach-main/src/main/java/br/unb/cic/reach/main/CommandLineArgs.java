package br.unb.cic.reach.main;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.Parameter;

import br.unb.cic.reach.common.model.ComponentType;
import br.unb.cic.reach.common.writer.WriterType;

/**
 * Command line argument configuration for reachability analysis.
 *
 * This class defines and validates command line parameters for configuring
 * reachability analysis behavior, including application selection, analysis
 * scope, and output formatting options with automatic format detection.
 *
 * ### Architectural Decisions:
 * - Automatic application type detection based on file extensions
 * - Sensible defaults for common analysis scenarios
 * - Comprehensive parameter validation with descriptive error messages
 * - Support for both Android APK and Java JAR analysis
 *
 * ### Role in the System:
 * - Command line interface configuration and validation
 * - Parameter processing and type conversion
 * - Default value management for user convenience
 * - Integration point between CLI and analysis execution
 */
public class CommandLineArgs {

    @Parameter(names = {"--input", "-i"},
            description = "Input file (APK or JAR)", required = true)
    private String inputPath;

    @Parameter(names = {"--targets", "-t"},
            description = "Target methods file containing method signatures")
    private String targetsFile;

    @Parameter(names = {"--entry-points", "-e"},
            description = "Custom entry points file (required for JAR without main())")
    private String entryPointsFile;

    @Parameter(names = {"--output", "-o"},
            description = "Output file for analysis results", required = true)
    private String outputFile;

    @Parameter(names = {"--extract-only"},
            description = "Extract information only, skip reachability analysis")
    private boolean extractOnly = false;

    @Parameter(names = {"--writer", "-w"},
            description = "Output format: csv, json (default: csv)")
    private String writerType = "csv";

    // Android-specific parameters
    @Parameter(names = {"--android-dir", "-d"},
            description = "Android platforms directory (default: $ANDROID_HOME/platforms)")
    private String androidDir;

    @Parameter(names = {"--rt-jar", "-r"},
            description = "Runtime JAR path (default: ~/.sdkman/candidates/java/8.0.302-open/jre/lib/rt.jar)")
    private String rtJar;

    @Parameter(names = {"--timeout"},
            description = "Analysis timeout in seconds (default: 300)")
    private int timeout = 300;

    @Parameter(names = {"--app-package-only"},
            description = "Analyze only application package classes (default: true)")
    private boolean appPackageOnly = true;

    @Parameter(names = {"--entry-point-types"},
            description = "Component types for entry points: all, activities, services, receivers, providers (default: all)")
    private String entryPointTypes = "all";

    @Parameter(names = {"--debug"},
            description = "Enable debug logging")
    private boolean debug = false;

    @Parameter(names = {"--help", "-h"},
            description = "Show help information", help = true)
    private boolean help = false;

    /**
     * Initializes command line arguments with default values.
     *
     * Sets up default values for Android and Java runtime paths based on
     * common environment configurations and standard installation locations.
     */
    public CommandLineArgs() {
        initializeDefaults();
    }

    /**
     * Initializes default values for configuration parameters.
     */
    private void initializeDefaults() {
        // Android platforms directory default
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null) {
            androidDir = androidHome + File.separatorChar + "platforms";
        }

        // RT JAR default to SDKMAN Java 8 installation
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            rtJar = userHome + File.separatorChar + ".sdkman" + File.separatorChar +
                    "candidates" + File.separatorChar + "java" + File.separatorChar +
                    "8.0.302-open" + File.separatorChar + "jre" + File.separatorChar +
                    "lib" + File.separatorChar + "rt.jar";
        }
    }

    /**
     * Validates command line arguments for consistency and completeness.
     *
     * Performs comprehensive validation of parameter combinations and
     * file accessibility, providing clear error messages for configuration
     * issues and missing dependencies.
     *
     * ### Validation Rules:
     * - Input file existence and readability
     * - Android-specific parameter requirements for APK files
     * - Target file requirements for reachability analysis
     * - Output directory writability
     * - Parameter combination consistency
     *
     * @throws IllegalArgumentException if validation fails with descriptive message
     */
    public void validate() {
        // Validate input file
        File input = new File(inputPath);
        if (!input.exists() || !input.canRead()) {
            throw new IllegalArgumentException("Input file not found or not readable: " + inputPath);
        }

        // Determine application type and validate specific requirements
        if (inputPath.toLowerCase().endsWith(".apk")) {
            validateAndroidParameters();
        } else if (inputPath.toLowerCase().endsWith(".jar")) {
            validateJarParameters();
        } else {
            throw new IllegalArgumentException("Unsupported file format. Supported: .apk, .jar");
        }

        // Validate target file if provided
        if (targetsFile != null) {
            File targets = new File(targetsFile);
            if (!targets.exists() || !targets.canRead()) {
                throw new IllegalArgumentException("Target methods file not found or not readable: " + targetsFile);
            }
        }

        // Validate output directory
        File output = new File(outputFile);
        File outputDir = output.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new IllegalArgumentException("Cannot create output directory: " + outputDir);
            }
        }

        // Validate writer type
        try {
            WriterType.fromString(writerType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid writer type: " + writerType + ". Supported: csv, json");
        }
    }

    /**
     * Validates Android-specific parameters.
     */
    private void validateAndroidParameters() {
        if (androidDir == null) {
            throw new IllegalArgumentException("Android platforms directory required for APK analysis. " +
                    "Use -d parameter or set ANDROID_HOME environment variable.");
        }

        File androidDirFile = new File(androidDir);
        if (!androidDirFile.exists() || !androidDirFile.isDirectory()) {
            throw new IllegalArgumentException("Android platforms directory not found: " + androidDir);
        }

        if (rtJar == null) {
            throw new IllegalArgumentException("Runtime JAR path required for APK analysis. Use -r parameter.");
        }

        File rtJarFile = new File(rtJar);
        if (!rtJarFile.exists() || !rtJarFile.canRead()) {
            throw new IllegalArgumentException("Runtime JAR not found or not readable: " + rtJar);
        }
    }

    /**
     * Validates JAR-specific parameters.
     */
    private void validateJarParameters() {
        // For reachability analysis, targets are required
        if (!extractOnly && targetsFile == null) {
            throw new IllegalArgumentException("Target methods file required for JAR reachability analysis. " +
                    "Use -t parameter or --extract-only for basic extraction.");
        }
    }

    /**
     * Parses entry point types from string specification.
     *
     * Converts comma-separated string of component types to Set of ComponentType
     * enums, supporting both individual types and the "all" shorthand.
     *
     * @return Set of ComponentType enums representing requested entry point types
     */
    public Set<ComponentType> getEntryPointTypesSet() {
        if ("all".equalsIgnoreCase(entryPointTypes)) {
            return Set.of(ComponentType.ACTIVITY, ComponentType.SERVICE,
                    ComponentType.RECEIVER, ComponentType.PROVIDER);
        }

        Set<ComponentType> types = new HashSet<>();
        for (String type : entryPointTypes.split(",")) {
            String trimmed = type.trim().toLowerCase();
            switch (trimmed) {
                case "activities" -> types.add(ComponentType.ACTIVITY);
                case "services" -> types.add(ComponentType.SERVICE);
                case "receivers" -> types.add(ComponentType.RECEIVER);
                case "providers" -> types.add(ComponentType.PROVIDER);
                default -> throw new IllegalArgumentException("Invalid entry point type: " + type);
            }
        }

        return types;
    }

    /**
     * Gets writer type as enum.
     */
    public WriterType getWriterTypeEnum() {
        return WriterType.fromString(writerType);
    }

    // Getters
    public String getInputPath() { return inputPath; }
    public String getTargetsFile() { return targetsFile; }
    public String getEntryPointsFile() { return entryPointsFile; }
    public String getOutputFile() { return outputFile; }
    public boolean isExtractOnly() { return extractOnly; }
    public String getAndroidDir() { return androidDir; }
    public String getRtJar() { return rtJar; }
    public int getTimeout() { return timeout; }
    public boolean isAppPackageOnly() { return appPackageOnly; }
    public boolean isDebug() { return debug; }
    public boolean isHelp() { return help; }
}