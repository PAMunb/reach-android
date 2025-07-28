package br.unb.cic.reach.android;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.apk.model.AndroidAppInfo;
import br.unb.cic.reach.apk.model.ComponentInfo;
import br.unb.cic.reach.apk.reader.AppReader;
import br.unb.cic.reach.apk.util.AndroidUtil;
import br.unb.cic.reach.common.extractor.ApplicationExtractor;
import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ApplicationType;
import br.unb.cic.reach.common.model.ComponentType;
import br.unb.cic.reach.common.model.EntryPoint;
import br.unb.cic.reach.common.model.ReachClass;
import br.unb.cic.reach.common.model.ReachMethod;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;

/**
 * Android-specific application extractor for APK analysis.
 * <p>
 * This extractor handles APK files using InfoflowAndroid for call graph
 * construction and ProcessManifest for component information extraction,
 * supporting both lightweight extraction and comprehensive reachability
 * analysis scenarios.
 * <p>
 * ### Architectural Decisions:
 * - InfoflowAndroid integration for robust call graph construction
 * - Component-based entry point extraction across all Android types
 * - Configurable analysis scope for performance optimization
 * - Direct call analysis for lightweight scenarios without call graph
 * <p>
 * ### Role in the System:
 * - Primary interface for Android APK processing and analysis
 * - Bridge between APK structure and generic analysis algorithms
 * - Foundation for Android-specific reachability computation
 * - Integration point for Android framework-aware analysis
 */
public class AndroidExtractor implements ApplicationExtractor {
    private static final Logger log = LoggerFactory.getLogger(AndroidExtractor.class);

    private String apkPath;
    private String androidPlatformsDir;
    private String rtJarPath;
    private int timeoutSeconds;
    private boolean appPackageOnly;
    private Set<ComponentType> entryPointTypes;

    @Override
    public void initialize(Object config) {
        // TODO: Extract configuration from proper CLI args when implemented
        // For now, these will be set directly by calling code
        log.debug("AndroidExtractor initialized");
    }

    /**
     * Sets Android-specific configuration parameters.
     * <p>
     * Temporary configuration method until CLI integration is completed.
     * Provides necessary parameters for Android APK analysis setup.
     *
     * @param apkPath             Path to the APK file to analyze
     * @param androidPlatformsDir Path to Android platforms directory
     * @param rtJarPath           Path to Java runtime JAR
     * @param timeoutSeconds      Analysis timeout in seconds
     * @param appPackageOnly      Whether to analyze only application package classes
     * @param entryPointTypes     Set of component types to consider as entry points
     */
    public void setAndroidConfig(String apkPath, String androidPlatformsDir, String rtJarPath,
                                 int timeoutSeconds, boolean appPackageOnly, Set<ComponentType> entryPointTypes) {
        this.apkPath = apkPath;
        this.androidPlatformsDir = androidPlatformsDir;
        this.rtJarPath = rtJarPath;
        this.timeoutSeconds = timeoutSeconds;
        this.appPackageOnly = appPackageOnly;
        this.entryPointTypes = entryPointTypes != null ? entryPointTypes : Set.of(ComponentType.values());
    }

    @Override
    public AppInfo extractAppInfo() {
        log.info("Extracting basic Android application information: {}", apkPath);

        try {
            // Initialize basic Soot for class loading
            SootConfig.initializeBasic(apkPath, androidPlatformsDir, rtJarPath);

            // Extract Android-specific information
            AndroidAppInfo androidAppInfo = AppReader.readApk(apkPath);

            // Convert to generic AppInfo
            AppInfo genericAppInfo = convertToGenericAppInfo(androidAppInfo, null);

            log.info("Basic extraction completed: {} components, {} classes",
                    androidAppInfo.getTotalComponentCount(), genericAppInfo.getClasses().size());

            return genericAppInfo;

        } catch (Exception e) {
            log.error("Error extracting basic Android app info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract Android application information", e);
        }
    }

    @Override
    public AppInfo extractAppInfo(Set<String> targetSignatures) {
        log.info("Extracting Android application information with direct call analysis: {}", apkPath);

        try {
            // Initialize basic Soot for method body analysis
            SootConfig.initializeBasic(apkPath, androidPlatformsDir, rtJarPath);

            // Extract Android-specific information
            AndroidAppInfo androidAppInfo = AppReader.readApk(apkPath);

            // Resolve target methods
            Set<SootMethod> targetMethods = resolveTargetMethodsBasic(targetSignatures);

            // Convert to generic AppInfo with direct call analysis
            AppInfo genericAppInfo = convertToGenericAppInfo(androidAppInfo, targetMethods);

            log.info("Direct call analysis completed: {} targets resolved, {} classes analyzed",
                    targetMethods.size(), genericAppInfo.getClasses().size());

            return genericAppInfo;

        } catch (Exception e) {
            log.error("Error extracting Android app info with direct calls: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract Android application information with direct call analysis", e);
        }
    }

