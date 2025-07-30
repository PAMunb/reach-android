package br.unb.cic.reach.common.model;

import java.util.Arrays;
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
 * 
 * This class centralizes all configuration parameters that determine analysis
 * behavior, algorithm selection, and output formatting. It replaces the previous
 * scattered configuration approach with a cohesive system that supports intelligent
 * validation, default selection, and strategy configuration.
 * 
 * ### Architecture Role:
 * The ConfigMatrix serves as the single source of truth for analysis configuration,
 * coordinating between command line parameters, algorithm selection, and output
 * formatting. It implements a 60+ dimensional configuration space that spans:
 * - Analysis scope (what methods to include in output)
 * - Package filtering (application vs system classes) 
 * - Entry point types (component types to analyze)
 * - Algorithm selection (performance vs precision trade-offs)
 * - Output configuration (formats and filtering options)
 * 
 * ### Configuration Matrix Dimensions:
 * The system supports analysis configurations through the combination of:
 * - AnalysisScope: 2 options (all-methods, reachable-only)
 * - Package filtering: 2 options (app-only, include-system)
 * - Entry point types: 15 combinations of Android components
 * - Reachability algorithms: 4 different strategies
 * - Performance priorities: 3 levels (speed, balanced, precision)
 * Total theoretical combinations: 2 × 2 × 15 × 4 × 3 = 720 configurations
 * 
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
    private final ReachabilityAlgorithm reachabilityAlgorithm;
    private final PerformancePriority performancePriority;
    private final CallGraphPrecision callGraphPrecision;
    
    // Output configuration
    private final WriterType writerType;
    private final boolean includeSystemClasses;
    private final boolean includePaths;
    private final boolean includeStatistics;
    
    // Performance tuning
    private final int timeoutSeconds;
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
    
    
    /**
     * Reachability algorithm enumeration defining computational strategies.
     * 
     * Each algorithm represents a different trade-off between computation time,
     * memory usage, and analysis precision. Algorithm selection is critical
     * for performance, especially with large applications.
     */
    public enum ReachabilityAlgorithm {
        SOOT_BFS("soot-bfs", "Direct BFS on Soot CallGraph (baseline implementation)"),
        OPTIMIZED_BFS("optimized-bfs", "Batch BFS with graph reversal (O(N+E) complexity)"),
        JGRAPHT_DIJKSTRA("jgrapht-dijkstra", "JGraphT-based shortest path algorithms"),
        PATH_SENSITIVE("path-sensitive", "Context and path-sensitive analysis");
        
        private final String value;
        private final String description;
        
        ReachabilityAlgorithm(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() { return value; }
        public String getDescription() { return description; }
        
        /**
         * Determines if this algorithm supports batch processing.
         * 
         * Batch processing algorithms can process multiple entry points and targets
         * simultaneously, providing significant performance improvements for complex
         * reachability queries. This information is used by the analysis engine
         * to select appropriate execution strategies.
         */
        public boolean supportsBatchProcessing() {
            return this == OPTIMIZED_BFS || this == JGRAPHT_DIJKSTRA;
        }
        
        /**
         * Returns expected time complexity for this algorithm.
         * 
         * This information helps the ConfigMatrix builder make intelligent decisions
         * about algorithm selection based on application size and timeout constraints.
         */
        public String getTimeComplexity() {
            switch (this) {
                case SOOT_BFS: return "O(N × M × C_path)";
                case OPTIMIZED_BFS: return "O(N + E)";  
                case JGRAPHT_DIJKSTRA: return "O((N + E) log N)";
                case PATH_SENSITIVE: return "O(N × P × C_path)";
                default: return "Unknown";
            }
        }
        
        public static ReachabilityAlgorithm fromString(String value) {
            return Arrays.stream(values())
                .filter(alg -> alg.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Invalid reachability algorithm: '" + value + "'. Valid options: " +
                    Arrays.stream(values()).map(a -> a.value).collect(Collectors.joining(", "))));
        }
    }
    
    /**
     * Performance priority enumeration for algorithm selection guidance.
     * 
     * This dimension influences algorithm selection, timeout values, and memory
     * allocation based on the user's primary optimization objective.
     */
    public enum PerformancePriority {
        SPEED("speed", "Optimize for fastest execution time"),
        BALANCED("balanced", "Balance execution time and analysis precision"),
        PRECISION("precision", "Optimize for highest analysis precision");
        
        private final String value;
        private final String description;
        
        PerformancePriority(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() { return value; }
        public String getDescription() { return description; }
        
        public static PerformancePriority fromString(String value) {
            return Arrays.stream(values())
                .filter(priority -> priority.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(BALANCED); // Safe default
        }
    }
    
    /**
     * Call graph precision enumeration affecting analysis accuracy.
     * 
     * Higher precision levels provide more accurate call graphs at the cost
     * of increased computation time and memory usage.
     */
    public enum CallGraphPrecision {
        LOW("low", "Context-insensitive analysis (fastest)"),
        MEDIUM("medium", "Context-sensitive analysis"),
        HIGH("high", "Context and flow-sensitive analysis");
        
        private final String value;
        private final String description;
        
        CallGraphPrecision(String value, String description) {
            this.value = value;
            this.description = description;
        }
        
        public String getValue() { return value; }
        public String getDescription() { return description; }
    }
    
    
    /**
     * Private constructor enforcing builder pattern usage.
     * 
     * The ConfigMatrix complexity requires careful validation and intelligent
     * default selection that is best handled through the builder pattern.
     */
    private ConfigMatrix(Builder builder) {
        this.analysisScope = builder.analysisScope;
        this.appPackageOnly = builder.appPackageOnly;
        this.entryPointTypes = Set.copyOf(builder.entryPointTypes);
        this.extractOnly = builder.extractOnly;
        this.reachabilityAlgorithm = builder.reachabilityAlgorithm;
        this.performancePriority = builder.performancePriority;
        this.callGraphPrecision = builder.callGraphPrecision;
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
    }
    
    /**
     * Builder class for fluent ConfigMatrix construction with intelligent defaults.
     * 
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
        private ReachabilityAlgorithm reachabilityAlgorithm = ReachabilityAlgorithm.OPTIMIZED_BFS;
        private PerformancePriority performancePriority = PerformancePriority.BALANCED;
        private CallGraphPrecision callGraphPrecision = CallGraphPrecision.MEDIUM;
        private WriterType writerType = WriterType.CSV;
        private boolean includeSystemClasses = false;
        private boolean includePaths = true;
        private boolean includeStatistics = true;
        private int timeoutSeconds = 300;
        private int maxMemoryMB = 4096;
        private boolean enableParallelization = false;
        
        // Application-specific defaults
        private String inputPath;
        private String targetsFile;
        private String entryPointsFile;
        private String outputFile;
        
        // Android-specific defaults
        private String androidPlatformsDir;
        private String rtJarPath;
        
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
        
        public Builder withEntryPointTypes(String typesString) {
            this.entryPointTypes = parseEntryPointTypes(typesString);
            return this;
        }
        
        public Builder withExtractOnly(boolean extractOnly) {
            this.extractOnly = extractOnly;
            return this;
        }
        
        public Builder withReachabilityAlgorithm(ReachabilityAlgorithm algorithm) {
            this.reachabilityAlgorithm = algorithm;
            return this;
        }
        
        public Builder withPerformancePriority(PerformancePriority priority) {
            this.performancePriority = priority;
            return this;
        }
        
        public Builder withCallGraphPrecision(CallGraphPrecision precision) {
            this.callGraphPrecision = precision;
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
        
        /**
         * Apply intelligent defaults based on application characteristics.
         * 
         * This method analyzes application metrics like method count, class count,
         * and complexity indicators to select optimal algorithm and performance
         * configurations. This adaptive approach improves performance for large
         * applications while maintaining precision for smaller ones.
         */
        public Builder adaptToApplication(AppInfo appInfo) {
            if (appInfo == null) return this;
            
            // Estimate application complexity based on available metrics
            int totalMethods = appInfo.getClasses().stream()
                .mapToInt(clazz -> clazz.getMethods().size())
                .sum();
            int totalClasses = appInfo.getClasses().size();
            
            // For large applications, prioritize performance
            if (totalMethods > 50000 || totalClasses > 5000) {
                log.info("Large application detected ({} methods, {} classes). Optimizing for performance.",
                        totalMethods, totalClasses);
                this.reachabilityAlgorithm = ReachabilityAlgorithm.OPTIMIZED_BFS;
                this.performancePriority = PerformancePriority.SPEED;
                this.callGraphPrecision = CallGraphPrecision.LOW;
                this.timeoutSeconds = Math.max(timeoutSeconds, 600); // Increase timeout
                this.enableParallelization = true;
            } 
            // For small applications, can afford higher precision
            else if (totalMethods < 5000 && totalClasses < 500) {
                log.info("Small application detected ({} methods, {} classes). Optimizing for precision.",
                        totalMethods, totalClasses);
                this.performancePriority = PerformancePriority.PRECISION;
                this.callGraphPrecision = CallGraphPrecision.HIGH;
                this.includePaths = true;
            }
            // Medium applications use balanced approach (default)
            else {
                log.info("Medium application detected ({} methods, {} classes). Using balanced configuration.",
                        totalMethods, totalClasses);
            }
            
            return this;
        }
        
        /**
         * Build ConfigMatrix with comprehensive validation.
         * 
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
            
            // Algorithm and performance priority consistency
            if (performancePriority == PerformancePriority.SPEED && 
                reachabilityAlgorithm == ReachabilityAlgorithm.PATH_SENSITIVE) {
                warnings.add("Path-sensitive algorithm conflicts with speed priority. " +
                           "Consider using optimized-bfs algorithm for faster execution.");
            }
            
            if (performancePriority == PerformancePriority.PRECISION && 
                reachabilityAlgorithm == ReachabilityAlgorithm.SOOT_BFS) {
                warnings.add("Basic BFS algorithm may not achieve optimal precision. " +
                           "Consider using path-sensitive or jgrapht-dijkstra algorithms.");
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
        
        /**
         * Parse entry point types from comma-separated string.
         * 
         * This method handles the complexity of parsing various entry point
         * specifications, including the special "all" keyword and validation
         * of individual component types.
         */
        private Set<ComponentType> parseEntryPointTypes(String typesString) {
            if (typesString == null || typesString.trim().isEmpty()) {
                return EnumSet.allOf(ComponentType.class);
            }
            
            String trimmed = typesString.trim().toLowerCase();
            if ("all".equals(trimmed)) {
                return EnumSet.allOf(ComponentType.class);
            }
            
            Set<ComponentType> types = EnumSet.noneOf(ComponentType.class);
            for (String type : trimmed.split(",")) {
                ComponentType componentType = parseComponentType(type.trim());
                types.add(componentType);
            }
            
            return types;
        }
        
        /**
         * Parse individual component type with comprehensive error handling.
         */
        private ComponentType parseComponentType(String type) {
            switch (type.toLowerCase()) {
                case "activities": return ComponentType.ACTIVITY;
                case "services": return ComponentType.SERVICE;
                case "receivers": return ComponentType.RECEIVER;
                case "providers": return ComponentType.PROVIDER;
                default:
                    throw new IllegalArgumentException("Invalid component type: '" + type + "'. " +
                        "Valid types: activities, services, receivers, providers, all");
            }
        }
    }
    
    /**
     * Generate comprehensive configuration summary for logging and debugging.
     * 
     * This method provides detailed information about the configuration matrix
     * state, including all selected options and their implications. The output
     * is formatted for both human readability and structured logging.
     */
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Configuration Matrix Summary ===\n");
        summary.append("Analysis Scope: ").append(analysisScope.getValue())
               .append(" (").append(analysisScope.getDescription()).append(")\n");
        summary.append("Package Filtering: ").append(appPackageOnly ? "app-only" : "include-system").append("\n");
        summary.append("Entry Point Types: ").append(entryPointTypes.stream()
            .map(type -> type.name().toLowerCase())
            .collect(Collectors.joining(", "))).append("\n");
        summary.append("Extract Only: ").append(extractOnly).append("\n");
        summary.append("Reachability Algorithm: ").append(reachabilityAlgorithm.getValue())
               .append(" (").append(reachabilityAlgorithm.getTimeComplexity()).append(")\n");
        summary.append("Performance Priority: ").append(performancePriority.getValue()).append("\n");
        summary.append("Call Graph Precision: ").append(callGraphPrecision.getValue()).append("\n");
        summary.append("Output Format: ").append(writerType.getExtension()).append("\n");
        summary.append("Timeout: ").append(timeoutSeconds).append("s\n");
        summary.append("Memory Limit: ").append(maxMemoryMB).append("MB\n");
        summary.append("Parallelization: ").append(enableParallelization ? "enabled" : "disabled").append("\n");
        summary.append("=====================================");
        return summary.toString();
    }
    
    /**
     * Calculate estimated configuration matrix position.
     * 
     * This method computes a hash representing the current configuration's
     * position within the theoretical configuration space, useful for
     * reproducibility and configuration management.
     */
    public String getMatrixPosition() {
        return String.format("scope=%s,package=%s,types=%s,algorithm=%s,priority=%s",
            analysisScope.getValue(),
            appPackageOnly ? "app" : "all",
            entryPointTypes.size() == ComponentType.values().length ? "all" : 
                entryPointTypes.stream().map(t -> t.name().toLowerCase()).collect(Collectors.joining(",")),
            reachabilityAlgorithm.getValue(),
            performancePriority.getValue());
    }
    
    // Getters for all configuration parameters
    public AnalysisScope getAnalysisScope() { return analysisScope; }
    public boolean isAppPackageOnly() { return appPackageOnly; }
    public Set<ComponentType> getEntryPointTypes() { return entryPointTypes; }
    public boolean isExtractOnly() { return extractOnly; }
    public ReachabilityAlgorithm getReachabilityAlgorithm() { return reachabilityAlgorithm; }
    public PerformancePriority getPerformancePriority() { return performancePriority; }
    public CallGraphPrecision getCallGraphPrecision() { return callGraphPrecision; }
    public WriterType getWriterType() { return writerType; }
    public boolean isIncludeSystemClasses() { return includeSystemClasses; }
    public boolean isIncludePaths() { return includePaths; }
    public boolean isIncludeStatistics() { return includeStatistics; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public int getMaxMemoryMB() { return maxMemoryMB; }
    public boolean isEnableParallelization() { return enableParallelization; }
    
    // Application-specific getters
    public String getInputPath() { return inputPath; }
    public String getTargetsFile() { return targetsFile; }
    public String getEntryPointsFile() { return entryPointsFile; }
    public String getOutputFile() { return outputFile; }
    
    // Android-specific getters
    public String getAndroidPlatformsDir() { return androidPlatformsDir; }
    public String getRtJarPath() { return rtJarPath; }
}

