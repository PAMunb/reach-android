package br.unb.cic.reach.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import br.unb.cic.reach.model.ReachMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.apk.model.ActivityInfo;
import br.unb.cic.reach.apk.model.AppInfo;
import br.unb.cic.reach.apk.util.AndroidUtil;
import br.unb.cic.reach.model.Path;
import br.unb.cic.reach.model.ReachClass;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class ReachabilityAnalysis {
    private static final Logger log = LoggerFactory.getLogger(ReachabilityAnalysis.class);

    private final AppInfo appInfo;
    private final Set<SootMethod> targetMethods;
    private final Set<SootMethod> entryPoints;
    private ReachabilityStrategy<SootMethod, Path> analysisStrategy;

    public ReachabilityAnalysis(AppInfo appInfo, Set<SootMethod> targetMethods, Set<SootMethod> entryPoints) {
        this.appInfo = appInfo;
        this.targetMethods = targetMethods;
        this.entryPoints = entryPoints;
    }

    /**
     * Runs the reachability analysis on the given application using the given strategy.
     *
     * @param strategy The strategy for analyzing the reachability of methods.
     * @return A set of {@link ReachClass} objects, each representing a class in the application
     *         with its reachable methods and paths.
     */
    public Set<ReachClass> reachabilityAnalysis(ReachabilityStrategy<SootMethod, Path> strategy) {
        Set<ReachClass> result = new HashSet<>();

        this.analysisStrategy = strategy;
        analysisStrategy.initialize(Scene.v().getCallGraph(), appInfo);

        // All reachable methods (from an entrypoint)
        Map<SootMethod, Path> reachableMethods = getReachableMethods(entryPoints);

        // for each class (in package declared in manifest)
        for (SootClass sootClass : getApplicationClasses()) {
            ReachClass clazz = createRvsecClass(sootClass);
            result.add(clazz);
            log.debug("Processing class: " + clazz.getClassName());
            // for each method of the clazz
            for (SootMethod sootMethod : sootClass.getMethods()) {
                ReachMethod method = new ReachMethod(sootMethod);
                clazz.addMethod(method);

//				System.out.println(" - "+sootMethod.getSignature());

                // check if the method is reachable
                if (reachableMethods.containsKey(sootMethod)) {
                    method.setReachable(true);
                    method.setPossiblePath(reachableMethods.get(sootMethod));
                }

                // analyses reachability between the current method and methods defined in MOP specs
                processReachabilityToMop(method, sootMethod, targetMethods);
            }
        }

        return result;
    }

    private void processActivityLifecycleCallbacks(Set<ReachClass> result) {
        List<String> activityLifecycleMethods = List.of("onCreate", "onStart", "onResume",
                "onPause", "onStop", "onDestroy", "onSaveInstanceState", "onRestoreInstanceState");
        for (ReachClass clazz : result) {
            for (ReachMethod method : clazz.getMethods()) {
                for (String callBack : activityLifecycleMethods) {
                    if (method.getMethodName().equals(callBack)) {
                        method.setReachable(true);
                    }
                }
            }
        }
    }

    private ReachMethod getMethodInResults(String signature, Set<ReachClass> result) {
        for (ReachClass clazz : result) {
            for (ReachMethod method : clazz.getMethods()) {
                if (method.getMethodSignature().equals(signature)) {
                    return method;
                }
            }
        }
        return null;
    }

    private ReachClass createRvsecClass(SootClass sootClass) {
        boolean isActivity = false;
        boolean isMainActivity = false;
        ActivityInfo info = getActivityInfo(sootClass);
        if (info != null) {
            isActivity = true;
            isMainActivity = info.isMain();
        }
        return new ReachClass(sootClass, isActivity, isMainActivity);
    }

    /**
     * Analyzes and records reachability from a given method to MOP specification methods.
     * 
     * @param method The ReachMethod representation being analyzed
     * @param sootMethod The corresponding SootMethod being analyzed
     * @param mopMethods Set of MOP specification methods to check reachability to
     */
    private void processReachabilityToMop(ReachMethod method, SootMethod sootMethod, Set<SootMethod> mopMethods) {
        // Check reachability to each MOP specification method
        for (SootMethod mopMethod : mopMethods) {
            // Find a path from the current method to the MOP method
            Optional<Path> pathOpt = analysisStrategy.findPath(sootMethod, mopMethod);
            
            if (pathOpt.isPresent()) {
                // Check if this is a direct call (immediate successor in call graph)
                boolean isSuccessor = analysisStrategy.isSuccessor(sootMethod, mopMethod);
                
                if (isSuccessor) {
                    method.setDirectlyReachesMop(true);
                    method.addMopMethodReached(mopMethod.getSignature());
                }
                
                // Record that this method can reach MOP methods and store the path
                method.setReachesMop(true);
                method.addPathToMop(pathOpt.get());
            }
        }
    }

    /**
     * Identifies all methods reachable from the given entry points.
     * 
     * @param entryPoints Set of entry point methods to start the reachability analysis from
     * @return Map of reachable methods to their paths from the nearest entry point
     */
    private Map<SootMethod, Path> getReachableMethods(Set<SootMethod> entryPoints) {
        log.debug("Finding reachable methods ...");
        Map<SootMethod, Path> reachableMethods = new HashMap<>();
        
        // Check each method in the application
        for (SootClass clazz : getApplicationClasses()) {
            for (SootMethod method : clazz.getMethods()) {
                // Skip if already marked as reachable
                if (reachableMethods.containsKey(method)) {
                    continue;
                }
                
                // Check reachability from each entry point
                for (SootMethod entrypoint : entryPoints) {
                    // Skip self-references
                    if (!entrypoint.equals(method)) {
                        Optional<Path> pathOpt = analysisStrategy.findPath(entrypoint, method);
                        if (pathOpt.isPresent()) {
                            // Store the first found path to this method
                            reachableMethods.put(method, pathOpt.get());
                            break;
                        }
                    }
                }
            }
        }
        
        return reachableMethods;
    }

    /**
     * Retrieves Activity information for a given SootClass.
     * 
     * @param clazz The class to get Activity information for
     * @return ActivityInfo if the class is an Activity, null otherwise
     */
    private ActivityInfo getActivityInfo(SootClass clazz) {
        return appInfo.getActivities().stream()
                .filter(info -> info.getName().equals(clazz.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves all application classes that belong to the application's package.
     * 
     * @return List of SootClass objects representing the application's classes
     */
    private List<SootClass> getApplicationClasses() {
        return Scene.v().getApplicationClasses().stream()
                .filter(clazz -> AndroidUtil.isClassInApplicationPackage(clazz, appInfo))
                .collect(Collectors.toList());
    }

}
