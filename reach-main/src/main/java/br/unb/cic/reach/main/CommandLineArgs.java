package br.unb.cic.reach.main;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import br.unb.cic.reach.common.model.AnalysisScope;
import br.unb.cic.reach.common.model.ComponentType;
import br.unb.cic.reach.common.model.ConfigMatrix;
import br.unb.cic.reach.common.model.ConfigurationException;
import br.unb.cic.reach.common.writer.WriterType;

/**
 * Command line arguments with integrated ConfigMatrix support.
 * <p>
 * This class provides a simplified interface for command line parameter processing
 * while leveraging ConfigMatrix for unified configuration management. It maintains
 * backward compatibility while enabling the full power of the configuration matrix
 * system including analysis scope filtering and optimized algorithm selection.
 * <p>
 * ### Configuration Integration:
 * - Direct ConfigMatrix construction from CLI parameters
 * - Intelligent defaults based on application characteristics
 * - Comprehensive validation with clear error messages
 * - Support for all analysis scope and algorithm combinations
 * <p>
 * ### Simplified Parameter Set:
 * The CLI focuses on essential user-facing parameters while ConfigMatrix
 * handles the complex interactions and algorithmic optimizations internally.
 */
public class CommandLineArgs {
    private static final Logger log = LoggerFactory.getLogger(CommandLineArgs.class);

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

    /**
     * Analysis scope parameter for output filtering.
     * <p>
     * This parameter directly maps to AnalysisScope and controls
     * which methods appear in the final output based on their reachability status.
     * The ConfigMatrix handles the complex interactions with other parameters
     * and provides appropriate warnings for incompatible combinations.
     */
    @Parameter(names = {"--analysis-scope"},
            description = "Scope of methods to include in output: all-methods (default) or reachable-only")
    private String analysisScope = "all-methods";

    @Parameter(names = {"--writer", "-w"},
            description = "Output format: csv, json (default: csv)")
    private String writerType = "csv";

    // Android-specific parameters
    @Parameter(names = {"--android-dir", "-d"},
            description = "Android platforms directory (default: $ANDROID_HOME/platforms)")
    private String androidDir;

    @Parameter(names = {"--rt-jar", "-r"},
            description = "Runtime JAR path (default: standard Java installation)")
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

    @Parameter(names = {"--reachability"},
            description = "Reachability strategy: soot-bfs (default), jgrapht-dijkstra")
    private ConfigMatrix.ReachabilityStrategyType reachabilityStrategyType = ConfigMatrix.ReachabilityStrategyType.SOOT_BFS;

    @Parameter(names = {"--callgraph"},
            description = "Reachability strategy: soot-bfs (default), jgrapht-dijkstra")
    private ConfigMatrix.CallGraphAlgorithm callgraphAlgorithm = ConfigMatrix.CallGraphAlgorithm.SPARK;

    @Parameter(names = {"--aliasing"},
            description = "Aliasing algorithm: flow-sensitive (default), pts-based, lazy, none")
    private ConfigMatrix.AliasingAlgorithm aliasingAlgorithm = ConfigMatrix.AliasingAlgorithm.FlowSensitive;

    @Parameter(names = {"--max-callback-depth"},
            description = "Maximum analysis callback depth (default: 10)")
    private int maxAnalysisCallbackDepth = 10;

    /**
     * Initialize command line arguments with environment-based defaults.
     */
    public CommandLineArgs() {
        initializeDefaults();
    }

