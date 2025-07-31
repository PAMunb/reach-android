package br.unb.cic.reach.android;

import java.io.File;
import java.util.Collections;

import br.unb.cic.reach.common.model.ConfigMatrix;
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
 * <p>
 * This class provides standardized Soot initialization for Android APK analysis,
 * supporting both basic class loading scenarios and comprehensive call graph
 * construction using InfoflowAndroid framework for accurate reachability analysis.
 * <p>
 * ### Architectural Decisions:
 * - Centralized Soot configuration for consistency across analysis scenarios
 * - InfoflowAndroid integration for robust Android-specific call graph construction
 * - Configurable timeout and analysis parameters for different use cases
 * - Memory and performance optimization for large APK analysis
 * <p>
 * ### Role in the System:
 * - Foundation for all Android-specific Soot initialization
 * - Integration point with InfoflowAndroid framework
 * - Performance and memory configuration for analysis scalability
 * - Standardized setup for consistent analysis results
 */
public class SootConfig {
    private static final Logger log = LoggerFactory.getLogger(SootConfig.class);

    private static InfoflowAndroidConfiguration currentConfig;

    /**
     * Initializes Soot with InfoflowAndroid using custom timeout.
     * <p>
     * Provides timeout configuration for analysis scenarios with different
     * performance requirements, enabling analysis customization based on
     * application complexity and available computational resources.
     *
     * @param config Configuration matrix
     * @return SetupApplication configured for call graph construction
     */
    public static SetupApplication initialize(ConfigMatrix config) {
        log.info("Initializing Soot for Android analysis: {}", new File(config.getInputPath()).getName());

        // Initialize basic Soot environment
        initializeSoot(config);

        // Configure InfoflowAndroid
        currentConfig = createInfoflowConfiguration(config);

        log.debug("Creating InfoflowAndroid SetupApplication");
        return new SetupApplication(currentConfig);
    }

