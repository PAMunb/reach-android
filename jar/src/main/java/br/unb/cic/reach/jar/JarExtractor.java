package br.unb.cic.reach.jar;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.common.extractor.ApplicationExtractor;
import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ApplicationType;
import br.unb.cic.reach.common.model.ComponentType;
import br.unb.cic.reach.common.model.EntryPoint;
import br.unb.cic.reach.common.model.ReachClass;
import br.unb.cic.reach.common.model.ReachMethod;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

/**
 * JAR-specific application extractor for Java applications.
 *
 * This extractor handles JAR files using standard Soot configuration with
 * SPARK call graph algorithm, supporting automatic main() method detection
 * and custom entry point specification for library analysis.
 *
 * ### Architectural Decisions:
 * - Standard Soot configuration for Java application analysis
 * - Automatic main() method detection for executable JARs
 * - Custom entry point support for library analysis scenarios
 * - SPARK call graph algorithm for comprehensive analysis
 *
 * ### Role in the System:
 * - JAR file processing and analysis foundation
 * - Java application entry point identification
 * - Generic Java class analysis and reachability computation
 * - Integration point for Java-specific analysis workflows
 */
public class JarExtractor implements ApplicationExtractor {
    private static final Logger log = LoggerFactory.getLogger(JarExtractor.class);
    
    private String jarPath;
    private Object config;
    
    @Override
    public void initialize(Object config) {
        this.config = config;
        // TODO: Extract jarPath from config when CLI is implemented
        // For now, this will be set by the calling code
    }
    