    /**
     * Build ConfigMatrix from command line parameters.
     * <p>
     * This method constructs a complete ConfigMatrix instance using the CLI parameters
     * as input, applying intelligent defaults and validation. The ConfigMatrix handles
     * the complex parameter interactions and algorithm selection automatically.
     * <p>
     * ### Configuration Process:
     * 1. Parse basic parameters (scope, entry points, etc.)
     * 2. Apply intelligent defaults based on input characteristics
     * 3. Validate parameter combinations and warn about conflicts
     * 4. Select optimal algorithms based on configuration and estimated complexity
     *
     * @return Fully configured ConfigMatrix ready for analysis execution
     * @throws ConfigurationException if parameter validation fails
     */
    public ConfigMatrix buildConfigMatrix() throws ConfigurationException {
        System.out.println("******** Initializing configuration...");
        System.out.println("reachabilityStrategyType: " + reachabilityStrategyType);
        System.out.println("callgraphAlgorithm: " + callgraphAlgorithm);
        System.out.println("aliasingAlgorithm: " + aliasingAlgorithm);
        System.out.println("maxAnalysisCallbackDepth: " + maxAnalysisCallbackDepth);
        return new ConfigMatrix.Builder()
                .withAnalysisScope(AnalysisScope.fromString(analysisScope))
                .withAppPackageOnly(appPackageOnly)
                .withEntryPointTypes(parseEntryPointTypes(entryPointTypes))
                .withExtractOnly(extractOnly)
                .withReachabilityStrategy(reachabilityStrategyType)
                .withCallGraphAlgorithm(callgraphAlgorithm)
                .withAliasingAlgorithm(aliasingAlgorithm)
                .withWriterType(WriterType.fromString(writerType))
                .withTimeout(timeout)
                .withInputPath(inputPath)
                .withTargetsFile(targetsFile)
                .withEntryPointsFile(entryPointsFile)
                .withOutputFile(outputFile)
                .withAndroidPlatformsDir(androidDir)
                .withRtJarPath(rtJar)
                .withMaxAnalysisCallbackDepth(maxAnalysisCallbackDepth)
                .build();
    }

    /**
     * Validate all command line parameters with comprehensive error reporting.
     * <p>
     * This method performs validation of file paths, parameter combinations,
     * and configuration consistency. It provides detailed error messages to
     * help users correct configuration issues.
     */
    public void validate() throws ParameterException {
        // Validate input file
        File input = new File(inputPath);
        if (!input.exists() || !input.canRead()) {
            throw new ParameterException("Input file not found or not readable: " + inputPath);
        }

        // Determine application type and validate specific requirements
        if (inputPath.toLowerCase().endsWith(".apk")) {
            validateAndroidParameters();
        } else if (inputPath.toLowerCase().endsWith(".jar")) {
            validateJarParameters();
        } else {
            throw new ParameterException("Unsupported file format. Supported: .apk, .jar");
        }

        // Validate target file if provided
        if (targetsFile != null) {
            File targets = new File(targetsFile);
            if (!targets.exists() || !targets.canRead()) {
                throw new ParameterException("Target methods file not found or not readable: " + targetsFile);
            }
        }

        // Validate output directory
        File output = new File(outputFile);
        File outputDir = output.getParentFile();
        if (outputDir != null && !outputDir.exists() && !outputDir.mkdirs()) {
            throw new ParameterException("Cannot create output directory: " + outputDir);
        }

        // Validate writer type
        try {
            WriterType.fromString(writerType);
        } catch (Exception e) {
            throw new ParameterException("Invalid writer type: " + writerType + ". Supported: csv, json");
        }

        // Validate ConfigMatrix construction - this catches parameter interaction issues
        try {
            buildConfigMatrix();
        } catch (ConfigurationException e) {
            throw new ParameterException("Configuration validation failed: " + e.getMessage());
        }
    }

    /**
     * Parse entry point types into ComponentType set.
     * <p>
     * This method handles the complexity of parsing various entry point specifications,
     * including the "all" shorthand and comma-separated component lists.
     */
    private Set<ComponentType> parseEntryPointTypes(String typesString) {
        if ("all".equalsIgnoreCase(typesString)) {
            return Set.of(ComponentType.ACTIVITY, ComponentType.SERVICE,
                    ComponentType.RECEIVER, ComponentType.PROVIDER);
        }

        Set<ComponentType> types = new HashSet<>();
        for (String type : typesString.split(",")) {
            String trimmed = type.trim().toLowerCase();
            switch (trimmed) {
                case "activities" -> types.add(ComponentType.ACTIVITY);
                case "services" -> types.add(ComponentType.SERVICE);
                case "receivers" -> types.add(ComponentType.RECEIVER);
                case "providers" -> types.add(ComponentType.PROVIDER);
                default -> throw new IllegalArgumentException("Invalid entry point type: " + type +
                        ". Valid options: activities, services, receivers, providers, all");
            }
        }

        return types;
    }

    /**
     * Initialize default values based on environment configuration.
     */
    private void initializeDefaults() {
        // Android platforms directory default
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null) {
            androidDir = androidHome + File.separatorChar + "platforms";
        }