    /**
     * Returns the current InfoflowAndroid configuration.
     * <p>
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
     * <p>
     * Implements fundamental Soot setup for Android APK analysis including
     * classpath configuration, APK processing options, and analysis parameters
     * optimized for Android application structure and framework integration.
     */
    private static void initializeSoot(ConfigMatrix config) {
        log.debug("Core Soot initialization");
        log.debug("APK: {}", config.getInputPath());
        log.debug("Android platforms: {}", config.getAndroidPlatformsDir());
        log.debug("RT JAR: {}", config.getRtJarPath());

        // Reset Soot for clean initialization
        G.reset();

        // Basic Soot configuration
        Options.v().set_full_resolver(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_validate(true);
        Options.v().set_output_format(Options.output_format_none);

        // Android-specific configuration
        Options.v().set_process_dir(Collections.singletonList(config.getInputPath()));
        Options.v().set_android_jars(config.getAndroidPlatformsDir());
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_multiple_dex(true);

        // Classpath configuration
        String classpath = config.getAndroidPlatformsDir() + File.pathSeparatorChar + config.getRtJarPath();
        Options.v().set_soot_classpath(classpath);

        // Call graph configuration
        Options.v().setPhaseOption("cg", "all-reachable");
        configureSootCallgraph(config);


        // Load necessary classes
        Scene.v().loadNecessaryClasses();

        log.debug("Soot initialization completed");
    }


    /**
     * Creates InfoflowAndroid configuration for comprehensive analysis.
     * <p>
     * Configures InfoflowAndroid with optimized parameters for reachability
     * analysis, balancing analysis precision with performance requirements
     * for practical APK analysis workflows.
     */
    private static InfoflowAndroidConfiguration createInfoflowConfiguration(ConfigMatrix matrix) {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();

        // Basic file configuration
        config.getAnalysisFileConfig().setTargetAPKFile(matrix.getInputPath());
        config.getAnalysisFileConfig().setAndroidPlatformDir(matrix.getAndroidPlatformsDir());

        // Callback analysis configuration
        config.getCallbackConfig().setEnableCallbacks(true);
        config.getCallbackConfig().setCallbackAnalyzer(CallbackAnalyzer.Default);
        config.getCallbackConfig().setCallbackAnalysisTimeout(matrix.getTimeoutSeconds());
        config.getCallbackConfig().setMaxAnalysisCallbackDepth(matrix.getMaxAnalysisCallbackDepth());

        // Analysis algorithm configuration
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        System.out.println(">>>>>> getInfoflowCallgraphAlgorithm(matrix)"+getInfoflowCallgraphAlgorithm(matrix));
        config.setCallgraphAlgorithm(getInfoflowCallgraphAlgorithm(matrix));
//        config.setAliasingAlgorithm(getAliasingAlgorithm(matrix));

        // Android-specific configuration
        config.setMergeDexFiles(true);
        config.setEnableTypeChecking(true);
        config.setEnableReflection(true);
        config.setEnableExceptionTracking(true);
        config.setEnableOriginalNames(true);
        config.setEnableLineNumbers(true);

        // Performance configuration
        config.setTaintAnalysisEnabled(false); // Disabled for reachability-only analysis
        config.setDataFlowTimeout(matrix.getTimeoutSeconds());
        config.setMaxThreadNum(Math.min(32, Runtime.getRuntime().availableProcessors() * 2));
        config.setSootIntegrationMode(InfoflowConfiguration.SootIntegrationMode.UseExistingInstance);

        log.debug("InfoflowAndroid configuration created with {} second timeout", matrix.getTimeoutSeconds());

        return config;
    }

    private static InfoflowConfiguration.AliasingAlgorithm getAliasingAlgorithm(ConfigMatrix matrix) {
        return switch (matrix.getAliasingAlgorithm()) {
            case FlowSensitive -> InfoflowConfiguration.AliasingAlgorithm.FlowSensitive;
            case PtsBased -> InfoflowConfiguration.AliasingAlgorithm.PtsBased;
            case Lazy -> InfoflowConfiguration.AliasingAlgorithm.Lazy;
            case None -> InfoflowConfiguration.AliasingAlgorithm.None;
        };
    }

    private static InfoflowConfiguration.CallgraphAlgorithm getInfoflowCallgraphAlgorithm(ConfigMatrix matrix) {
        return switch (matrix.getCallGraphAlgorithm()) {
            case SPARK -> InfoflowConfiguration.CallgraphAlgorithm.SPARK;
            case CHA -> InfoflowConfiguration.CallgraphAlgorithm.CHA;
            case RTA -> InfoflowConfiguration.CallgraphAlgorithm.RTA;
            case VTA -> InfoflowConfiguration.CallgraphAlgorithm.VTA;
            case GEOM -> InfoflowConfiguration.CallgraphAlgorithm.GEOM;
        };
    }

    private static void configureSootCallgraph(ConfigMatrix config) {
        String spark = "cg.spark";
        switch (config.getCallGraphAlgorithm()) {
            case CHA:
                Options.v().setPhaseOption("cg.cha", "on");
                Options.v().setPhaseOption(spark, "off");
                Options.v().setPhaseOption("cg.paddle", "off");
                break;

            case RTA:
                Options.v().setPhaseOption(spark, "on");
                Options.v().setPhaseOption(spark, "rta:true");
                Options.v().setPhaseOption(spark, "verbose:false");
                break;

            case VTA:
                Options.v().setPhaseOption(spark, "on");
                Options.v().setPhaseOption(spark, "vta:true");
                Options.v().setPhaseOption(spark, "verbose:false");
                break;

            case SPARK:
                Options.v().setPhaseOption(spark, "on");
                Options.v().setPhaseOption(spark, "verbose:false");
                Options.v().setPhaseOption(spark, "on-fly-cg:true");
                Options.v().setPhaseOption(spark, "field-based:false");
                break;

            case GEOM:
                Options.v().setPhaseOption(spark, "on");
                Options.v().setPhaseOption(spark, "geom-pta:true");
                Options.v().setPhaseOption(spark, "verbose:false");
                Options.v().setPhaseOption(spark, "geom-encoding:Geom");
                Options.v().setPhaseOption(spark, "geom-worklist:PQ");
                break;
        }
    }
}