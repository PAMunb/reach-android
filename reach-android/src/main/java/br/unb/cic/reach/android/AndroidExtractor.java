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
import soot.*;
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
        if (config instanceof br.unb.cic.reach.common.model.ConfigMatrix) {
            br.unb.cic.reach.common.model.ConfigMatrix matrix = (br.unb.cic.reach.common.model.ConfigMatrix) config;
            this.apkPath = matrix.getInputPath();
            this.androidPlatformsDir = matrix.getAndroidPlatformsDir();
            this.rtJarPath = matrix.getRtJarPath();
            this.timeoutSeconds = matrix.getTimeoutSeconds();
            this.appPackageOnly = matrix.isAppPackageOnly();
            this.entryPointTypes = matrix.getEntryPointTypes();
            
            log.debug("AndroidExtractor initialized with ConfigMatrix: apk={}, androidDir={}, rtJar={}", 
                    apkPath, androidPlatformsDir, rtJarPath);
        } else {
            log.debug("AndroidExtractor initialized without proper ConfigMatrix configuration");
        }
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
            // CRITICAL FIX: DO NOT re-initialize Soot here!
            // When called after buildCallGraph(), Soot is already initialized with InfoflowAndroid
            // and classes are properly classified as ApplicationClass. Re-initializing with
            // initializeBasic() causes classes to lose their ApplicationClass status, leading
            // to incorrect application method classification in reachability analysis.
            //
            // The Soot scene is already available from the previous call graph construction,
            // so we can directly use it without re-initialization.
            
            // SootConfig.initializeBasic(apkPath, androidPlatformsDir, rtJarPath); // REMOVED!

            // Extract Android-specific information
            AndroidAppInfo androidAppInfo = AppReader.readApk(apkPath);

            // Convert to generic AppInfo using already-initialized Soot scene
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
            // CRITICAL FIX: DO NOT re-initialize Soot here either!
            // Same issue as extractAppInfo() - when called after buildCallGraph(), Soot is already
            // properly initialized and re-initialization causes loss of ApplicationClass status.
            
            // SootConfig.initializeBasic(apkPath, androidPlatformsDir, rtJarPath); // REMOVED!

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
        System.out.println("DEBUG_REACH: AndroidExtractor.extractEntryPoints() starting"); // DEBUG_REACH

        try {
            // Extract Android component information
            AndroidAppInfo androidAppInfo = AppReader.readApk(apkPath);
            System.out.println("DEBUG_REACH: AndroidAppInfo loaded, total components: " + androidAppInfo.getAllComponents().size()); // DEBUG_REACH

            // Filter components by configured types
            Set<ComponentInfo> filteredComponents = filterComponentsByType(androidAppInfo.getAllComponents());
            System.out.println("DEBUG_REACH: Filtered components: " + filteredComponents.size()); // DEBUG_REACH
            System.out.println("DEBUG_REACH: Entry point types configured: " + entryPointTypes); // DEBUG_REACH

            // Extract entry points from filtered components
            Set<EntryPoint> entryPoints = EntryPointExtractor.extractEntryPoints(filteredComponents, androidAppInfo);
            System.out.println("DEBUG_REACH: Entry points extracted: " + entryPoints.size()); // DEBUG_REACH

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
        System.out.println("DEBUG_REACH: AndroidExtractor.resolveTargetMethods() starting with " + signatures.size() + " signatures"); // DEBUG_REACH

        Set<SootMethod> sootMethods = new HashSet<>();
        int resolvedCount = 0;

        for (String signature : signatures) {
            try {
                SootMethod method = Scene.v().getMethod(signature);
                sootMethods.add(method);
                resolvedCount++;
                log.debug("Resolved target method: {}", signature);
                System.out.println("DEBUG_REACH: Resolved target: " + signature); // DEBUG_REACH
            } catch (RuntimeException e) {
                log.warn("Could not resolve target method: {} - {}", signature, e.getMessage());
                System.out.println("DEBUG_REACH: Failed to resolve target: " + signature + " - " + e.getMessage()); // DEBUG_REACH
            }
        }

        log.info("Target method resolution completed: {}/{} methods resolved",
                resolvedCount, signatures.size());
        System.out.println("DEBUG_REACH: Target method resolution completed: " + resolvedCount + "/" + signatures.size() + " resolved"); // DEBUG_REACH

        return sootMethods;
    }

    /**
     * Converts Android-specific AppInfo to generic AppInfo structure.
     * <p>
     * This method serves as the architectural bridge between Android-specific component
     * analysis (APK structure, manifest parsing) and the generic reachability analysis
     * framework. The conversion handles the complex mapping from Android component
     * concepts to the unified analysis model required by ConfigMatrix-driven algorithms.
     * <p>
     * ### Architectural Role in ConfigMatrix System:
     * The targetMethods parameter directly implements the ConfigMatrix decision tree:
     * - NULL targetMethods: Extract-only mode where no reachability computation occurs
     * - NON-NULL targetMethods: Full analysis mode requiring direct call detection
     * This design ensures that method-level analysis behavior is determined by the
     * higher-level configuration matrix rather than hardcoded extraction logic.
     * <p>
     * ### Component Type Mapping Strategy:
     * Android components (Activity, Service, BroadcastReceiver, ContentProvider) are
     * mapped to generic ComponentType enums, preserving semantic meaning while enabling
     * framework-agnostic analysis algorithms. This abstraction is critical for supporting
     * future analysis of other platforms (JAR, native binaries) through the same pipeline.
     * <p>
     * ### Performance Implications:
     * The optional direct call analysis represents a significant architectural decision:
     * when targetMethods is provided, this method performs lightweight static analysis
     * of method bodies without full call graph construction. This enables the ConfigMatrix
     * "extract-only + targets" mode to provide meaningful results with minimal overhead,
     * supporting rapid security screening and API usage detection workflows.
     *
     * @param androidAppInfo Android-specific application information extracted from APK
     * @param targetMethods  Optional target methods for direct call analysis; null in extract-only mode
     * @return Generic AppInfo structure compatible with ConfigMatrix-driven analysis pipeline
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
        System.out.println("Application classes: " + applicationClasses.size());
        
        // ConfigMatrix-driven target method handling: null indicates extract-only mode
        // where reachability computation is disabled but structure extraction continues
        if (targetMethods != null) {
            System.out.println("Target methods: " + targetMethods.size() + " (direct call analysis enabled)");
        } else {
            System.out.println("Target methods: none (extract-only mode - no reachability computation)");
        }

        // Convert each class to ReachClass with Android component context preservation
        for (SootClass sootClass : applicationClasses) {
            ReachClass reachClass = createReachClass(sootClass, androidAppInfo);
//            System.out.println("ReachClass: " + reachClass);

            // Process methods in the class with ConfigMatrix-aware analysis
            for (SootMethod sootMethod : sootClass.getMethods()) {
                ReachMethod reachMethod = new ReachMethod(sootMethod);
//                System.out.println("\t ReachMethod: " + reachMethod);

                // ConfigMatrix Integration Point: Direct call analysis is conditionally executed
                // based on the analysis mode determined by ConfigMatrix. This architectural
                // decision enables the system to support both lightweight extraction (for
                // structural analysis) and targeted API usage detection (for security analysis)
                // without requiring full call graph construction in either case.
                //
                // The conditional execution here directly implements the ConfigMatrix decision:
                // - Extract-only mode (targetMethods == null): Skip analysis, preserve structure
                // - Extract-with-targets mode (targetMethods != null): Perform direct call detection
                // - Full analysis mode: Delegated to ReachabilityAnalysis with call graph
                if (targetMethods != null && !targetMethods.isEmpty()) {
                    // Direct call analysis: examine method bodies for immediate invocations
                    // of target methods without constructing intermediate call graph representation.
                    // This provides O(1) per-method analysis complexity rather than O(N*M) 
                    // graph traversal, enabling rapid security screening workflows.
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
     * @param sootClass      The Soot class to convert
     * @param androidAppInfo Android application information for context
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
                log.warn("Could not resolve method signature: {}", signature);
            }
        }

        return methods;
    }

    /**
     * Analyzes method for direct calls to target methods, with simple indirect detection.
     * 
     * This method examines method bodies for immediate invocations of target methods
     * and, if none found, performs limited recursive analysis through application methods.
     * Maintains the original working direct call detection while adding minimal
     * indirect call capability.
     *
     * @param reachMethod   The ReachMethod to populate with call analysis results
     * @param sootMethod    The SootMethod to analyze for target invocations
     * @param targetMethods Set of target methods to detect in method body
     */
    private void analyzeDirectCalls(ReachMethod reachMethod, SootMethod sootMethod, Set<SootMethod> targetMethods) {
        Body body;
        if (sootMethod.hasActiveBody()) {
            body = sootMethod.getActiveBody();
        } else {
            try {
                body = sootMethod.retrieveActiveBody();
            } catch (Exception e) {
                log.debug("Could not retrieve body for method {}: {}", 
                         sootMethod.getSignature(), e.getMessage());
                return;
            }
        }
        
        try {
            // Phase 1: Direct call detection (original working logic)
            boolean foundInvoke = false;
            for (Unit unit : body.getUnits()) {
                if (unit instanceof Stmt) {
                    Stmt stmt = (Stmt) unit;
                    if (stmt.containsInvokeExpr()) {
                        foundInvoke = true;
                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        SootMethod calledMethod = invokeExpr.getMethod();
                        
                        if (sootMethod.getDeclaringClass().getName().contains("MessageDigestUtil")) {
                            System.out.println("\t\tMethod: " + sootMethod.getSignature());
                            System.out.println("\t\t\tCall to: " + calledMethod.getSignature());
                            System.out.println("\t\t\tTarget methods size: " + targetMethods.size());
                            System.out.println("\t\t\tContains? " + targetMethods.contains(calledMethod));
                        }
                        
                        if (targetMethods.contains(calledMethod)) {
                            System.out.println("\t\t\tTARGET : " + calledMethod.getSignature());
                            reachMethod.setDirectlyReachesTarget(true);
                            reachMethod.addReachableTarget(calledMethod.getSignature());
                            reachMethod.setReachesTarget(true);
                        }
                    }
                }
            }
            
            if (sootMethod.getDeclaringClass().getName().contains("MessageDigestUtil") && !foundInvoke) {
                System.out.println("\t\tMethod: " + sootMethod.getSignature() + " - NO INVOKE EXPRESSIONS FOUND");
                System.out.println("\t\t\tBody units: " + body.getUnits().size());
            }
            
            // Phase 2: Simple indirect call detection (only if no direct calls found)
            if (!reachMethod.isReachesTarget()) {
                if (sootMethod.getName().equals("unreachableHash")) {
                    System.out.println("DEBUG_UNREACHABLE: Starting indirect analysis for unreachableHash");
                }
                analyzeIndirectCalls(reachMethod, sootMethod, targetMethods, body);
                if (sootMethod.getName().equals("unreachableHash")) {
                    System.out.println("DEBUG_UNREACHABLE: After indirect analysis - reachesTarget=" + reachMethod.isReachesTarget() + ", targets=" + reachMethod.getReachableTargets().size());
                }
            } else if (sootMethod.getName().equals("unreachableHash")) {
                System.out.println("DEBUG_UNREACHABLE: Skipping indirect analysis - already reachesTarget=true");
            }
            
        } catch (Exception e) {
            log.debug("Error analyzing direct calls in method {}: {}", sootMethod.getSignature(), e.getMessage());
        }
    }

    /**
     * Simple indirect call analysis for application methods only.
     * 
     * Checks if this method calls other application methods that reach targets.
     * Limited to one level of indirection to keep analysis simple and fast.
     * 
     * @param reachMethod The ReachMethod to populate with results
     * @param sootMethod The containing method being analyzed
     * @param targetMethods Set of target methods to detect
     * @param body The method body to analyze
     */
    private void analyzeIndirectCalls(ReachMethod reachMethod, SootMethod sootMethod, 
                                    Set<SootMethod> targetMethods, Body body) {
        
        if (sootMethod.getDeclaringClass().getName().contains("MessageDigestUtil")) {
            System.out.println("\t\tIndirect analysis in method: " + sootMethod.getSignature());
        }
        
        // Check each method call for indirect target access
        for (Unit unit : body.getUnits()) {
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                if (stmt.containsInvokeExpr()) {
                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    SootMethod calledMethod = invokeExpr.getMethod();
                    
                    // Skip direct target calls (already handled)
                    if (targetMethods.contains(calledMethod)) {
                        continue;
                    }
                    
                    // Only check application methods to avoid system/library explosion
                    if (isApplicationMethod(calledMethod)) {
                        
                        if (sootMethod.getDeclaringClass().getName().contains("MessageDigestUtil")) {
                            System.out.println("\t\t\tChecking indirect call to: " + calledMethod.getSignature());
                        }
                        
                        // Check if this application method directly calls targets
                        Body calledMethodBody = getMethodBody(calledMethod);
                        if (calledMethodBody != null) {
                            ReachMethod tempReachMethod = new ReachMethod(calledMethod);
                            analyzeDirectCallsInBody(calledMethodBody, calledMethod, targetMethods, tempReachMethod);
                            
                            if (tempReachMethod.isReachesTarget()) {
                                System.out.println("\t\t\tINDIRECT TARGET CALL through: " + calledMethod.getSignature());
                                reachMethod.setReachesTarget(true);
                                for (String targetSignature : tempReachMethod.getReachableTargets()) {
                                    reachMethod.addReachableTarget(targetSignature);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Simple direct call analysis on a method body.
     */
    private void analyzeDirectCallsInBody(Body body, SootMethod sootMethod, 
                                        Set<SootMethod> targetMethods, ReachMethod reachMethod) {
        for (Unit unit : body.getUnits()) {
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
    }

    /**
     * Checks if a method belongs to application code.
     */
    private boolean isApplicationMethod(SootMethod method) {
        if (method == null || method.getDeclaringClass() == null) {
            return false;
        }
        
        SootClass declaringClass = method.getDeclaringClass();
        String className = declaringClass.getName();
        
        return declaringClass.isApplicationClass() && 
               !className.startsWith("java.") &&
               !className.startsWith("javax.") &&
               !className.startsWith("android.") &&
               !className.startsWith("androidx.") &&
               !className.startsWith("com.google.android.");
    }

    /**
     * Safely retrieves method body handling Soot's lazy loading.
     */
    private Body getMethodBody(SootMethod method) {
        if (method == null) {
            return null;
        }
        
        try {
            if (method.hasActiveBody()) {
                return method.getActiveBody();
            } else {
                return method.retrieveActiveBody();
            }
        } catch (Exception e) {
            return null;
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