    @Override
    public CallGraph buildCallGraph() {
        log.info("Building call graph for Android application: {}", apkPath);

        try {
            // Initialize InfoflowAndroid for comprehensive call graph construction
            SetupApplication infoflow = SootConfig.initialize(apkPath, androidPlatformsDir, rtJarPath, timeoutSeconds);

            log.info("Constructing call graph...");
            infoflow.constructCallgraph();

            CallGraph callGraph = Scene.v().getCallGraph();
            log.info("Call graph construction completed: {} edges", callGraph.size());

            return callGraph;

        } catch (Exception e) {
            log.error("Error building call graph: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build call graph for Android application", e);
        }
    }

    @Override
    public Set<EntryPoint> extractEntryPoints() {
        log.info("Extracting entry points for Android application: {}", apkPath);

        try {
            // Extract Android component information
            AndroidAppInfo androidAppInfo = AppReader.readApk(apkPath);

            // Filter components by configured types
            Set<ComponentInfo> filteredComponents = filterComponentsByType(androidAppInfo.getAllComponents());

            // Extract entry points from filtered components
            Set<EntryPoint> entryPoints = EntryPointExtractor.extractEntryPoints(filteredComponents, androidAppInfo);

            log.info("Entry point extraction completed: {} entry points from {} components",
                    entryPoints.size(), filteredComponents.size());

            if (log.isDebugEnabled()) {
                logEntryPointSummary(entryPoints);
            }

            return entryPoints;

        } catch (Exception e) {
            log.error("Error extracting entry points: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract entry points for Android application", e);
        }
    }

    @Override
    public Set<SootMethod> resolveTargetMethods(Set<String> signatures) {
        log.info("Resolving {} target method signatures", signatures.size());

        Set<SootMethod> sootMethods = new HashSet<>();
        int resolvedCount = 0;

        for (String signature : signatures) {
            try {
                SootMethod method = Scene.v().getMethod(signature);
                sootMethods.add(method);
                resolvedCount++;
                log.debug("Resolved target method: {}", signature);
            } catch (RuntimeException e) {
                log.warn("Could not resolve target method: {} - {}", signature, e.getMessage());
            }
        }

        log.info("Target method resolution completed: {}/{} methods resolved",
                resolvedCount, signatures.size());

        return sootMethods;
    }

    /**
     * Converts Android-specific AppInfo to generic AppInfo structure.
     * <p>
     * Transforms the Android-specific application information into the generic
     * format required by the analysis framework, including component type
     * mapping and optional direct call analysis for target methods.
     * <p>
     * ### Conversion Process:
     * - Basic application metadata transfer
     * - Component type mapping from Android to generic classifications
     * - Class and method structure analysis with Soot integration
     * - Optional direct call analysis for lightweight scenarios
     *
     * @param apkInfo       Android-specific application information
     * @param targetMethods Optional target methods for direct call analysis
     * @return Generic AppInfo suitable for analysis framework
     */
    private AppInfo convertToGenericAppInfo(AndroidAppInfo androidAppInfo,
                                            Set<SootMethod> targetMethods) {

        AppInfo genericAppInfo = new AppInfo(androidAppInfo.getPath());
        genericAppInfo.setType(ApplicationType.ANDROID);
        genericAppInfo.setPackageName(androidAppInfo.getPackageName());
        genericAppInfo.setAppName(androidAppInfo.getAppName());
        genericAppInfo.setLabel(androidAppInfo.getLabel());

        // Get application classes based on configuration
        List<SootClass> applicationClasses = getApplicationClasses(androidAppInfo);

        // Convert each class to ReachClass
        for (SootClass sootClass : applicationClasses) {
            ReachClass reachClass = createReachClass(sootClass, androidAppInfo);

            // Process methods in the class
            for (SootMethod sootMethod : sootClass.getMethods()) {
                ReachMethod reachMethod = new ReachMethod(sootMethod);

                // Perform direct call analysis if target methods provided
                if (targetMethods != null && !targetMethods.isEmpty()) {
                    analyzeDirectCalls(reachMethod, sootMethod, targetMethods);
                }

                reachClass.addMethod(reachMethod);
            }

            genericAppInfo.addClass(reachClass);
        }

        return genericAppInfo;
    }

