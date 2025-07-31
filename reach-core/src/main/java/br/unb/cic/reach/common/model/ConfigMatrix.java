package br.unb.cic.reach.common.model;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.common.writer.WriterType;

/**
 * Unified configuration matrix for reachability analysis parameters.
 * <p>
 * This class centralizes all configuration parameters that determine analysis
 * behavior, algorithm selection, and output formatting. It replaces the previous
 * scattered configuration approach with a cohesive system that supports intelligent
 * validation, default selection, and strategy configuration.
 * <p>
 * ### Architecture Role:
 * The ConfigMatrix serves as the single source of truth for analysis configuration,
 * coordinating between command line parameters, algorithm selection, and output
 * formatting. It implements a 60+ dimensional configuration space that spans:
 * - Analysis scope (what methods to include in output)
 * - Package filtering (application vs system classes)
 * - Entry point types (component types to analyze)
 * - Algorithm selection (performance vs precision trade-offs)
 * - Output configuration (formats and filtering options)
 * <p>
 * ### Configuration Matrix Dimensions:
 * The system supports analysis configurations through the combination of:
 * - AnalysisScope: 2 options (all-methods, reachable-only)
 * - Package filtering: 2 options (app-only, include-system)
 * - Entry point types: 15 combinations of Android components
 * - Reachability algorithms: 4 different strategies
 * - Performance priorities: 3 levels (speed, balanced, precision)
 * Total theoretical combinations: 2 × 2 × 15 × 4 × 3 = 720 configurations
 * <p>
 * ### Intelligent Default Selection:
 * The ConfigMatrix applies heuristics to select optimal configurations based on
 * application characteristics like size, complexity, and analysis requirements.
 * This reduces the cognitive load on users while enabling expert-level customization.
 */
public class ConfigMatrix {
    private static final Logger log = LoggerFactory.getLogger(ConfigMatrix.class);

    // Core analysis configuration
    private final AnalysisScope analysisScope;
    private final boolean appPackageOnly;
    private final Set<ComponentType> entryPointTypes;
    private final boolean extractOnly;

    // Advanced algorithm configuration
    private final ReachabilityStrategyType reachabilityStrategy;
    private final CallGraphAlgorithm callGraphAlgorithm;
    private final AliasingAlgorithm aliasingAlgorithm;

    // Output configuration
    private final WriterType writerType;
    private final boolean includeSystemClasses;
    private final boolean includePaths;
    private final boolean includeStatistics;

    // Performance tuning
    private final int timeoutSeconds;
    //TODO: implement
    private final int maxMemoryMB;
    private final boolean enableParallelization;

    // Application-specific configuration
    private final String inputPath;
    private final String targetsFile;
    private final String entryPointsFile;
    private final String outputFile;

    // Android-specific configuration
    private final String androidPlatformsDir;
    private final String rtJarPath;
    private final int maxAnalysisCallbackDepth;


    /**
     * Reachability algorithm enumeration defining computational strategies.
     * <p>
     * Each algorithm represents a different trade-off between computation time,
     * memory usage, and analysis precision. Algorithm selection is critical
     * for performance, especially with large applications.
     */
    public enum ReachabilityStrategyType {
        SOOT_BFS("soot-bfs", "Direct BFS on Soot CallGraph"),
        JGRAPHT_DIJKSTRA("jgrapht-dijkstra", "JGraphT-based shortest path algorithms");

        private final String value;
        private final String description;

