// common/src/main/java/br/unb/cic/reach/common/analysis/ReachabilityAnalysis.java

package br.unb.cic.reach.common.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ComponentType;
import br.unb.cic.reach.common.model.EntryPoint;
import br.unb.cic.reach.common.model.Path;
import br.unb.cic.reach.common.model.ReachClass;
import br.unb.cic.reach.common.model.ReachMethod;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

/**
 * Generic reachability analysis implementation using pluggable strategies.
 *
 * This class performs comprehensive reachability analysis by coordinating
 * between entry point extraction, target method resolution, and graph
 * traversal strategies. It produces complete analysis results including
 * reachable methods, target-reaching methods, and path information.
 *
 * ### Architecture Role:
 * - Orchestrates the complete reachability analysis workflow
 * - Coordinates between different analysis phases using strategy pattern
 * - Produces unified results regardless of underlying graph representation
 * - Abstracts complexity of multi-source, multi-target reachability computation
 *
 * ### Analysis Process:
 * - Forward reachability: identifies methods reachable from entry points
 * - Backward reachability: identifies methods that can reach target methods
 * - Intersection analysis: finds methods that both are reachable and reach targets
 * - Path computation: generates representative paths for reachability relationships
 *
 * ### Performance Considerations:
 * - Uses batch reachability computation for efficiency
 * - Delegates graph traversal optimization to strategy implementations
 * - Minimizes redundant path computations through strategic ordering
 * - Scales to large call graphs through efficient algorithm selection
 */
public class ReachabilityAnalysis {
    private static final Logger log = LoggerFactory.getLogger(ReachabilityAnalysis.class);

    /**
     * Perform complete reachability analysis using the specified strategy.
     *
     * Executes a comprehensive reachability analysis that determines which
     * methods are reachable from entry points, which methods can reach targets,
     * and computes representative paths for reachability relationships.
     *
     * ### Analysis Phases:
     * - Strategy initialization with the provided call graph
     * - Forward reachability computation from all entry points
     * - Backward reachability computation to all target methods
     * - Path computation for methods that both are reachable and reach targets
     * - Result consolidation into AppInfo structure with detailed metrics
     *
     * ### Result Characteristics:
     * - All application methods are included with reachability status
     * - Path information is provided for methods that reach targets
     * - Direct call relationships are identified separately from transitive relationships
     * - Component type information is preserved from entry point analysis
     *
     * @param callGraph Soot call graph representing the application structure
     * @param entryPoints Set of entry points to start reachability analysis from
     * @param targetMethods Set of target methods to analyze reachability towards
     * @param strategy Reachability strategy to use for graph traversal
     * @return Complete analysis results with populated AppInfo and metrics
     * @throws IllegalArgumentException if any parameter is null or invalid
     * @throws RuntimeException if analysis fails due to memory or processing constraints
     */
    public ReachabilityResult analyze(CallGraph callGraph,
                                      Set<EntryPoint> entryPoints,
                                      Set<SootMethod> targetMethods,
                                      ReachabilityStrategy strategy) {

        if (callGraph == null) {
            throw new IllegalArgumentException("CallGraph cannot be null");
        }
        if (entryPoints == null || entryPoints.isEmpty()) {
            throw new IllegalArgumentException("Entry points cannot be null or empty");
        }
        if (targetMethods == null) {
            throw new IllegalArgumentException("Target methods cannot be null");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("Reachability strategy cannot be null");
        }

        log.info("Starting reachability analysis: {} entry points, {} targets",
                entryPoints.size(), targetMethods.size());

        long startTime = System.currentTimeMillis();

        // Initialize strategy with call graph
        strategy.initialize(callGraph);

        // Phase 1: Forward reachability from entry points
        Set<SootMethod> entryPointMethods = entryPoints.stream()
                .map(EntryPoint::getSootMethod)
                .collect(Collectors.toSet());

        Set<SootMethod> reachableFromEntry = strategy.getReachableMethods(entryPointMethods);
        log.debug("Forward reachability: {} methods reachable from entry points",
                reachableFromEntry.size());

        // Phase 2: Backward reachability to targets
        Set<SootMethod> reachingTargets = computeBackwardReachability(targetMethods, strategy);
        log.debug("Backward reachability: {} methods can reach targets",
                reachingTargets.size());

        // Phase 3: Build comprehensive results
        Map<SootMethod, ReachabilityInfo> reachabilityMap =
                buildReachabilityMap(reachableFromEntry, reachingTargets, targetMethods, strategy);

        // Phase 4: Create AppInfo from results
        AppInfo appInfo = buildAppInfoFromResults(reachabilityMap, entryPoints, targetMethods);

        long analysisTime = System.currentTimeMillis() - startTime;
        log.info("Reachability analysis completed in {}ms", analysisTime);

        return new ReachabilityResult(appInfo, reachabilityMap, analysisTime);
    }

