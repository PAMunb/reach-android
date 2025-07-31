package br.unb.cic.reach.common.writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.model.AnalysisScope;
import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ComponentType;
import br.unb.cic.reach.common.model.ConfigMatrix;
import br.unb.cic.reach.common.model.Path;
import br.unb.cic.reach.common.model.ReachClass;
import br.unb.cic.reach.common.model.ReachMethod;

/**
 * Optimized JSON writer with ConfigMatrix integration and hierarchical filtering.
 * <p>
 * This writer produces structured JSON output with configurable method filtering
 * while maintaining hierarchical organization by classes and components. It includes
 * comprehensive metadata, analysis configuration, and statistical information
 * for detailed result analysis and integration with external tools.
 * <p>
 * ### JSON Structure:
 * - app_info: Application metadata and component statistics
 * - analysis_config: Complete configuration matrix information
 * - results: Hierarchical method data with scope-based filtering
 * - statistics: Comprehensive filtering and analysis metrics
 * <p>
 * ### Filtering Optimization:
 * - Empty classes (after filtering) are excluded to keep output clean
 * - Method filtering applied during JSON construction for efficiency
 * - Statistics computed during filtering pass for performance
 */
public class JsonWriter implements Writer {
    private static final Logger log = LoggerFactory.getLogger(JsonWriter.class);
    
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();