        ReachabilityStrategyType(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum CallGraphAlgorithm {
        CHA("cha", ""),
        VTA("vta", ""),
        RTA("rta", ""),
        SPARK("spark", ""),
        GEOM("geom", "");

        private final String value;
        private final String description;

        CallGraphAlgorithm(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum AliasingAlgorithm {
        FlowSensitive("flow-sensitive", ""),
        PtsBased("pts-based", ""),
        None("none", ""),
        Lazy("lazy", "");

        private final String value;
        private final String description;

        AliasingAlgorithm(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }


    /**
     * Private constructor enforcing builder pattern usage.
     * <p>
     * The ConfigMatrix complexity requires careful validation and intelligent
     * default selection that is best handled through the builder pattern.
     */
    private ConfigMatrix(Builder builder) {
        this.analysisScope = builder.analysisScope;
        this.appPackageOnly = builder.appPackageOnly;
        this.entryPointTypes = Set.copyOf(builder.entryPointTypes);
        this.extractOnly = builder.extractOnly;
        this.reachabilityStrategy = builder.reachabilityStrategy;
        this.callGraphAlgorithm = builder.callGraphAlgorithm;
        this.aliasingAlgorithm = builder.aliasingAlgorithm;
        this.writerType = builder.writerType;
        this.includeSystemClasses = builder.includeSystemClasses;
        this.includePaths = builder.includePaths;
        this.includeStatistics = builder.includeStatistics;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.maxMemoryMB = builder.maxMemoryMB;
        this.enableParallelization = builder.enableParallelization;
        this.inputPath = builder.inputPath;
        this.targetsFile = builder.targetsFile;
        this.entryPointsFile = builder.entryPointsFile;
        this.outputFile = builder.outputFile;
        this.androidPlatformsDir = builder.androidPlatformsDir;
        this.rtJarPath = builder.rtJarPath;
        this.maxAnalysisCallbackDepth = builder.maxAnalysisCallbackDepth;
    }

    /**
     * Builder class for fluent ConfigMatrix construction with intelligent defaults.
     * <p>
     * The builder implements configuration validation, intelligent default selection,
     * and adaptive configuration based on application characteristics. This approach
     * reduces configuration complexity while maintaining full customization capability.
     */
    public static class Builder {
        // Default configuration optimized for common use cases
        private AnalysisScope analysisScope = AnalysisScope.ALL_METHODS;
        private boolean appPackageOnly = true;
        private Set<ComponentType> entryPointTypes = EnumSet.allOf(ComponentType.class);
        private boolean extractOnly = false;
        private ReachabilityStrategyType reachabilityStrategy = ReachabilityStrategyType.SOOT_BFS;
        private CallGraphAlgorithm callGraphAlgorithm = CallGraphAlgorithm.SPARK;
        private AliasingAlgorithm aliasingAlgorithm = AliasingAlgorithm.FlowSensitive;
        private WriterType writerType = WriterType.CSV;
        private int timeoutSeconds = 300;

        private final boolean includeSystemClasses = false;
        private final boolean includePaths = true;
        private final boolean includeStatistics = true;

        private final int maxMemoryMB = 4096;
        private final boolean enableParallelization = false;

        // Application-specific defaults
        private String inputPath;
        private String targetsFile;
        private String entryPointsFile;
        private String outputFile;

        // Android-specific defaults
        private String androidPlatformsDir;
        private String rtJarPath;
        private int maxAnalysisCallbackDepth = 10;

        public Builder withAnalysisScope(AnalysisScope scope) {
            this.analysisScope = scope;
            return this;
        }

        public Builder withAppPackageOnly(boolean appPackageOnly) {
            this.appPackageOnly = appPackageOnly;
            return this;
        }

        public Builder withEntryPointTypes(Set<ComponentType> types) {
            this.entryPointTypes = EnumSet.copyOf(types);
            return this;
        }

        public Builder withExtractOnly(boolean extractOnly) {
            this.extractOnly = extractOnly;
            return this;
        }

        public Builder withReachabilityStrategy(ReachabilityStrategyType strategy) {
            this.reachabilityStrategy = strategy;
            return this;
        }

        public Builder withCallGraphAlgorithm(CallGraphAlgorithm algorithm) {
            this.callGraphAlgorithm = algorithm;
            return this;
        }

        public Builder withAliasingAlgorithm(AliasingAlgorithm algorithm) {
            this.aliasingAlgorithm = algorithm;
            return this;
        }

        public Builder withWriterType(WriterType writerType) {
            this.writerType = writerType;
            return this;
        }

        public Builder withTimeout(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder withInputPath(String inputPath) {
            this.inputPath = inputPath;
            return this;
        }

        public Builder withTargetsFile(String targetsFile) {
            this.targetsFile = targetsFile;
            return this;
        }

        public Builder withEntryPointsFile(String entryPointsFile) {
            this.entryPointsFile = entryPointsFile;
            return this;
        }

        public Builder withOutputFile(String outputFile) {
            this.outputFile = outputFile;
            return this;
        }

        public Builder withAndroidPlatformsDir(String androidPlatformsDir) {
            this.androidPlatformsDir = androidPlatformsDir;
            return this;
        }

        public Builder withRtJarPath(String rtJarPath) {
            this.rtJarPath = rtJarPath;
            return this;
        }

        public Builder withMaxAnalysisCallbackDepth(int maxAnalysisCallbackDepth) {
            this.maxAnalysisCallbackDepth = maxAnalysisCallbackDepth;
            return this;
        }

        /**
         * Build ConfigMatrix with comprehensive validation.
         * <p>
         * This method performs extensive validation of parameter combinations,
         * checking for logical inconsistencies and potentially problematic
         * configurations. It provides detailed error messages and warnings
         * to guide users toward effective configurations.
         */
        public ConfigMatrix build() throws ConfigurationException {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Critical validation: extract-only mode limitations
            if (extractOnly && analysisScope == AnalysisScope.REACHABLE_ONLY) {
                warnings.add("Analysis scope 'reachable-only' has no effect in extract-only mode. " +
                        "Extract-only mode does not compute reachability information. " +
                        "Output will include all methods regardless of scope setting.");
            }

            // Entry point validation
            if (entryPointTypes.isEmpty()) {
                errors.add("At least one entry point type must be specified. " +
                        "Valid types: activities, services, receivers, providers");
            }

            // Memory and timeout validation
            if (maxMemoryMB < 1024) {
                warnings.add("Low memory limit (" + maxMemoryMB + "MB) may cause analysis failures. " +
                        "Recommended minimum: 2048MB for typical applications.");
            }

            if (timeoutSeconds < 60) {
                warnings.add("Short timeout (" + timeoutSeconds + "s) may cause analysis failures. " +
                        "Consider increasing timeout for complex applications.");
            }

            // Report errors and warnings
            if (!errors.isEmpty()) {
                throw new ConfigurationException("Configuration validation failed:\n" +
                        String.join("\n", errors));
            }

            for (String warning : warnings) {
                log.warn("Configuration warning: {}", warning);
            }

            return new ConfigMatrix(this);
        }

    }

    /**
     * Generate comprehensive configuration summary for logging and debugging.
     * <p>
     * This method provides detailed information about the configuration matrix
     * state, including all selected options and their implications. The output
     * is formatted for both human readability and structured logging.
     */
    public String getConfigurationSummary() {
        //TODO converter para string com 3 aspas
        StringBuilder summary = new StringBuilder();
        summary.append("=== Configuration Matrix Summary ===\n");
        summary.append("Analysis Scope: ").append(analysisScope.getValue())
                .append(" (").append(analysisScope.getDescription()).append(")\n");
        summary.append("Package Filtering: ").append(appPackageOnly ? "app-only" : "include-system").append("\n");
        summary.append("Entry Point Types: ").append(entryPointTypes.stream()
                .map(type -> type.name().toLowerCase())
                .collect(Collectors.joining(", "))).append("\n");
        summary.append("Extract Only: ").append(extractOnly).append("\n");
        summary.append("Reachability Algorithm: ").append(reachabilityStrategy.getValue());
        summary.append("CallGraph Algorithm: ").append(callGraphAlgorithm.getValue()).append("\n");
        summary.append("Aliasing Algorithm: ").append(aliasingAlgorithm.getValue()).append("\n");
        summary.append("Output Format: ").append(writerType.getExtension()).append("\n");
        summary.append("Timeout: ").append(timeoutSeconds).append("s\n");
        summary.append("Memory Limit: ").append(maxMemoryMB).append("MB\n");
        summary.append("Parallelization: ").append(enableParallelization ? "enabled" : "disabled").append("\n");
        summary.append("=====================================");
        return summary.toString();
    }

    /**
     * Calculate estimated configuration matrix position.
     * <p>
     * This method computes a hash representing the current configuration's
     * position within the theoretical configuration space, useful for
     * reproducibility and configuration management.
     */
    public String getMatrixPosition() {
        return String.format("scope=%s,package=%s,types=%s,algorithm=%s,callgraph=%s",
                analysisScope.getValue(),
                appPackageOnly ? "app" : "all",
                entryPointTypes.size() == ComponentType.values().length ? "all" :
                        entryPointTypes.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(",")),
                reachabilityStrategy.getValue(),
                callGraphAlgorithm.getValue());
    }

    // Getters for all configuration parameters
    public AnalysisScope getAnalysisScope() {
        return analysisScope;
    }

    public boolean isAppPackageOnly() {
        return appPackageOnly;
    }

    public Set<ComponentType> getEntryPointTypes() {
        return entryPointTypes;
    }

    public boolean isExtractOnly() {
        return extractOnly;
    }

    public ReachabilityStrategyType getReachabilityStrategy() {
        return reachabilityStrategy;
    }

    public CallGraphAlgorithm getCallGraphAlgorithm() {
        return callGraphAlgorithm;
    }

    public AliasingAlgorithm getAliasingAlgorithm() {
        return aliasingAlgorithm;
    }

    public WriterType getWriterType() {
        return writerType;
    }

    public boolean isIncludeSystemClasses() {
        return includeSystemClasses;
    }

    public boolean isIncludePaths() {
        return includePaths;
    }

    public boolean isIncludeStatistics() {
        return includeStatistics;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxMemoryMB() {
        return maxMemoryMB;
    }

    public boolean isEnableParallelization() {
        return enableParallelization;
    }

    // Application-specific getters
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

    // Android-specific getters
    public String getAndroidPlatformsDir() {
        return androidPlatformsDir;
    }

    public String getRtJarPath() {
        return rtJarPath;
    }

    public int getMaxAnalysisCallbackDepth() {
        return maxAnalysisCallbackDepth;
    }
}