    /**
     * Compute backward reachability to target methods.
     *
     * Identifies all methods that can potentially reach any of the target
     * methods through call graph traversal. This is computed by reversing
     * the call graph edges and performing forward reachability from targets.
     *
     * ### Implementation Strategy:
     * - Uses the strategy's path finding capabilities in reverse
     * - Iterates through application methods to test reachability to targets
     * - Optimizes by early termination when sufficient coverage is achieved
     * - Handles empty target sets gracefully
     *
     * @param targetMethods Set of target methods to compute reachability towards
     * @param strategy Initialized reachability strategy for path computation
     * @return Set of methods that can reach at least one target method
     */
    private Set<SootMethod> computeBackwardReachability(Set<SootMethod> targetMethods,
                                                        ReachabilityStrategy strategy) {
        if (targetMethods.isEmpty()) {
            return new HashSet<>();
        }

        Set<SootMethod> reachingTargets = new HashSet<>();

        // For each application method, check if it can reach any target
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            for (SootMethod method : clazz.getMethods()) {
                if (method.isConcrete()) {
                    for (SootMethod target : targetMethods) {
                        Optional<Path> path = strategy.findPath(method, target);
                        if (path.isPresent()) {
                            reachingTargets.add(method);
                            break; // Found path to at least one target
                        }
                    }
                }
            }
        }

