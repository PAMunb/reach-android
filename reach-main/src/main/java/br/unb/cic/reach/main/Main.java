package br.unb.cic.reach.main;

import br.unb.cic.reach.common.analysis.ReachabilityAnalysis;
import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.analysis.strategy.ReachabilityStrategy;
import br.unb.cic.reach.common.analysis.strategy.ReachabilityStrategyFactory;
import br.unb.cic.reach.common.extractor.ApplicationExtractor;
import br.unb.cic.reach.common.model.*;
import br.unb.cic.reach.common.writer.Writer;
import br.unb.cic.reach.common.writer.WriterFactory;
import br.unb.cic.reach.main.factory.ExtractorFactory;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Optimized main application with ConfigMatrix integration and O(N+E) algorithm.
 * <p>
 * This class orchestrates the complete reachability analysis pipeline using the
 * unified ConfigMatrix system and optimized algorithms. It provides a clean
 * command-line interface while leveraging the full power of the configuration
 * matrix for intelligent parameter management and algorithm selection.
 * <p>
 * ### Pipeline Architecture:
 * 1. Parse and validate CLI parameters
 * 2. Build ConfigMatrix with intelligent defaults
 * 3. Initialize appropriate extractor (APK/JAR)
 * 4. Execute analysis with optimized algorithms
 * 5. Generate filtered output based on analysis scope
 * 6. Provide comprehensive reporting and statistics
 * <p>
 * ### Performance Optimization:
 * - ConfigMatrix-driven algorithm selection
 * - O(N+E) reachability analysis with batch processing
 * - Efficient output filtering during generation
 * - Minimal memory allocation and optimal data structures
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
//        runCLI(args);

        runLocal();
    }

    private static void runLocal() {
        String apkPath = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/cryptoapp.apk";
        String androidPlatformsDir = "/home/pedro/desenvolvimento/aplicativos/android/sdk/platforms";
        String rtJarPath = "/home/pedro/.sdkman/candidates/java/8.0.302-open/jre/lib/rt.jar";
        int timeoutSeconds = 300;
        String targetsFile = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/mopMethods.txt";
        String outputFile = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/result_run_local.csv";

        try {
            ConfigMatrix config = new ConfigMatrix.Builder()
                    .withInputPath(apkPath)
                    .withAndroidPlatformsDir(androidPlatformsDir)
                    .withRtJarPath(rtJarPath)
                    .withTimeout(timeoutSeconds)
                    .withTargetsFile(targetsFile)
                    .withOutputFile(outputFile)
                    .withAppPackageOnly(true)
                    .withExtractOnly(false)
                    .withAnalysisScope(AnalysisScope.ALL_METHODS)
                    .withReachabilityStrategy(ConfigMatrix.ReachabilityStrategyType.SOOT_BFS)
                    .withCallGraphAlgorithm(ConfigMatrix.CallGraphAlgorithm.SPARK)
                    .withAliasingAlgorithm(ConfigMatrix.AliasingAlgorithm.FlowSensitive)
                    .build();

            ReachabilityResult result = executeAnalysis(config);

            System.out.println("RESULTS:");
            AppInfo appInfo = result.getAppInfo();
            for (ReachClass reachClass : appInfo.getClasses()) {
                System.out.println("CLASS: " + reachClass.getClassName());
                for (ReachMethod reachMethod : reachClass.getMethods()) {
                    System.out.println("\t" + reachMethod);
                }
            }


            writeResults(result, config);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runCLI(String[] args) {
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
            ReachabilityResult result = executeAnalysis(configMatrix);

            // Write results using configured output format
            writeResults(result, configMatrix);

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
     * <p>
     * This method coordinates the complete analysis pipeline, selecting appropriate
     * extractors and algorithms based on the configuration matrix. It leverages
     * the O(N+E) optimized algorithm for significant performance improvements
     * over the previous O(N×M×C_path) implementation.
     */
    private static ReachabilityResult executeAnalysis(ConfigMatrix configMatrix) throws Exception {
        // Initialize appropriate extractor based on file type
        ApplicationExtractor extractor = ExtractorFactory.create(configMatrix.getInputPath());
        extractor.initialize(configMatrix);

        if (configMatrix.isExtractOnly()) {
            return executeExtractOnlyAnalysis(extractor, configMatrix);
        } else {
            return executeFullReachabilityAnalysis(extractor, configMatrix);
        }
    }

    /**
     * Execute extract-only analysis for basic application information.
     * <p>
     * This mode provides fast application structure analysis without
     * call graph construction or reachability computation. It supports
     * direct call detection when targets are provided.
     */
    private static ReachabilityResult executeExtractOnlyAnalysis(ApplicationExtractor extractor,
                                                                 ConfigMatrix configMatrix) throws Exception {
        log.info("Executing extract-only analysis");

        Set<String> targetSignatures = loadTargetSignatures(configMatrix.getTargetsFile());
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
     * <p>
     * This method implements the complete optimized analysis pipeline:
     * - Call graph construction with intelligent configuration
     * - Entry point extraction based on ConfigMatrix settings
     * - Target method resolution with comprehensive validation
     * - O(N+E) batch reachability analysis with reverse graph optimization
     * - Result integration with ConfigMatrix metadata
     */
    private static ReachabilityResult executeFullReachabilityAnalysis(ApplicationExtractor extractor,
                                                                      ConfigMatrix configMatrix) throws Exception {
        log.info("Executing full reachability analysis with algorithm: {}",
                configMatrix.getReachabilityStrategy().getValue());

        // Phase 1: Build call graph with intelligent configuration
        log.info("Building call graph...");
        long callGraphStart = System.currentTimeMillis();
        CallGraph callGraph = extractor.buildCallGraph();
        long callGraphTime = System.currentTimeMillis() - callGraphStart;
        log.info("Call graph built in {}ms", callGraphTime);
        System.out.println("DEBUG_REACH: Call graph edges: " + callGraph.size()); // DEBUG_REACH

        // Phase 2: Extract entry points based on ConfigMatrix settings
        boolean seen = false;
        String acc = null;
        for (ComponentType type : configMatrix.getEntryPointTypes()) {
            String lowerCase = type.name().toLowerCase();
            if (!seen) {
                seen = true;
                acc = lowerCase;
            } else {
                acc = acc + ", " + lowerCase;
            }
        }
        log.info("Extracting entry points for types: {}", seen ? acc : "none");
        Set<EntryPoint> entryPoints = extractor.extractEntryPoints();
        log.info("Found {} entry points", entryPoints.size());
        System.out.println("DEBUG_REACH: Entry points found: " + entryPoints.size()); // DEBUG_REACH
        for (EntryPoint ep : entryPoints) {
            System.out.println("DEBUG_REACH:   - " + ep.getSignature() + " [" + ep.getComponentType() + "]"); // DEBUG_REACH
        }

        // Phase 3: Resolve target methods with validation
        Set<String> targetSignatures = loadTargetSignatures(configMatrix.getTargetsFile());
        Set<SootMethod> targetMethods = extractor.resolveTargetMethods(targetSignatures);
        log.info("Resolved {} target methods from {} signatures",
                targetMethods.size(), targetSignatures.size());
        System.out.println("DEBUG_REACH: Target methods resolved: " + targetMethods.size()); // DEBUG_REACH
        for (SootMethod target : targetMethods) {
            System.out.println("DEBUG_REACH:   - " + target.getSignature()); // DEBUG_REACH
        }

        // Phase 4: Execute optimized reachability analysis
        log.info("Executing O(N+E) optimized reachability analysis...");

        // Build base AppInfo with direct/indirect call analysis included
        // This ensures both extract-only and full analysis use the same comprehensive approach
        AppInfo baseAppInfo = extractor.extractAppInfo(targetSignatures);
        System.out.println("DEBUG_REACH: Base AppInfo classes: " + baseAppInfo.getClasses().size()); // DEBUG_REACH
        System.out.println("DEBUG_REACH: Base AppInfo total methods: " + baseAppInfo.getClasses().stream().mapToInt(c -> c.getMethods().size()).sum()); // DEBUG_REACH

        // Create strategy
        ReachabilityStrategy strategy = ReachabilityStrategyFactory.createReachabilityStrategy(configMatrix);

        // Execute optimized analysis with ConfigMatrix integration
        ReachabilityAnalysis analysis = new ReachabilityAnalysis();
        System.out.println("DEBUG_REACH: Starting ReachabilityAnalysis.analyze()"); // DEBUG_REACH
        ReachabilityResult result = analysis.analyze(callGraph, entryPoints, targetMethods, strategy, baseAppInfo, configMatrix);
        System.out.println("DEBUG_REACH: ReachabilityAnalysis.analyze() completed"); // DEBUG_REACH

        log.info("Optimized reachability analysis completed");
        return result;
    }

    /**
     * Write results using ConfigMatrix-driven output configuration.
     * <p>
     * This method handles the output generation with analysis scope filtering
     * and comprehensive statistics reporting. The ConfigMatrix provides unified
     * access to all output configuration parameters.
     */
    private static void writeResults(ReachabilityResult result, ConfigMatrix configMatrix) throws IOException {
        log.info("Writing results to: {} (format: {}, scope: {})",
                configMatrix.getOutputFile(),
                configMatrix.getWriterType().getExtension(),
                configMatrix.getAnalysisScope().getValue());

        // Create appropriate writer based on ConfigMatrix
        Writer writer = WriterFactory.create(configMatrix.getWriterType().getExtension());

        File outputFile = new File(configMatrix.getOutputFile());
        writer.write(result, outputFile);

        log.info("Results written successfully");
    }

    /**
     * Load target method signatures from file.
     * <p>
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
            while ((line = reader.readLine()) != null) {
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
     * <p>
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
            log.info("Reachability Algorithm: {}", configMatrix.getReachabilityStrategy().getValue());
            log.info("Callgraph Algorithm: {}", configMatrix.getCallGraphAlgorithm().getValue());
            log.info("Aliasing Algorithm: {}", configMatrix.getAliasingAlgorithm().getValue());
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