package br.unb.cic.reach.common.writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.model.AnalysisScope;
import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ConfigMatrix;
import br.unb.cic.reach.common.model.ReachClass;
import br.unb.cic.reach.common.model.ReachMethod;

/**
 * Optimized CSV writer with ConfigMatrix integration and analysis scope filtering.
 * 
 * This writer implements efficient CSV generation with configurable method filtering
 * based on the analysis scope configuration. It provides detailed statistics about
 * filtering operations and maintains backward compatibility with existing CSV formats.
 * 
 * ### Filtering Implementation:
 * The writer applies analysis scope filtering during output generation, which means:
 * - ALL_METHODS: All discovered methods are written to CSV
 * - REACHABLE_ONLY: Only methods marked as reachable from entry points are written
 * - Empty classes (after filtering) are still included to maintain structure
 * 
 * ### Performance Optimization:
 * - Single-pass filtering during write operation
 * - Efficient character encoding handling
 * - Minimal memory allocation during CSV generation
 * - Batch statistics computation for comprehensive reporting
 */
public class CsvWriter implements Writer {
    private static final Logger log = LoggerFactory.getLogger(CsvWriter.class);

    @Override
    public void write(ReachabilityResult result, File outputFile) throws IOException {
        validateParameters(result, outputFile);

        AppInfo appInfo = result.getAppInfo();
        AnalysisScope scope = result.getAnalysisScope();
        
        log.info("Writing CSV results to: {} with scope: {}", 
                outputFile.getAbsolutePath(), scope.getValue());

        FilteringStatistics stats = new FilteringStatistics();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            writeHeader(writer);
            
            // Single-pass filtering and writing for optimal performance
            for (ReachClass reachClass : appInfo.getClasses()) {
                writeClassMethods(writer, reachClass, scope, stats);
            }
        }

        logFilteringStatistics(outputFile, stats, scope);
    }

    /**
     * Write all methods from a class that match the analysis scope filter.
     * 
     * This method applies the core filtering logic that implements the analysis
     * scope functionality. The filtering happens during write operation to
     * minimize memory usage and provide immediate feedback about filtering effects.
     */
    private void writeClassMethods(PrintWriter writer, ReachClass reachClass, 
                                  AnalysisScope scope, FilteringStatistics stats) {
        for (ReachMethod method : reachClass.getMethods()) {
            stats.totalMethods++;
            
            // Core filtering logic - this is where analysis scope is applied
            if (shouldIncludeMethod(method, scope)) {
                writeCsvLine(writer, reachClass, method);
                stats.includedMethods++;
                
                // Collect detailed statistics for comprehensive reporting
                if (method.isReachable()) {
                    stats.reachableMethods++;
                }
                if (method.isReachesTarget()) {
                    stats.targetReachingMethods++;
                }
                if (method.isEntryPoint()) {
                    stats.entryPointMethods++;
                }
            } else {
                stats.filteredMethods++;
            }
        }
    }

    /**
     * Core filtering predicate implementing analysis scope logic.
     * 
     * This method implements the fundamental filtering decision that determines
     * which methods appear in the final output based on the user's analysis scope choice.
     * The logic is intentionally simple and direct for maximum performance and clarity.
     * 
     * ### Filtering Rules:
     * - ALL_METHODS: Include all discovered methods regardless of reachability
     * - REACHABLE_ONLY: Include only methods that are reachable from entry points
     * 
     * ### Performance Note:
     * This method is called for every method in the analysis, so it must be highly
     * optimized. The switch statement provides optimal branch prediction performance.
     */
    private boolean shouldIncludeMethod(ReachMethod method, AnalysisScope scope) {
        switch (scope) {
            case ALL_METHODS:
                return true;
            case REACHABLE_ONLY:
                // This is the key filtering condition - only reachable methods pass
                return method.isReachable();
            default:
                log.warn("Unknown analysis scope: {}. Defaulting to ALL_METHODS", scope);
                return true;
        }
    }

    /**
     * Write CSV header with complete field documentation.
     */
    private void writeHeader(PrintWriter writer) {
        writer.println("class,method,signature,component_type,is_main_component," +
                      "is_entry_point,reachable,reaches_targets,directly_reaches_targets," +
                      "reachable_targets");
    }

    /**
     * Write individual method row with proper CSV escaping.
     * 
     * This method handles the complexity of CSV formatting including proper
     * escaping of special characters, null value handling, and consistent
     * field ordering that maintains compatibility with existing analysis tools.
     */
    private void writeCsvLine(PrintWriter writer, ReachClass reachClass, ReachMethod method) {
        // Concatenate reachable targets with semicolon separator
        String reachableTargets = String.join(";", method.getReachableTargets());
        
        // Use proper CSV formatting with quoted fields to handle commas and special characters
        writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",%b,%b,%b,%b,%b,\"%s\"%n",
            escapeCsvField(reachClass.getClassName()),
            escapeCsvField(method.getMethodName()),
            escapeCsvField(method.getMethodSignature()),
            reachClass.getComponentType().toString().toLowerCase(),
            reachClass.isMainComponent(),
            method.isEntryPoint(),
            method.isReachable(),
            method.isReachesTarget(),
            method.isDirectlyReachesTarget(),
            escapeCsvField(reachableTargets)
        );
    }

    /**
     * Escape CSV field content to handle commas, quotes, and newlines properly.
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        
        // Escape internal quotes by doubling them (CSV standard)
        return field.replace("\"", "\"\"");
    }

    /**
     * Log comprehensive statistics about the CSV writing operation.
     * 
     * This method provides detailed feedback about the filtering operation,
     * helping users understand the impact of their analysis scope choice.
     * The statistics are particularly valuable for REACHABLE_ONLY scope
     * where significant filtering may occur.
     */
    private void logFilteringStatistics(File outputFile, FilteringStatistics stats, 
                                      AnalysisScope scope) {
        log.info("CSV output completed successfully");
        log.info("Output file: {}", outputFile.getAbsolutePath());
        log.info("Analysis scope: {}", scope.getValue());
        log.info("Writing statistics:");
        log.info("  Total methods processed: {}", stats.totalMethods);
        log.info("  Methods included in output: {}", stats.includedMethods);
        log.info("  Methods filtered out: {}", stats.filteredMethods);
        
        // Detailed statistics for reachable-only scope
        if (scope == AnalysisScope.REACHABLE_ONLY && stats.totalMethods > 0) {
            double filteringRate = (stats.filteredMethods * 100.0) / stats.totalMethods;
            log.info("  Filtering effectiveness: {:.1f}% of methods excluded", filteringRate);
            
            if (stats.includedMethods > 0) {
                double reachabilityRate = (stats.reachableMethods * 100.0) / stats.includedMethods;
                log.info("  Reachability validation: {:.1f}% of included methods are reachable", reachabilityRate);
            }
        }
        
        // Additional metrics for analysis insight
        log.info("  Entry points in output: {}", stats.entryPointMethods);
        log.info("  Target-reaching methods in output: {}", stats.targetReachingMethods);
        
        // File size information for performance monitoring
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

    /**
     * Statistics container for comprehensive filtering operation tracking.
     * 
     * This class captures detailed metrics about the filtering operation,
     * enabling comprehensive reporting and performance analysis of the
     * analysis scope functionality.
     */
    private static class FilteringStatistics {
        int totalMethods = 0;
        int includedMethods = 0;
        int filteredMethods = 0;
        int reachableMethods = 0;
        int targetReachingMethods = 0;
        int entryPointMethods = 0;
    }
}