    /**
     * Creates ReachClass from SootClass with Android component context.
     * <p>
     * Determines component type and main component status based on Android
     * component information, providing proper classification for analysis
     * algorithms and result reporting.
     *
     * @param sootClass The Soot class to convert
     * @param androidAppInfo   Android application information for context
     * @return ReachClass with proper Android component classification
     */
    private ReachClass createReachClass(SootClass sootClass, AndroidAppInfo androidAppInfo) {
        String className = sootClass.getName();

        // Determine component type based on Android components
        ComponentType componentType = determineComponentType(className, androidAppInfo);

        // Check if this is a main component
        boolean isMainComponent = isMainComponent(className, androidAppInfo);

        return new ReachClass(className, componentType, isMainComponent);
    }

    /**
     * Determines component type for a class based on Android component declarations.
     */
    private ComponentType determineComponentType(String className, AndroidAppInfo androidAppInfo) {
        // Check each component type for matching class
        for (ComponentInfo component : androidAppInfo.getAllComponents()) {
            if (className.equals(component.getName()) || className.startsWith(component.getName() + "$")) {
                return component.getComponentType();
            }
        }

        // Default to application class for non-component classes
        return ComponentType.APPLICATION_CLASS;
    }

    /**
     * Checks if a class represents a main component.
     */
    private boolean isMainComponent(String className, AndroidAppInfo androidAppInfo) {
        ComponentInfo mainComponent = androidAppInfo.getMainComponent();
        if (mainComponent != null) {
            return className.equals(mainComponent.getName()) ||
                    className.startsWith(mainComponent.getName() + "$");
        }
        return false;
    }

    /**
     * Gets application classes based on configuration.
     */
    private List<SootClass> getApplicationClasses(AndroidAppInfo androidAppInfo) {
        return Scene.v().getApplicationClasses().stream()
                .filter(clazz -> shouldIncludeClass(clazz, androidAppInfo))
                .toList();
    }

    /**
     * Determines if a class should be included based on configuration.
     */
    private boolean shouldIncludeClass(SootClass clazz, AndroidAppInfo androidAppInfo) {
        if (appPackageOnly) {
            return AndroidUtil.isClassInApplicationPackage(clazz, androidAppInfo);
        } else {
            return AndroidUtil.isApplicationClass(clazz);
        }
    }

    /**
     * Filters components by configured component types.
     */
    private Set<ComponentInfo> filterComponentsByType(Set<ComponentInfo> allComponents) {
        return allComponents.stream()
                .filter(component -> entryPointTypes.contains(component.getComponentType()))
                .collect(Collectors.toSet());
    }

    /**
     * Resolves target method signatures for basic analysis scenarios.
     */
    private Set<SootMethod> resolveTargetMethodsBasic(Set<String> signatures) {
        Set<SootMethod> methods = new HashSet<>();

        for (String signature : signatures) {
            try {
                SootMethod method = Scene.v().getMethod(signature);
                methods.add(method);
            } catch (RuntimeException e) {
                log.debug("Could not resolve method signature: {}", signature);
            }
        }

        return methods;
    }

    /**
     * Analyzes method for direct calls to target methods.
     * <p>
     * Examines method body to identify direct invocations of target methods,
     * providing lightweight reachability information without full call graph
     * construction for performance-optimized analysis scenarios.
     *
     * @param reachMethod   The ReachMethod to populate with results
     * @param sootMethod    The SootMethod to analyze
     * @param targetMethods Set of target methods to look for
     */
    private void analyzeDirectCalls(ReachMethod reachMethod, SootMethod sootMethod, Set<SootMethod> targetMethods) {
        if (!sootMethod.hasActiveBody()) {
            return;
        }

        try {
            for (Unit unit : sootMethod.getActiveBody().getUnits()) {
                if (unit instanceof Stmt) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        SootMethod calledMethod = invokeExpr.getMethod();

                        if (targetMethods.contains(calledMethod)) {
                            reachMethod.setDirectlyReachesTarget(true);
                            reachMethod.addReachableTarget(calledMethod.getSignature());
                            reachMethod.setReachesTarget(true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error analyzing direct calls in method {}: {}",
                    sootMethod.getSignature(), e.getMessage());
        }
    }

    /**
     * Logs entry point summary for debugging and monitoring.
     */
    private void logEntryPointSummary(Set<EntryPoint> entryPoints) {
        log.debug("Entry Point Summary:");
        entryPoints.stream()
                .collect(Collectors.groupingBy(EntryPoint::getComponentType, Collectors.counting()))
                .forEach((type, count) -> log.debug("  {} entries of type {}", count, type));
    }
}