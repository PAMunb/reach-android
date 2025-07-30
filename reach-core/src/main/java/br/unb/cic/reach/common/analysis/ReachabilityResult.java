package br.unb.cic.reach.common.analysis;

import java.util.Map;

import br.unb.cic.reach.common.model.AnalysisScope;
import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ConfigMatrix;
import soot.SootMethod;

/**
 * Container for complete reachability analysis results with ConfigMatrix integration.
 * 
 * This class encapsulates the comprehensive results of reachability analysis,
 * including the populated AppInfo with analysis results, detailed reachability
 * information for individual methods, and ConfigMatrix metadata for unified
 * configuration management and output filtering.
 * 
 * ### Architectural Integration:
 * - Unified result container for optimized analysis outcomes
 * - AppInfo integration for result organization and output
 * - Detailed reachability mapping for analysis introspection  
 * - ConfigMatrix integration for centralized configuration access
 * 
 * ### ConfigMatrix Pipeline:
 * The ConfigMatrix travels through: CLI → Main → ReachabilityAnalysis → ReachabilityResult → Writers
 * This ensures consistent access to all configuration parameters including analysis scope,
 * algorithm selection, performance priorities, and output formatting preferences.
 */
public class ReachabilityResult {
    private AppInfo appInfo;
    private Map<SootMethod, ReachabilityInfo> reachabilityMap;
    private long executionTime;
    private int entryPointCount;
    private int targetMethodCount;

    /**
     * Unified configuration matrix containing all analysis parameters.
     * 
     * This field provides writers and other consumers access to the complete
     * configuration state including analysis scope, algorithm selection,
     * performance priorities, and output formatting preferences.
     * The ConfigMatrix centralizes what were previously scattered parameters
     * across multiple classes and configuration points.
     */
    private ConfigMatrix configMatrix;

    public ReachabilityResult(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    public ReachabilityResult(AppInfo appInfo, Map<SootMethod, ReachabilityInfo> reachabilityMap) {
        this.appInfo = appInfo;
        this.reachabilityMap = reachabilityMap;
    }

    public ReachabilityResult(AppInfo appInfo, Map<SootMethod, ReachabilityInfo> reachabilityMap, long executionTime) {
        this.appInfo = appInfo;
        this.reachabilityMap = reachabilityMap;
        this.executionTime = executionTime;
    }

    // Getters and setters
    public AppInfo getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    public Map<SootMethod, ReachabilityInfo> getReachabilityMap() {
        return reachabilityMap;
    }

    public void setReachabilityMap(Map<SootMethod, ReachabilityInfo> reachabilityMap) {
        this.reachabilityMap = reachabilityMap;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public int getEntryPointCount() {
        return entryPointCount;
    }

    public void setEntryPointCount(int entryPointCount) {
        this.entryPointCount = entryPointCount;
    }

    public int getTargetMethodCount() {
        return targetMethodCount;
    }

    public void setTargetMethodCount(int targetMethodCount) {
        this.targetMethodCount = targetMethodCount;
    }

    /**
     * Gets the complete configuration matrix.
     *
     * @return ConfigMatrix containing all analysis configuration parameters
     */
    public ConfigMatrix getConfigMatrix() {
        return configMatrix;
    }

    /**
     * Sets the configuration matrix for result processing.
     * 
     * This method is called by the analysis engine to pass the complete
     * configuration state through to writers and other result consumers,
     * enabling consistent parameter access throughout the processing pipeline.
     *
     * @param configMatrix The configuration matrix from the analysis
     */
    public void setConfigMatrix(ConfigMatrix configMatrix) {
        this.configMatrix = configMatrix;
    }

    /**
     * Gets the analysis scope configuration for output filtering.
     * 
     * Convenience method that extracts the analysis scope from the ConfigMatrix.
     * This maintains compatibility with existing code while leveraging the
     * unified configuration system.
     *
     * @return AnalysisScope enum indicating what methods should be included in output
     */
    public AnalysisScope getAnalysisScope() {
        return configMatrix != null ? configMatrix.getAnalysisScope() : AnalysisScope.ALL_METHODS;
    }

    /**
     * Sets the analysis scope configuration for output filtering.
     * 
     * Convenience method for compatibility. For new code, prefer setting
     * the complete ConfigMatrix which includes the analysis scope along
     * with all other configuration parameters.
     *
     * @param analysisScope The scope configuration from command line arguments
     */
    public void setAnalysisScope(AnalysisScope analysisScope) {
        if (configMatrix != null) {
            // If ConfigMatrix exists, we should update it rather than override
            // For now, just log a warning since this indicates mixed usage patterns
            System.err.println("Warning: Setting analysis scope directly when ConfigMatrix is present. " +
                             "Consider using ConfigMatrix for unified configuration.");
        }
        // For compatibility, create a minimal ConfigMatrix if one doesn't exist
        if (configMatrix == null) {
            try {
                configMatrix = new ConfigMatrix.Builder()
                    .withAnalysisScope(analysisScope)
                    .build();
            } catch (Exception e) {
                System.err.println("Failed to create ConfigMatrix for scope setting: " + e.getMessage());
            }
        }
    }

    /**
     * Returns count of reachable methods in the analysis.
     * 
     * This count reflects methods that are reachable from entry points,
     * which is only meaningful in full analysis mode. In extract-only mode,
     * this will return 0 since reachability is not computed.
     */
    public int getReachableMethodCount() {
        if (reachabilityMap == null) {
            return 0;
        }
        return (int) reachabilityMap.values().stream()
                .filter(ReachabilityInfo::isReachable)
                .count();
    }

    /**
     * Returns count of methods that can reach target methods.
     * 
     * This count reflects methods that can reach targets, which is only
     * meaningful in full analysis mode. In extract-only mode, this will return 0
     * since target reachability is not computed through call graph traversal.
     */
    public int getTargetReachingMethodCount() {
        if (reachabilityMap == null) {
            return 0;
        }
        return (int) reachabilityMap.values().stream()
                .filter(ReachabilityInfo::isReachesTarget)
                .count();
    }

    /**
     * Returns a summary of analysis configuration and results.
     * 
     * This method provides a comprehensive overview of the analysis execution
     * including configuration parameters, timing information, and key metrics.
     */
    public String getAnalysisSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Reachability Analysis Results ===\n");
        
        if (configMatrix != null) {
            summary.append("Configuration: ").append(configMatrix.getMatrixPosition()).append("\n");
            summary.append("Analysis Scope: ").append(configMatrix.getAnalysisScope().getValue()).append("\n");
            summary.append("Algorithm: ").append(configMatrix.getReachabilityAlgorithm().getValue()).append("\n");
        }
        
        summary.append("Execution Time: ").append(executionTime).append("ms\n");
        summary.append("Entry Points: ").append(entryPointCount).append("\n");
        summary.append("Target Methods: ").append(targetMethodCount).append("\n");
        summary.append("Reachable Methods: ").append(getReachableMethodCount()).append("\n");
        summary.append("Target-Reaching Methods: ").append(getTargetReachingMethodCount()).append("\n");
        
        if (appInfo != null) {
            int totalMethods = appInfo.getClasses().stream()
                .mapToInt(clazz -> clazz.getMethods().size())
                .sum();
            summary.append("Total Methods: ").append(totalMethods).append("\n");
        }
        
        summary.append("=====================================");
        return summary.toString();
    }
}