        return reachingTargets;
    }

    /**
     * Build comprehensive reachability information map.
     *
     * Creates detailed reachability information for each method by combining
     * forward and backward reachability results with path computation and
     * direct call analysis.
     *
     * ### Information Computed:
     * - Reachability status from entry points
     * - Target reachability status and specific targets reached
     * - Direct call relationships to target methods
     * - Representative paths from entry points to targets through each method
     *
     * @param reachableFromEntry Methods reachable from entry points
     * @param reachingTargets Methods that can reach target methods
     * @param targetMethods Target methods for analysis
     * @param strategy Initialized strategy for path computation
     * @return Map from methods to their complete reachability information
     */
    private Map<SootMethod, ReachabilityInfo> buildReachabilityMap(
            Set<SootMethod> reachableFromEntry,
            Set<SootMethod> reachingTargets,
            Set<SootMethod> targetMethods,
            ReachabilityStrategy strategy) {

        Map<SootMethod, ReachabilityInfo> reachabilityMap = new HashMap<>();

        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            for (SootMethod method : clazz.getMethods()) {
                ReachabilityInfo info = new ReachabilityInfo();

                // Basic reachability status
                info.setReachable(reachableFromEntry.contains(method));
                info.setReachesTarget(reachingTargets.contains(method));

                // Detailed target analysis if method can reach targets
                if (info.isReachesTarget()) {
                    analyzeTargetReachability(method, targetMethods, strategy, info);
                }

                reachabilityMap.put(method, info);
            }
        }

        return reachabilityMap;
    }

    /**
     * Analyze specific target reachability for a method.
     *
     * Determines which specific target methods are reachable from the given
     * method and whether the relationships are direct calls or transitive.
     * Computes representative paths for documentation and debugging purposes.
     *
     * @param method Method to analyze target reachability for
     * @param targetMethods Set of target methods to check
     * @param strategy Strategy for path computation
     * @param info ReachabilityInfo object to populate with results
     */
    private void analyzeTargetReachability(SootMethod method,
                                           Set<SootMethod> targetMethods,
                                           ReachabilityStrategy strategy,
                                           ReachabilityInfo info) {

        Set<String> reachableTargets = new HashSet<>();
        boolean hasDirectCall = false;

        for (SootMethod target : targetMethods) {
            Optional<Path> path = strategy.findPath(method, target);
            if (path.isPresent()) {
                reachableTargets.add(target.getSignature());
                info.addPathToTarget(path.get());

                // Check for direct call relationship
                if (strategy.isSuccessor(method, target)) {
                    hasDirectCall = true;
                }
            }
        }

        info.setReachableTargets(reachableTargets);
        info.setDirectlyReachesTarget(hasDirectCall);
    }

    /**
     * Build AppInfo structure from reachability analysis results.
     *
     * Converts the detailed reachability information into the standard
     * AppInfo format used for output generation. Organizes methods by
     * class and component type while preserving all analysis results.
     *
     * ### AppInfo Population:
     * - Creates ReachClass entries for each application class
     * - Populates ReachMethod entries with complete reachability information
     * - Preserves component type information from entry point analysis
     * - Maintains entry point identification for output formatting
     *
     * @param reachabilityMap Complete reachability information for all methods
     * @param entryPoints Original entry points with component information
     * @param targetMethods Target methods for reference
     * @return Populated AppInfo ready for output generation
     */
    private AppInfo buildAppInfoFromResults(Map<SootMethod, ReachabilityInfo> reachabilityMap,
                                            Set<EntryPoint> entryPoints,
                                            Set<SootMethod> targetMethods) {

        AppInfo appInfo = new AppInfo();

        // Create entry point lookup for component type determination
        Map<SootMethod, EntryPoint> entryPointMap = entryPoints.stream()
                .collect(Collectors.toMap(EntryPoint::getSootMethod, ep -> ep));

        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            ReachClass reachClass = new ReachClass(
                    clazz.getName(),
                    determineComponentType(clazz, entryPointMap),
                    isMainComponent(clazz, entryPointMap)
            );

            for (SootMethod sootMethod : clazz.getMethods()) {
                ReachMethod reachMethod = new ReachMethod(sootMethod);
                ReachabilityInfo info = reachabilityMap.get(sootMethod);

                if (info != null) {
                    populateReachMethod(reachMethod, info, entryPointMap.containsKey(sootMethod));
                }

                reachClass.addMethod(reachMethod);
            }

            appInfo.addClass(reachClass);
        }

        return appInfo;
    }

    /**
     * Populate ReachMethod with information from ReachabilityInfo.
     *
     * @param reachMethod ReachMethod to populate
     * @param info ReachabilityInfo containing analysis results
     * @param isEntryPoint Whether this method is an entry point
     */
    private void populateReachMethod(ReachMethod reachMethod, ReachabilityInfo info, boolean isEntryPoint) {
        reachMethod.setEntryPoint(isEntryPoint);
        reachMethod.setReachable(info.isReachable());
        reachMethod.setReachesTarget(info.isReachesTarget());
        reachMethod.setDirectlyReachesTarget(info.isDirectlyReachesTarget());

        if (info.getReachableTargets() != null) {
            for (String target : info.getReachableTargets()) {
                reachMethod.addReachableTarget(target);
            }
        }

        if (info.getPathsToTargets() != null) {
            for (Path path : info.getPathsToTargets()) {
                reachMethod.addPathToTarget(path);
            }
        }
    }

    /**
     * Determine component type for a class based on entry point information.
     *
     * @param clazz Class to determine component type for
     * @param entryPointMap Map of entry points with component information
     * @return ComponentType for the class
     */
    private ComponentType determineComponentType(SootClass clazz, Map<SootMethod, EntryPoint> entryPointMap) {
        // Check if any method in this class is an entry point
        for (SootMethod method : clazz.getMethods()) {
            EntryPoint entryPoint = entryPointMap.get(method);
            if (entryPoint != null) {
                return entryPoint.getComponentType();
            }
        }

        // Default classification based on Soot's classification
        if (Scene.v().getApplicationClasses().contains(clazz)) {
            return ComponentType.APPLICATION_CLASS;
        } else if (Scene.v().getLibraryClasses().contains(clazz)) {
            return ComponentType.LIBRARY_CLASS;
        }

        return ComponentType.UNKNOWN;
    }

    /**
     * Determine if a class contains the main component (main activity or main method).
     *
     * @param clazz Class to check
     * @param entryPointMap Map of entry points with main component information
     * @return true if class contains main component
     */
    private boolean isMainComponent(SootClass clazz, Map<SootMethod, EntryPoint> entryPointMap) {
        for (SootMethod method : clazz.getMethods()) {
            EntryPoint entryPoint = entryPointMap.get(method);
            if (entryPoint != null && entryPoint.isMainEntry()) {
                return true;
            }
        }
        return false;
    }
}

