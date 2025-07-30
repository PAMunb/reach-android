package br.unb.cic.reach.android;

import java.io.File;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.G;
import soot.Scene;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackAnalyzer;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;

/**
 * Soot configuration utility for Android application analysis.
 *
 * This class provides standardized Soot initialization for Android APK analysis,
 * supporting both basic class loading scenarios and comprehensive call graph
 * construction using InfoflowAndroid framework for accurate reachability analysis.
 *
 * ### Architectural Decisions:
 * - Centralized Soot configuration for consistency across analysis scenarios
 * - InfoflowAndroid integration for robust Android-specific call graph construction
 * - Configurable timeout and analysis parameters for different use cases
 * - Memory and performance optimization for large APK analysis
 *
 * ### Role in the System:
 * - Foundation for all Android-specific Soot initialization
 * - Integration point with InfoflowAndroid framework
 * - Performance and memory configuration for analysis scalability
 * - Standardized setup for consistent analysis results
 */
public class SootConfig {
    private static final Logger log = LoggerFactory.getLogger(SootConfig.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes

    private static InfoflowAndroidConfiguration currentConfig;

    /**
     * Initializes Soot with InfoflowAndroid for comprehensive call graph construction.
     *
     * Configures complete Soot environment with InfoflowAndroid for accurate
     * Android call graph construction, including callback analysis and framework
     * integration for comprehensive reachability analysis.
     *
     * ### Configuration Details:
     * - Callback analysis enabled with configurable timeout
     * - SPARK call graph algorithm for precision
     * - Exception tracking and reflection support
     * - Memory optimization for large application analysis
     *
     * @param apkPath Path to the APK file to analyze
     * @param androidPlatformsDir Path to Android platforms directory
     * @param rtJarPath Path to Java runtime JAR file
     * @return SetupApplication configured for call graph construction
     */
    public static SetupApplication initialize(String apkPath, String androidPlatformsDir, String rtJarPath) {
        return initialize(apkPath, androidPlatformsDir, rtJarPath, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Initializes Soot with InfoflowAndroid using custom timeout.
     *
     * Provides timeout configuration for analysis scenarios with different
     * performance requirements, enabling analysis customization based on
     * application complexity and available computational resources.
     *
     * @param apkPath Path to the APK file to analyze
     * @param androidPlatformsDir Path to Android platforms directory
     * @param rtJarPath Path to Java runtime JAR file
     * @param timeoutSeconds Analysis timeout in seconds
     * @return SetupApplication configured for call graph construction
     */
    public static SetupApplication initialize(String apkPath, String androidPlatformsDir,
                                              String rtJarPath, int timeoutSeconds) {

        log.info("Initializing Soot for Android analysis: {}", new File(apkPath).getName());

        // Initialize basic Soot environment
        initializeSoot(apkPath, androidPlatformsDir, rtJarPath);

        // Configure InfoflowAndroid
        currentConfig = createInfoflowConfiguration(apkPath, androidPlatformsDir, timeoutSeconds);

        log.debug("Creating InfoflowAndroid SetupApplication");
        SetupApplication setupApp = new SetupApplication(currentConfig);

        return setupApp;
    }

    /**
     * Initializes basic Soot environment for lightweight analysis scenarios.
     *
     * Configures minimal Soot environment for scenarios that require class loading
     * and basic method analysis without full call graph construction, such as
     * direct call analysis and component extraction.
     *
     * ### Use Cases:
     * - Component information extraction from manifest
     * - Direct method call analysis without call graph
     * - Method signature resolution and validation
     * - Lightweight class structure analysis
     *
     * @param apkPath Path to the APK file to analyze
     * @param androidPlatformsDir Path to Android platforms directory
     * @param rtJarPath Path to Java runtime JAR file
     */
    public static void initializeBasic(String apkPath, String androidPlatformsDir, String rtJarPath) {
        log.debug("Initializing basic Soot for Android: {}", new File(apkPath).getName());
        initializeSoot(apkPath, androidPlatformsDir, rtJarPath);
    }

    /**
     * Returns the current InfoflowAndroid configuration.
     *
     * Provides access to the current configuration for analysis introspection
     * and parameter validation in advanced analysis scenarios.
     *
     * @return Current InfoflowAndroidConfiguration or null if not initialized
     */
    public static InfoflowAndroidConfiguration getCurrentConfig() {
        return currentConfig;
    }

    /**
     * Core Soot initialization with Android-specific configuration.
     *
     * Implements fundamental Soot setup for Android APK analysis including
     * classpath configuration, APK processing options, and analysis parameters
     * optimized for Android application structure and framework integration.
     */
    private static void initializeSoot(String apkPath, String androidPlatformsDir, String rtJarPath) {
        log.debug("Core Soot initialization");
        log.debug("APK: {}", apkPath);
        log.debug("Android platforms: {}", androidPlatformsDir);
        log.debug("RT JAR: {}", rtJarPath);

        // Reset Soot for clean initialization
        G.reset();

        // Basic Soot configuration
        Options.v().set_full_resolver(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_validate(true);
        Options.v().set_output_format(Options.output_format_none);

        // Android-specific configuration
        Options.v().set_process_dir(Collections.singletonList(apkPath));
        Options.v().set_android_jars(androidPlatformsDir);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_multiple_dex(true);

        // Classpath configuration
        String classpath = androidPlatformsDir + File.pathSeparatorChar + rtJarPath;
        Options.v().set_soot_classpath(classpath);

        // Call graph configuration
        Options.v().setPhaseOption("cg", "all-reachable");
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "verbose:false");

        // Load necessary classes
        Scene.v().loadNecessaryClasses();

        log.debug("Soot initialization completed");
    }

    /**
     * Creates InfoflowAndroid configuration for comprehensive analysis.
     *
     * Configures InfoflowAndroid with optimized parameters for reachability
     * analysis, balancing analysis precision with performance requirements
     * for practical APK analysis workflows.
     */
    private static InfoflowAndroidConfiguration createInfoflowConfiguration(String apkPath,
                                                                            String androidPlatformsDir,
                                                                            int timeoutSeconds) {

        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();

        // Basic file configuration
//        config.getAnalysisFileConfig().setTargetAPKFile(new File(apkPath));
//        config.getAnalysisFileConfig().setAndroidPlatformDir(new File(androidPlatformsDir));
        config.getAnalysisFileConfig().setTargetAPKFile(apkPath);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidPlatformsDir);

        // Callback analysis configuration
        config.getCallbackConfig().setEnableCallbacks(true);
        config.getCallbackConfig().setCallbackAnalyzer(CallbackAnalyzer.Default);
        config.getCallbackConfig().setCallbackAnalysisTimeout(timeoutSeconds);
        config.getCallbackConfig().setMaxAnalysisCallbackDepth(10);

        // Analysis algorithm configuration
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);

        // Android-specific configuration
        config.setMergeDexFiles(true);
        config.setEnableTypeChecking(true);
        config.setEnableReflection(true);
        config.setEnableExceptionTracking(true);
        config.setEnableOriginalNames(true);
        config.setEnableLineNumbers(true);

        // Performance configuration
        config.setTaintAnalysisEnabled(false); // Disabled for reachability-only analysis
        config.setDataFlowTimeout(timeoutSeconds);
        config.setMaxThreadNum(Math.min(32, Runtime.getRuntime().availableProcessors() * 2));
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);

        log.debug("InfoflowAndroid configuration created with {} second timeout", timeoutSeconds);

        return config;
    }

    /**
     * Validates Soot initialization state.
     *
     * Checks if Soot has been properly initialized and is ready for analysis
     * operations, providing clear error information for debugging configuration issues.
     *
     * @return true if Soot is properly initialized, false otherwise
     */
    public static boolean isSootInitialized() {
        try {
            Scene scene = Scene.v();
            return scene != null && !scene.getApplicationClasses().isEmpty();
        } catch (Exception e) {
            log.debug("Soot initialization check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Provides memory usage information for analysis monitoring.
     *
     * Returns current memory usage statistics for analysis performance
     * monitoring and optimization in large-scale APK analysis scenarios.
     *
     * @return String containing memory usage information
     */
    public static String getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        return String.format("Memory: Used=%dMB, Free=%dMB, Total=%dMB, Max=%dMB",
                usedMemory / (1024 * 1024),
                freeMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024));
    }
}