        // RT JAR default to system Java installation
        // First try to find Java 8 rt.jar in SDKMAN (most compatible with Soot)
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            // Try SDKMAN Java 8 first (most compatible)
            String sdkmanJava8 = userHome + File.separatorChar + ".sdkman" + File.separatorChar +
                    "candidates" + File.separatorChar + "java" + File.separatorChar +
                    "8.0.302-open" + File.separatorChar + "jre" + File.separatorChar +
                    "lib" + File.separatorChar + "rt.jar";

            if (new File(sdkmanJava8).exists()) {
                rtJar = sdkmanJava8;
            } else {
                // Try system Java (may not work well with newer versions)
                String javaHome = System.getProperty("java.home");
                if (javaHome != null) {
                    String systemRtJar = javaHome + File.separatorChar + "lib" + File.separatorChar + "rt.jar";
                    if (new File(systemRtJar).exists()) {
                        rtJar = systemRtJar;
                    } else {
                        // Warn about potential compatibility issues with newer Java
                        log.warn("No rt.jar found. Soot/FlowDroid may not work properly with Java 9+. " +
                                "Consider installing Java 8 via SDKMAN for better compatibility.");
                    }
                }
            }
        }
    }

    /**
     * Validate Android-specific parameters.
     */
    private void validateAndroidParameters() throws ParameterException {
        if (androidDir == null) {
            throw new ParameterException("Android platforms directory required for APK analysis. " +
                    "Use --android-dir parameter or set ANDROID_HOME environment variable.");
        }

        File androidDirFile = new File(androidDir);
        if (!androidDirFile.exists() || !androidDirFile.isDirectory()) {
            throw new ParameterException("Android platforms directory not found: " + androidDir);
        }

        if (rtJar == null) {
            throw new ParameterException("Runtime JAR path required for APK analysis. Use --rt-jar parameter.");
        }

        File rtJarFile = new File(rtJar);
        if (!rtJarFile.exists() || !rtJarFile.canRead()) {
            throw new ParameterException("Runtime JAR not found or not readable: " + rtJar);
        }
    }

    /**
     * Validate JAR-specific parameters.
     */
    private void validateJarParameters() throws ParameterException {
        // For reachability analysis, targets are required
        if (!extractOnly && targetsFile == null) {
            throw new ParameterException("Target methods file required for JAR reachability analysis. " +
                    "Use --targets parameter or --extract-only for basic extraction.");
        }
    }

    // Getters for legacy compatibility
    public String getInputPath() {
        return inputPath;
    }

    public String getTargetsFile() {
        return targetsFile;
    }

    public String getEntryPointsFile() {
        return entryPointsFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public boolean isExtractOnly() {
        return extractOnly;
    }

    public String getAnalysisScope() {
        return analysisScope;
    }

    public String getAndroidDir() {
        return androidDir;
    }

    public String getRtJar() {
        return rtJar;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isAppPackageOnly() {
        return appPackageOnly;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isHelp() {
        return help;
    }

    // Setters for testing and programmatic usage
    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public void setTargetsFile(String targetsFile) {
        this.targetsFile = targetsFile;
    }

    public void setEntryPointsFile(String entryPointsFile) {
        this.entryPointsFile = entryPointsFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public void setExtractOnly(boolean extractOnly) {
        this.extractOnly = extractOnly;
    }

    public void setAnalysisScope(String analysisScope) {
        this.analysisScope = analysisScope;
    }

    public void setAndroidDir(String androidDir) {
        this.androidDir = androidDir;
    }

    public void setRtJar(String rtJar) {
        this.rtJar = rtJar;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setAppPackageOnly(boolean appPackageOnly) {
        this.appPackageOnly = appPackageOnly;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    /**
     * Get entry point types as enum set for compatibility.
     */
    public Set<ComponentType> getEntryPointTypesSet() {
        return parseEntryPointTypes(entryPointTypes);
    }

    /**
     * Get writer type as enum for compatibility.
     */
    public WriterType getWriterTypeEnum() {
        return WriterType.fromString(writerType);
    }

    /**
     * Get analysis scope as enum for compatibility.
     */
    public AnalysisScope getAnalysisScopeEnum() {
        return AnalysisScope.fromString(analysisScope);
    }
}

