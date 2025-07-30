package br.unb.cic.reach.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import br.unb.cic.reach.common.analysis.ReachabilityAnalysis;
import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.analysis.SootReachabilityStrategy;
import br.unb.cic.reach.common.extractor.ApplicationExtractor;
import br.unb.cic.reach.common.model.AnalysisScope;
import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ConfigMatrix;
import br.unb.cic.reach.common.model.ConfigurationException;
import br.unb.cic.reach.common.model.EntryPoint;
import br.unb.cic.reach.common.writer.Writer;
import br.unb.cic.reach.common.writer.WriterFactory;
import br.unb.cic.reach.main.factory.ExtractorFactory;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

/**
 * Optimized main application with ConfigMatrix integration and O(N+E) algorithm.
 * 
 * This class orchestrates the complete reachability analysis pipeline using the
 * unified ConfigMatrix system and optimized algorithms. It provides a clean
 * command-line interface while leveraging the full power of the configuration
 * matrix for intelligent parameter management and algorithm selection.
 * 
 * ### Pipeline Architecture:
 * 1. Parse and validate CLI parameters
 * 2. Build ConfigMatrix with intelligent defaults  
 * 3. Initialize appropriate extractor (APK/JAR)
 * 4. Execute analysis with optimized algorithms
 * 5. Generate filtered output based on analysis scope
 * 6. Provide comprehensive reporting and statistics
 * 
 * ### Performance Optimization:
 * - ConfigMatrix-driven algorithm selection
 * - O(N+E) reachability analysis with batch processing
 * - Efficient output filtering during generation
 * - Minimal memory allocation and optimal data structures
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            long startTime = System.currentTimeMillis();

            // Parse and validate command line arguments
            CommandLineArgs cliArgs = parseAndValidateArgs(args);
            if (cliArgs.isHelp()) {
                return; // Help was displayed, exit normally
            }

            // Build unified configuration matrix
            ConfigMatrix configMatrix = cliArgs.buildConfigMatrix();
            log.info("Configuration Matrix initialized:\n{}", configMatrix.getConfigurationSummary());

            // Execute analysis with optimized pipeline
            ReachabilityResult result = executeAnalysis(cliArgs, configMatrix);

            // Write results using configured output format
            writeResults(result, cliArgs, configMatrix);

            // Log comprehensive final summary
            long totalTime = System.currentTimeMillis() - startTime;
            logFinalSummary(result, totalTime);

        } catch (ParameterException e) {
            handleParameterError(e);
            System.exit(1);
        } catch (ConfigurationException e) {
            handleConfigurationError(e);
            System.exit(1);
        } catch (Exception e) {
            handleUnexpectedError(e);
            System.exit(1);
        }
    }

    /**
     * Execute optimized reachability analysis with ConfigMatrix integration.
     * 
     * This method coordinates the complete analysis pipeline, selecting appropriate
     * extractors and algorithms based on the configuration matrix. It leverages
     * the O(N+E) optimized algorithm for significant performance improvements
     * over the previous O(N×M×C_path) implementation.
     */
    private static ReachabilityResult executeAnalysis(CommandLineArgs cliArgs, 
                                                    ConfigMatrix configMatrix) throws Exception {
        
        // Initialize appropriate extractor based on file type
        ApplicationExtractor extractor = ExtractorFactory.create(cliArgs.getInputPath());
        extractor.initialize(configMatrix);

        if (configMatrix.isExtractOnly()) {
            return executeExtractOnlyAnalysis(cliArgs, extractor, configMatrix);
        } else {
            return executeFullReachabilityAnalysis(cliArgs, extractor, configMatrix);
        }
    }

    /**
     * Execute extract-only analysis for basic application information.
     * 
     * This mode provides fast application structure analysis without
     * call graph construction or reachability computation. It supports
     * direct call detection when targets are provided.
     */
    private static ReachabilityResult executeExtractOnlyAnalysis(CommandLineArgs cliArgs,
                                                               ApplicationExtractor extractor,
                                                               ConfigMatrix configMatrix) throws Exception {
        log.info("Executing extract-only analysis");
        
        Set<String> targetSignatures = loadTargetSignatures(cliArgs.getTargetsFile());
        AppInfo appInfo;
        
        if (targetSignatures != null && !targetSignatures.isEmpty()) {
            log.info("Analyzing direct calls to {} target methods", targetSignatures.size());
            appInfo = extractor.extractAppInfo(targetSignatures);
        } else {
            log.info("Extracting application information without target analysis");
            appInfo = extractor.extractAppInfo();
        }
        
        log.info("Extract-only analysis completed: {} classes, {} total methods",
                appInfo.getClasses().size(),
                appInfo.getClasses().stream().mapToInt(c -> c.getMethods().size()).sum());
        
        ReachabilityResult result = new ReachabilityResult(appInfo);
        result.setConfigMatrix(configMatrix);
        return result;
    }

    /**
     * Execute full reachability analysis with optimized O(N+E) algorithm.
     * 
     * This method implements the complete optimized analysis pipeline:
     * - Call graph construction with intelligent configuration
     * - Entry point extraction based on ConfigMatrix settings
     * - Target method resolution with comprehensive validation
     * - O(N+E) batch reachability analysis with reverse graph optimization
     * - Result integration with ConfigMatrix metadata
     */
    private static ReachabilityResult executeFullReachabilityAnalysis(CommandLineArgs cliArgs,
                                                                    ApplicationExtractor extractor,
                                                                    ConfigMatrix configMatrix) throws Exception {
        log.info("Executing full reachability analysis with algorithm: {}", 
                configMatrix.getReachabilityAlgorithm().getValue());

        // Phase 1: Build call graph with intelligent configuration
        log.info("Building call graph...");
        long callGraphStart = System.currentTimeMillis();
        CallGraph callGraph = extractor.buildCallGraph();
        long callGraphTime = System.currentTimeMillis() - callGraphStart;
        log.info("Call graph built in {}ms", callGraphTime);

        // Phase 2: Extract entry points based on ConfigMatrix settings
        log.info("Extracting entry points for types: {}", 
                configMatrix.getEntryPointTypes().stream()
                    .map(type -> type.name().toLowerCase())
                    .reduce((a, b) -> a + ", " + b).orElse("none"));
        Set<EntryPoint> entryPoints = extractor.extractEntryPoints();
        log.info("Found {} entry points", entryPoints.size());

        // Phase 3: Resolve target methods with validation
        Set<String> targetSignatures = loadTargetSignatures(cliArgs.getTargetsFile());
        Set<SootMethod> targetMethods = extractor.resolveTargetMethods(targetSignatures);
        log.info("Resolved {} target methods from {} signatures", 
                targetMethods.size(), targetSignatures.size());

        // Phase 4: Execute optimized reachability analysis
        log.info("Executing O(N+E) optimized reachability analysis...");
        
        // Build base AppInfo for consistent scoping
        AppInfo baseAppInfo = extractor.extractAppInfo();
        
        // Create strategy (maintained for compatibility, but analysis is optimized internally)
        SootReachabilityStrategy strategy = new SootReachabilityStrategy();
        
        // Execute optimized analysis with ConfigMatrix integration
        ReachabilityAnalysis analysis = new ReachabilityAnalysis();
        ReachabilityResult result = analysis.analyze(
            callGraph, entryPoints, targetMethods, strategy, baseAppInfo, configMatrix);

        log.info("Optimized reachability analysis completed");
        return result;
    }

    /**
     * Write results using ConfigMatrix-driven output configuration.
     * 
     * This method handles the output generation with analysis scope filtering
     * and comprehensive statistics reporting. The ConfigMatrix provides unified
     * access to all output configuration parameters.
     */
    private static void writeResults(ReachabilityResult result, CommandLineArgs cliArgs, 
                                   ConfigMatrix configMatrix) throws IOException {
        log.info("Writing results to: {} (format: {}, scope: {})", 
                cliArgs.getOutputFile(), 
                configMatrix.getWriterType().getExtension(),
                configMatrix.getAnalysisScope().getValue());

        // Create appropriate writer based on ConfigMatrix
        Writer writer = WriterFactory.create(configMatrix.getWriterType().getExtension());
        
        File outputFile = new File(cliArgs.getOutputFile());
        writer.write(result, outputFile);

        log.info("Results written successfully");
    }

    /**
     * Load target method signatures from file.
     * 
     * This method handles target file parsing with comprehensive error handling
     * and comment/empty line filtering for clean target specification.
     */
    private static Set<String> loadTargetSignatures(String targetsFile) throws IOException {
        if (targetsFile == null) {
            return Collections.emptySet();
        }

        Set<String> signatures = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(targetsFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines and comments
                if (!line.isEmpty() && !line.startsWith("#")) {
                    signatures.add(line);
                }
            }
        }

        log.info("Loaded {} target signatures from: {}", signatures.size(), targetsFile);
        return signatures;
    }

    /**
     * Parse and validate command line arguments with comprehensive error handling.
     */
    private static CommandLineArgs parseAndValidateArgs(String[] args) throws ParameterException {
        CommandLineArgs cliArgs = new CommandLineArgs();
        JCommander commander = new JCommander(cliArgs);

        try {
            commander.parse(args);
            
            if (cliArgs.isHelp()) {
                commander.usage();
                return cliArgs;
            }
            
            cliArgs.validate();
            return cliArgs;
        } catch (ParameterException e) {
            System.err.println("Parameter error: " + e.getMessage());
            System.err.println();
            commander.usage();
            throw e;
        }
    }

    /**
     * Log comprehensive final summary with ConfigMatrix details.
     * 
     * This method provides detailed feedback about the analysis execution
     * including configuration parameters, performance metrics, and result
     * statistics for complete analysis transparency.
     */
    private static void logFinalSummary(ReachabilityResult result, long totalTime) {
        log.info("");
        log.info("=== ANALYSIS COMPLETED ===");

        // Application information
        AppInfo appInfo = result.getAppInfo();
        if (appInfo != null) {
            log.info("Application: {} ({})", appInfo.getLabel(), appInfo.getType());
            log.info("Package: {}", appInfo.getPackageName());
            log.info("Classes analyzed: {}", appInfo.getClasses().size());

            int totalMethods = appInfo.getClasses().stream()
                .mapToInt(clazz -> clazz.getMethods().size())
                .sum();
            log.info("Total methods: {}", totalMethods);

            // Analysis scope specific metrics
            AnalysisScope scope = result.getAnalysisScope();
            log.info("Analysis scope: {}", scope.getValue());
            
            if (scope == AnalysisScope.REACHABLE_ONLY && totalMethods > 0) {
                int reachableMethods = result.getReachableMethodCount();
                double reachabilityRate = (reachableMethods * 100.0) / totalMethods;
                log.info("Reachable methods: {} ({:.1f}% of total)", 
                        reachableMethods, reachabilityRate);
            }

            int targetReachingMethods = result.getTargetReachingMethodCount();
            log.info("Target-reaching methods: {}", targetReachingMethods);
        }

        // Configuration and performance information
        ConfigMatrix configMatrix = result.getConfigMatrix();
        if (configMatrix != null) {
            log.info("Algorithm: {}", configMatrix.getReachabilityAlgorithm().getValue());
            log.info("Performance priority: {}", configMatrix.getPerformancePriority().getValue());
            log.info("Configuration: {}", configMatrix.getMatrixPosition());
        }

        log.info("Entry points: {}", result.getEntryPointCount());
        log.info("Target methods: {}", result.getTargetMethodCount());
        log.info("Analysis time: {}ms", result.getExecutionTime());
        log.info("Total execution time: {}ms", totalTime);
        log.info("===========================");
    }

    // Error handling methods

    private static void handleParameterError(ParameterException e) {
        System.err.println("Error: Invalid command line parameters");
        System.err.println(e.getMessage());
        System.err.println("\nUse --help for usage information");
    }

    private static void handleConfigurationError(ConfigurationException e) {
        System.err.println("Error: Configuration validation failed");
        System.err.println(e.getMessage());
        System.err.println("\nPlease check your parameter combinations");
    }

    private static void handleUnexpectedError(Exception e) {
        System.err.println("Error: Unexpected analysis failure");
        System.err.println(e.getMessage());

        log.error("Unexpected error during analysis", e);
        System.err.println("\nThis appears to be a system error. Please report with:");
        System.err.println("1. The command line used");
        System.err.println("2. The input file (if shareable)");
        System.err.println("3. The complete error log");
    }
}