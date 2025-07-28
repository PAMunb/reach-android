package br.unb.cic.reach.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import br.unb.cic.reach.common.analysis.SootReachabilityStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import br.unb.cic.reach.android.AndroidExtractor;
import br.unb.cic.reach.common.analysis.ReachabilityAnalysis;
import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.extractor.ApplicationExtractor;
import br.unb.cic.reach.main.factory.ExtractorFactory;
import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.EntryPoint;
import br.unb.cic.reach.common.writer.Writer;
import br.unb.cic.reach.common.writer.WriterFactory;
import br.unb.cic.reach.jar.JarExtractor;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

/**
 * Main application entry point for reachability analysis.
 * <p>
 * This class serves as the primary interface for executing reachability analysis
 * on different application types (APK, JAR) through a unified command line
 * interface with automatic format detection and analysis orchestration.
 * <p>
 * ### Architectural Decisions:
 * - Facade pattern orchestrating different analysis scenarios
 * - Automatic application type detection and extractor selection
 * - Unified error handling with descriptive user messages
 * - Performance monitoring and logging for analysis transparency
 * <p>
 * ### Role in the System:
 * - Primary user interface for reachability analysis execution
 * - Orchestration point for different analysis workflows
 * - Error handling and user feedback interface
 * - Integration point for CLI configuration and analysis execution
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Application entry point for command line execution.
     * <p>
     * Processes command line arguments, validates configuration, and executes
     * appropriate analysis workflow based on input file type and user parameters.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try {
            // Parse and validate command line arguments
            CommandLineArgs cliArgs = parseCommandLineArgs(args);

            if (cliArgs.isHelp()) {
                return; // Help already displayed
            }

            // Configure logging
            configureLogging(cliArgs.isDebug());

            // Execute analysis
            executeAnalysis(cliArgs);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Analysis completed successfully in {} seconds", executionTime / 1000.0);

        } catch (ParameterException e) {
            System.err.println("Parameter error: " + e.getMessage());
            System.err.println("Use --help for usage information");
            System.exit(1);
        } catch (Exception e) {
            log.error("Analysis failed: {}", e.getMessage(), e);
            System.err.println("Analysis failed: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parses and validates command line arguments.
     * <p>
     * Processes command line parameters using JCommander, performs validation,
     * and provides help information when requested.
     *
     * @param args Command line arguments
     * @return Validated CommandLineArgs object
     * @throws ParameterException if argument parsing or validation fails
     */
    private static CommandLineArgs parseCommandLineArgs(String[] args) {
        CommandLineArgs cliArgs = new CommandLineArgs();
        JCommander commander = new JCommander(cliArgs);
        commander.setProgramName("reach-analyzer");

        // Show help if no arguments provided
        if (args.length == 0) {
            commander.usage();
            System.exit(0);
        }

        try {
            commander.parse(args);

            if (cliArgs.isHelp()) {
                commander.usage();
                System.exit(0);
            }

            // Validate arguments
            cliArgs.validate();

            return cliArgs;

        } catch (ParameterException e) {
            System.err.println("Error: " + e.getMessage());
            commander.usage();
            throw e;
        }
    }

    /**
     * Configures logging level based on debug flag.
     */
    private static void configureLogging(boolean debug) {
        if (debug) {
            ch.qos.logback.classic.Logger root =
                    (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(ch.qos.logback.classic.Level.DEBUG);
            log.info("Debug logging enabled");
        }
    }

    /**
     * Executes analysis based on command line configuration.
     * <p>
     * Orchestrates the complete analysis workflow including extractor creation,
     * configuration, and analysis execution based on the specified parameters
     * and application type.
     * <p>
     * ### Execution Workflow:
     * - Create appropriate extractor based on input file type
     * - Configure extractor with user parameters
     * - Execute extract-only or full analysis based on configuration
     * - Generate output using specified writer format
     * - Provide comprehensive logging and error handling
     *
     * @param cliArgs Validated command line arguments
     * @throws Exception if analysis execution fails
     */
    private static void executeAnalysis(CommandLineArgs cliArgs) throws Exception {
        log.info("Starting reachability analysis");
        log.info("Input: {}", cliArgs.getInputPath());
        log.info("Output: {}", cliArgs.getOutputFile());
        log.info("Mode: {}", cliArgs.isExtractOnly() ? "Extract Only" : "Full Analysis");

        // Create and configure appropriate extractor
        ApplicationExtractor extractor = createExtractor(cliArgs);

        // Execute analysis based on mode
        ReachabilityResult result;
        if (cliArgs.isExtractOnly()) {
            result = executeExtractOnly(cliArgs, extractor);
        } else {
            result = executeFullAnalysis(cliArgs, extractor);
        }

        // Write results
        writeResults(result, cliArgs);

        // Log summary
        logAnalysisSummary(result);
    }

    /**
     * Creates and configures appropriate extractor for the input file.
     * <p>
     * Uses factory pattern to create the correct extractor type and configures
     * it with user-provided parameters for the specific application format.
     *
     * @param cliArgs Command line arguments for configuration
     * @return Configured ApplicationExtractor
     * @throws Exception if extractor creation or configuration fails
     */
    private static ApplicationExtractor createExtractor(CommandLineArgs cliArgs) throws Exception {
        ApplicationExtractor extractor = ExtractorFactory.create(cliArgs.getInputPath());

        // Configure extractor based on type
        if (extractor instanceof AndroidExtractor) {
            AndroidExtractor androidExtractor = (AndroidExtractor) extractor;
            androidExtractor.setAndroidConfig(
                    cliArgs.getInputPath(),
                    cliArgs.getAndroidDir(),
                    cliArgs.getRtJar(),
                    cliArgs.getTimeout(),
                    cliArgs.isAppPackageOnly(),
                    cliArgs.getEntryPointTypesSet()
            );
        } else if (extractor instanceof JarExtractor) {
            JarExtractor jarExtractor = (JarExtractor) extractor;
            jarExtractor.setJarPath(cliArgs.getInputPath());
        }

        extractor.initialize(cliArgs);

        return extractor;
    }

    /**
     * Executes extract-only analysis workflow.
     * <p>
     * Performs lightweight analysis without call graph construction,
     * suitable for fast information extraction or direct call analysis
     * scenarios with minimal performance overhead.
     * <p>
     * ### Extract-Only Workflow:
     * - Basic application information extraction
     * - Optional direct call analysis if targets provided
     * - No call graph construction for performance optimization
     * - Result packaging for consistent output format
     *
     * @param cliArgs   Command line arguments
     * @param extractor Configured application extractor
     * @return ReachabilityResult containing extraction results
     * @throws Exception if extraction fails
     */
    private static ReachabilityResult executeExtractOnly(CommandLineArgs cliArgs,
                                                         ApplicationExtractor extractor) throws Exception {

        log.info("Executing extract-only analysis");

        AppInfo appInfo;

        if (cliArgs.getTargetsFile() != null) {
            // Extract with direct call analysis
            Set<String> targetSignatures = readTargetMethods(cliArgs.getTargetsFile());
            log.info("Performing direct call analysis with {} target methods", targetSignatures.size());
            appInfo = extractor.extractAppInfo(targetSignatures);
        } else {
            // Basic extraction only
            log.info("Performing basic information extraction");
            appInfo = extractor.extractAppInfo();
        }

        // Create result container
        ReachabilityResult result = new ReachabilityResult(appInfo);
        result.setExecutionTime(System.currentTimeMillis());

        log.info("Extract-only analysis completed");

        return result;
    }

    /**
     * Executes full reachability analysis workflow.
     * <p>
     * Performs comprehensive reachability analysis including call graph
     * construction, entry point extraction, and complete path discovery
     * for detailed analysis results with full reachability information.
     * <p>
     * ### Full Analysis Workflow:
     * - Call graph construction using application-specific algorithms
     * - Entry point extraction based on application type and configuration
     * - Target method resolution and validation
     * - Comprehensive reachability analysis with path discovery
     * - Result consolidation and performance metrics collection
     *
     * @param cliArgs   Command line arguments
     * @param extractor Configured application extractor
     * @return ReachabilityResult containing comprehensive analysis results
     * @throws Exception if analysis fails
     */
    private static ReachabilityResult executeFullAnalysis(CommandLineArgs cliArgs,
                                                          ApplicationExtractor extractor) throws Exception {

        log.info("Executing full reachability analysis");

        // Validate target methods file for full analysis
        if (cliArgs.getTargetsFile() == null) {
            throw new IllegalArgumentException("Target methods file required for full reachability analysis");
        }

        // Build call graph
        log.info("Building call graph...");
        CallGraph callGraph = extractor.buildCallGraph();

        // Extract entry points
        log.info("Extracting entry points...");
        Set<EntryPoint> entryPoints = extractor.extractEntryPoints();

        // Read and resolve target methods
        Set<String> targetSignatures = readTargetMethods(cliArgs.getTargetsFile());
        log.info("Resolving {} target methods...", targetSignatures.size());
        Set<SootMethod> targetMethods = extractor.resolveTargetMethods(targetSignatures);

        // Perform reachability analysis
        log.info("Performing reachability analysis...");
        ReachabilityAnalysis analysis = new ReachabilityAnalysis();

        ReachabilityResult result = analysis.analyze(
                callGraph,
                entryPoints,
                targetMethods,
                new SootReachabilityStrategy()
        );

        // Set analysis metrics
        result.setEntryPointCount(entryPoints.size());
        result.setTargetMethodCount(targetMethods.size());
        result.setExecutionTime(System.currentTimeMillis());

        log.info("Full reachability analysis completed");

        return result;
    }


    /**
     * Writes analysis results using specified writer format.
     * <p>
     * Creates appropriate writer based on user configuration and outputs
     * analysis results in the requested format with comprehensive error
     * handling and validation.
     *
     * @param result  Analysis results to write
     * @param cliArgs Command line arguments for output configuration
     * @throws Exception if result writing fails
     */
    private static void writeResults(ReachabilityResult result, CommandLineArgs cliArgs) throws Exception {
        log.info("Writing results to: {}", cliArgs.getOutputFile());

        Writer writer = WriterFactory.create(cliArgs.getWriterTypeEnum());
        File outputFile = new File(cliArgs.getOutputFile());

        writer.write(result, outputFile);

        log.info("Results written successfully");
    }

    /**
     * Reads target method signatures from file.
     * <p>
     * Parses target methods file to extract method signatures for reachability
     * analysis, supporting comments and empty lines for user convenience.
     * <p>
     * ### File Format Support:
     * - One method signature per line
     * - Comment lines starting with # or //
     * - Empty lines ignored
     * - Whitespace trimming for robustness
     *
     * @param fileName Path to target methods file
     * @return Set of method signatures
     * @throws IOException if file reading fails
     */
    private static Set<String> readTargetMethods(String fileName) throws IOException {
        Set<String> signatures = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Skip empty lines and comments
                if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("//")) {
                    signatures.add(line);
                }
            }
        }

        log.debug("Read {} target method signatures from {}", signatures.size(), fileName);

        return signatures;
    }

    /**
     * Logs comprehensive analysis summary for user feedback.
     * <p>
     * Provides detailed information about analysis results including
     * performance metrics, coverage statistics, and result counts
     * for user understanding and validation.
     *
     * @param result Analysis results to summarize
     */
    private static void logAnalysisSummary(ReachabilityResult result) {
        log.info("=== Analysis Summary ===");

        AppInfo appInfo = result.getAppInfo();
        if (appInfo != null) {
            log.info("Application: {} ({})", appInfo.getLabel(), appInfo.getType());
            log.info("Package: {}", appInfo.getPackageName());
            log.info("Classes analyzed: {}", appInfo.getClasses().size());

            int totalMethods = appInfo.getClasses().stream()
                    .mapToInt(clazz -> clazz.getMethods().size())
                    .sum();
            log.info("Methods analyzed: {}", totalMethods);
        }

        if (result.getEntryPointCount() > 0) {
            log.info("Entry points: {}", result.getEntryPointCount());
        }

        if (result.getTargetMethodCount() > 0) {
            log.info("Target methods: {}", result.getTargetMethodCount());
            log.info("Reachable methods: {}", result.getReachableMethodCount());
            log.info("Target-reaching methods: {}", result.getTargetReachingMethodCount());
        }

        if (result.getExecutionTime() > 0) {
            log.info("Execution time: {} seconds", result.getExecutionTime() / 1000.0);
        }

        log.info("========================");
    }
}