    @Override
    public void write(ReachabilityResult result, File outputFile) throws IOException {
        validateParameters(result, outputFile);

        AppInfo appInfo = result.getAppInfo();
        AnalysisScope scope = result.getAnalysisScope();
        
        log.info("Writing JSON results to: {} with scope: {}", 
                outputFile.getAbsolutePath(), scope.getValue());

        // Build complete JSON structure with filtering
        JsonObject root = buildJsonStructure(result, appInfo, scope);

        try (FileWriter writer = new FileWriter(outputFile, StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        }

        logJsonStatistics(root, scope, outputFile);
    }

    /**
     * Build complete JSON structure with ConfigMatrix integration.
     * <p>
     * This method constructs the full JSON document including application metadata,
     * configuration information, filtered results, and comprehensive statistics.
     * The structure provides complete traceability of analysis parameters and results.
     */
    private JsonObject buildJsonStructure(ReachabilityResult result, AppInfo appInfo, 
                                        AnalysisScope scope) {
        JsonObject root = new JsonObject();

        // Application information section
        root.add("app_info", createAppInfoJson(appInfo));

        // Analysis configuration section with ConfigMatrix details
        root.add("analysis_config", createAnalysisConfigJson(result));

        // Results section with scope-based filtering
        root.add("results", createFilteredResultsJson(appInfo, scope));

        // Statistics section with filtering metrics
        root.add("statistics", createStatisticsJson(appInfo, scope));

        return root;
    }

    /**
     * Create application information JSON with component statistics.
     */
    private JsonObject createAppInfoJson(AppInfo appInfo) {
        JsonObject appInfoJson = new JsonObject();
        appInfoJson.addProperty("path", appInfo.getPath());
        appInfoJson.addProperty("package", appInfo.getPackageName());
        appInfoJson.addProperty("name", appInfo.getAppName());
        appInfoJson.addProperty("label", appInfo.getLabel());
        appInfoJson.addProperty("type", appInfo.getType().toString());

        // Component type statistics for analysis insight
        JsonObject componentCounts = new JsonObject();
        for (ComponentType type : ComponentType.values()) {
            long count = appInfo.getClasses().stream()
                .filter(clazz -> clazz.getComponentType() == type)
                .count();
            componentCounts.addProperty(type.toString().toLowerCase(), count);
        }
        appInfoJson.add("component_counts", componentCounts);

        return appInfoJson;
    }

    /**
     * Create analysis configuration JSON with complete ConfigMatrix details.
     * <p>
     * This section provides complete traceability of analysis parameters,
     * enabling reproducible results and parameter sensitivity analysis.
     */
    private JsonObject createAnalysisConfigJson(ReachabilityResult result) {
        JsonObject configJson = new JsonObject();

        // Basic timing and scope information
        configJson.addProperty("analysis_scope", result.getAnalysisScope().getValue());
        configJson.addProperty("execution_time_ms", result.getExecutionTime());

        // ConfigMatrix integration - comprehensive parameter capture
        ConfigMatrix configMatrix = result.getConfigMatrix();
        if (configMatrix != null) {
            configJson.addProperty("app_package_only", configMatrix.isAppPackageOnly());
            configJson.addProperty("extract_only", configMatrix.isExtractOnly());
            configJson.addProperty("timeout_seconds", configMatrix.getTimeoutSeconds());
            configJson.addProperty("max_memory_mb", configMatrix.getMaxMemoryMB());
            configJson.addProperty("parallelization_enabled", configMatrix.isEnableParallelization());

            // Entry point types configuration
            JsonArray entryPointTypes = new JsonArray();
            configMatrix.getEntryPointTypes().forEach(type -> 
                entryPointTypes.add(type.toString().toLowerCase()));
            configJson.add("entry_point_types", entryPointTypes);

            // Algorithm and performance configuration
            configJson.addProperty("reachability_algorithm", 
                configMatrix.getReachabilityStrategy().getValue());
            configJson.addProperty("callgraph_algorithm",
                configMatrix.getCallGraphAlgorithm().getValue());
            configJson.addProperty("aliasing_algorithm",
                configMatrix.getAliasingAlgorithm().getValue());

            // Output configuration
            configJson.addProperty("writer_type", configMatrix.getWriterType().getExtension());
            configJson.addProperty("include_system_classes", configMatrix.isIncludeSystemClasses());
            configJson.addProperty("include_paths", configMatrix.isIncludePaths());
            configJson.addProperty("include_statistics", configMatrix.isIncludeStatistics());

            // Configuration matrix position for reproducibility
            configJson.addProperty("matrix_position", configMatrix.getMatrixPosition());
        }

        return configJson;
    }

    /**
     * Create filtered results JSON with hierarchical structure.
     * <p>
     * This method applies analysis scope filtering while maintaining the
     * hierarchical class-method organization. Empty classes (after filtering)
     * are excluded to keep the output focused and clean.
     */
    private JsonArray createFilteredResultsJson(AppInfo appInfo, AnalysisScope scope) {
        JsonArray resultsArray = new JsonArray();

        for (ReachClass reachClass : appInfo.getClasses()) {
            JsonObject classJson = createClassJson(reachClass, scope);

            // Only include classes that have methods after filtering
            JsonArray methodsArray = classJson.getAsJsonArray("methods");
            if (methodsArray.size() > 0) {
                resultsArray.add(classJson);
            }
        }

        return resultsArray;
    }

    /**
     * Create JSON representation of a class with filtered methods.
     * <p>
     * This method handles the core filtering logic while maintaining
     * complete class metadata and hierarchical structure.
     */
    private JsonObject createClassJson(ReachClass reachClass, AnalysisScope scope) {
        JsonObject classJson = new JsonObject();
        classJson.addProperty("class", reachClass.getClassName());
        classJson.addProperty("component_type", reachClass.getComponentType().toString().toLowerCase());
        classJson.addProperty("is_main_component", reachClass.isMainComponent());

        // Apply filtering during method JSON creation
        JsonArray methodsArray = new JsonArray();
        for (ReachMethod method : reachClass.getMethods()) {
            if (shouldIncludeMethod(method, scope)) {
                methodsArray.add(createMethodJson(method));
            }
        }
        classJson.add("methods", methodsArray);

        return classJson;
    }

    /**
     * Create complete JSON representation of a method.
     * <p>
     * This method includes all available reachability information,
     * path data, and analysis results for comprehensive method documentation.
     */
    private JsonObject createMethodJson(ReachMethod method) {
        JsonObject methodJson = new JsonObject();
        methodJson.addProperty("name", method.getMethodName());
        methodJson.addProperty("signature", method.getMethodSignature());
        methodJson.addProperty("is_entry_point", method.isEntryPoint());
        methodJson.addProperty("reachable", method.isReachable());
        methodJson.addProperty("reaches_targets", method.isReachesTarget());
        methodJson.addProperty("directly_reaches_targets", method.isDirectlyReachesTarget());

        // Reachable targets array
        JsonArray targetsArray = new JsonArray();
        method.getReachableTargets().forEach(targetsArray::add);
        methodJson.add("reachable_targets", targetsArray);

        // Path information if available and configured
        if (method.getPathsToTargets() != null && !method.getPathsToTargets().isEmpty()) {
            JsonArray pathsArray = new JsonArray();
            for (Path path : method.getPathsToTargets()) {
                JsonArray pathArray = new JsonArray();
                path.getPath().forEach(pathArray::add);
                pathsArray.add(pathArray);
            }
            methodJson.add("paths", pathsArray);
        }

        return methodJson;
    }

    /**
     * Create comprehensive statistics JSON with filtering metrics.
     * <p>
     * This method computes detailed statistics about the filtering operation
     * and analysis results, providing insight into the effectiveness of
     * the analysis scope configuration.
     */
    private JsonObject createStatisticsJson(AppInfo appInfo, AnalysisScope scope) {
        JsonObject statsJson = new JsonObject();

        // Compute filtering statistics during single pass
        int totalMethods = 0;
        int includedMethods = 0;
        int reachableMethods = 0;
        int targetReachingMethods = 0;
        int entryPointMethods = 0;
        int classesWithMethods = 0;

        for (ReachClass clazz : appInfo.getClasses()) {
            boolean classHasMethods = false;
            for (ReachMethod method : clazz.getMethods()) {
                totalMethods++;
                if (shouldIncludeMethod(method, scope)) {
                    includedMethods++;
                    classHasMethods = true;
                    
                    if (method.isReachable()) reachableMethods++;
                    if (method.isReachesTarget()) targetReachingMethods++;
                    if (method.isEntryPoint()) entryPointMethods++;
                }
            }
            if (classHasMethods) classesWithMethods++;
        }

        // Basic counts
        statsJson.addProperty("total_methods", totalMethods);
        statsJson.addProperty("included_methods", includedMethods);
        statsJson.addProperty("filtered_methods", totalMethods - includedMethods);
        statsJson.addProperty("reachable_methods", reachableMethods);
        statsJson.addProperty("target_reaching_methods", targetReachingMethods);
        statsJson.addProperty("entry_point_methods", entryPointMethods);
        statsJson.addProperty("classes_with_methods", classesWithMethods);
        statsJson.addProperty("total_classes", appInfo.getClasses().size());

        // Computed rates for analysis insight
        if (totalMethods > 0) {
            double inclusionRate = (includedMethods * 100.0) / totalMethods;
            statsJson.addProperty("inclusion_rate_percent", 
                Math.round(inclusionRate * 10.0) / 10.0);
        }

        if (includedMethods > 0) {
            double reachabilityRate = (reachableMethods * 100.0) / includedMethods;
            statsJson.addProperty("reachability_rate_percent", 
                Math.round(reachabilityRate * 10.0) / 10.0);
        }

        return statsJson;
    }

    /**
     * Core filtering predicate matching CsvWriter logic.
     * <p>
     * This method implements identical filtering logic to CsvWriter
     * to ensure consistent behavior across output formats.
     */
    private boolean shouldIncludeMethod(ReachMethod method, AnalysisScope scope) {
        return switch (scope) {
            case ALL_METHODS -> true;
            case REACHABLE_ONLY -> method.isReachable();
            default -> {
                log.warn("Unknown analysis scope: {}. Defaulting to ALL_METHODS", scope);
                yield true;
            }
        };
    }

    /**
     * Log comprehensive statistics about JSON generation.
     */
    private void logJsonStatistics(JsonObject root, AnalysisScope scope, File outputFile) {
        JsonObject stats = root.getAsJsonObject("statistics");
        if (stats != null) {
            log.info("JSON output completed successfully");
            log.info("Output file: {}", outputFile.getAbsolutePath());
            log.info("Analysis scope: {}", scope.getValue());
            log.info("JSON statistics:");
            log.info("  Total methods: {}", stats.get("total_methods").getAsInt());
            log.info("  Included methods: {}", stats.get("included_methods").getAsInt());
            log.info("  Filtered methods: {}", stats.get("filtered_methods").getAsInt());

            if (scope == AnalysisScope.REACHABLE_ONLY && stats.has("inclusion_rate_percent")) {
                log.info("  Inclusion rate: {}%", stats.get("inclusion_rate_percent").getAsDouble());
            }

            log.info("  Classes with methods: {}", stats.get("classes_with_methods").getAsInt());
            log.info("  Total classes: {}", stats.get("total_classes").getAsInt());
        }

        // File size information
        long fileSize = outputFile.length();
        log.info("  Output file size: {} bytes", fileSize);
    }

    /**
     * Validate input parameters before processing.
     */
    private void validateParameters(ReachabilityResult result, File outputFile) {
        if (result == null) {
            throw new IllegalArgumentException("ReachabilityResult cannot be null");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }
        if (result.getAppInfo() == null) {
            throw new IllegalArgumentException("AppInfo in result cannot be null");
        }
    }
}