    /**
     * Sets the JAR path for analysis.
     * 
     * Temporary method until CLI configuration is implemented.
     * 
     * @param jarPath Path to the JAR file to analyze
     */
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }
    
    @Override
    public AppInfo extractAppInfo() {
        log.info("Extracting basic JAR information: {}", jarPath);
        
        // Initialize basic Soot for class loading
        initializeBasicSoot();
        
        AppInfo appInfo = new AppInfo(jarPath);
        appInfo.setType(ApplicationType.JAR);
        
        // Detect main package
        String mainPackage = detectMainPackage();
        appInfo.setPackageName(mainPackage);
        
        // Process all application classes
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            ReachClass reachClass = createReachClass(clazz);
            appInfo.addClass(reachClass);
        }
        
        log.info("JAR analysis completed: {} classes processed", appInfo.getClasses().size());
        return appInfo;
    }
    
    @Override
    public AppInfo extractAppInfo(Set<String> targetSignatures) {
        log.info("Extracting JAR information with direct call analysis: {}", jarPath);
        
        // Initialize basic Soot for method body analysis
        initializeBasicSoot();
        
        AppInfo appInfo = new AppInfo(jarPath);
        appInfo.setType(ApplicationType.JAR);
        
        // Resolve target methods
        Set<SootMethod> targetMethods = resolveTargetMethodsBasic(targetSignatures);
        
        // Detect main package
        String mainPackage = detectMainPackage();
        appInfo.setPackageName(mainPackage);
        
        // Process all application classes with direct call analysis
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            ReachClass reachClass = createReachClass(clazz);
            
            // Analyze each method for direct calls to targets
            for (SootMethod method : clazz.getMethods()) {
                ReachMethod reachMethod = new ReachMethod(method);
                analyzeDirectCalls(reachMethod, method, targetMethods);
                reachClass.addMethod(reachMethod);
            }
            
            appInfo.addClass(reachClass);
        }
        
        log.info("JAR analysis with direct calls completed: {} classes processed", appInfo.getClasses().size());
        return appInfo;
    }
    
    @Override
    public CallGraph buildCallGraph() {
        log.info("Building call graph for JAR: {}", jarPath);
        
        // Initialize Soot with call graph construction
        G.reset();
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(jarPath));
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_allow_phantom_refs(true);
        
        // Configure SPARK call graph algorithm
        Options.v().setPhaseOption("cg.spark", "on");
        Options.v().setPhaseOption("cg.spark", "verbose:false");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");
        
        // Load classes and run analysis
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
        
        CallGraph callGraph = Scene.v().getCallGraph();
        log.info("Call graph construction completed: {} edges", callGraph.size());
        
        return callGraph;
    }
    
    @Override
    public Set<EntryPoint> extractEntryPoints() {
        log.info("Extracting entry points for JAR: {}", jarPath);
        
        Set<SootMethod> mainMethods = findMainMethods();
        
        if (mainMethods.isEmpty()) {
            throw new EntryPointNotFoundException(
                "No main() methods found in JAR. Use --entry-points parameter to specify entry points manually.");
        }
        
        Set<EntryPoint> entryPoints = mainMethods.stream()
                .map(method -> new EntryPoint(method, ComponentType.APPLICATION_CLASS, true))
                .collect(Collectors.toSet());
        
        log.info("Found {} main method entry points", entryPoints.size());
        entryPoints.forEach(ep -> log.debug(" - {}", ep.getSignature()));
        
        return entryPoints;
    }
    
    @Override
    public Set<SootMethod> resolveTargetMethods(Set<String> signatures) {
        log.info("Resolving {} target method signatures", signatures.size());
        
        Set<SootMethod> sootMethods = new HashSet<>();
        
        for (String signature : signatures) {
            try {
                SootMethod method = Scene.v().getMethod(signature);
                sootMethods.add(method);
                log.debug("Resolved target method: {}", signature);
            } catch (RuntimeException e) {
                log.warn("Could not resolve target method: {} - {}", signature, e.getMessage());
            }
        }
        
        log.info("Resolved {} of {} target methods", sootMethods.size(), signatures.size());
        return sootMethods;
    }
    
    /**
     * Initializes basic Soot environment for class loading and analysis.
     */
    private void initializeBasicSoot() {
        log.debug("Initializing basic Soot for JAR: {}", jarPath);
        
        G.reset();
        Options.v().set_process_dir(Collections.singletonList(jarPath));
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        
        Scene.v().loadNecessaryClasses();
    }
    
    /**
     * Resolves target methods for direct call analysis.
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
     * Detects the main package of the JAR application.
     *
     * Analyzes all application classes to determine the most common package
     * prefix, providing a reasonable default for package-based filtering.
     *
     * @return Most common package name or "default" if none found
     */
    private String detectMainPackage() {
        Map<String, Integer> packageCounts = new HashMap<>();
        
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            String packageName = clazz.getPackageName();
            if (!packageName.isEmpty()) {
                packageCounts.merge(packageName, 1, Integer::sum);
            }
        }
        
        String mainPackage = packageCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("default");
        
        log.debug("Detected main package: {}", mainPackage);
        return mainPackage;
    }
    
    /**
     * Creates ReachClass instance for a SootClass.
     */
    private ReachClass createReachClass(SootClass sootClass) {
        String className = sootClass.getName();
        ComponentType componentType = determineComponentType(sootClass);
        boolean isMainComponent = hasMainMethod(sootClass);
        
        return new ReachClass(className, componentType, isMainComponent);
    }
    
    /**
     * Determines component type for a class in Java context.
     */
    private ComponentType determineComponentType(SootClass clazz) {
        // For JAR files, classify as application or library class
        String packageName = clazz.getPackageName();
        
        // Simple heuristic: classes in common library packages are library classes
        List<String> libraryPackages = List.of("org.apache.", "com.google.", "org.springframework.", 
                                              "org.junit.", "org.slf4j.", "ch.qos.logback.");
        
        boolean isLibraryClass = libraryPackages.stream()
                .anyMatch(packageName::startsWith);
        
        return isLibraryClass ? ComponentType.LIBRARY_CLASS : ComponentType.APPLICATION_CLASS;
    }
    
    /**
     * Checks if a class has a valid main method.
     */
    private boolean hasMainMethod(SootClass clazz) {
        try {
            SootMethod mainMethod = clazz.getMethodByName("main");
            return isValidMainMethod(mainMethod);
        } catch (RuntimeException e) {
            return false;
        }
    }
    
    /**
     * Finds all valid main methods in the application.
     */
    private Set<SootMethod> findMainMethods() {
        Set<SootMethod> mainMethods = new HashSet<>();
        
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            try {
                SootMethod mainMethod = clazz.getMethodByName("main");
                if (isValidMainMethod(mainMethod)) {
                    mainMethods.add(mainMethod);
                }
            } catch (RuntimeException e) {
                // Method not found, continue
            }
        }
        
        return mainMethods;
    }
    
    /**
     * Validates if a method is a proper main method.
     */
    private boolean isValidMainMethod(SootMethod method) {
        return method.isStatic() 
                && method.isPublic() 
                && method.getParameterCount() == 1
                && method.getParameterType(0).toString().equals("java.lang.String[]")
                && method.getReturnType().toString().equals("void");
    }
    
    /**
     * Analyzes method for direct calls to target methods.
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
}