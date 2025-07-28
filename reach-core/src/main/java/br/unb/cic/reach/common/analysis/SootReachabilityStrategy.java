package br.unb.cic.reach.common.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.common.model.Path;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * Direct Soot CallGraph reachability strategy using breadth-first search.
 * <p>
 * This implementation uses the Soot CallGraph interface directly without
 * conversion to other graph representations. It provides straightforward
 * BFS-based reachability analysis that is memory-efficient and suitable
 * for most analysis scenarios.
 * <p>
 * ### Implementation Characteristics:
 * - Uses Soot CallGraph edgesOutOf() method for traversal
 * - Implements breadth-first search for path finding and reachability
 * - Maintains visited sets to handle cycles in call graphs
 * - Memory-efficient as it doesn't create additional graph representations
 * <p>
 * ### Performance Profile:
 * - Lower memory usage compared to conversion-based strategies
 * - Moderate performance for individual path queries
 * - Efficient for sparse call graphs with few edges per method
 * - No preprocessing overhead during initialization
 * <p>
 * ### Use Cases:
 * - Default strategy for general-purpose reachability analysis
 * - Memory-constrained environments where graph conversion is expensive
 * - Quick analysis scenarios where preprocessing overhead should be avoided
 * - Debugging and development where direct CallGraph access is preferred
 */
public class SootReachabilityStrategy implements ReachabilityStrategy {
    private static final Logger log = LoggerFactory.getLogger(SootReachabilityStrategy.class);

    private CallGraph callGraph;

    @Override
    public void initialize(CallGraph callGraph) {
        if (callGraph == null) {
            throw new IllegalArgumentException("CallGraph cannot be null");
        }

        this.callGraph = callGraph;
        log.debug("SootReachabilityStrategy initialized with CallGraph size: {}", callGraph.size());
    }

    @Override
    public Optional<Path> findPath(SootMethod source, SootMethod target) {
        validateInitialized();
        validateMethod(source, "source");
        validateMethod(target, "target");

        if (source.equals(target)) {
            return Optional.of(new Path(List.of(source)));
        }

        List<SootMethod> path = computePath(source, target);
        return isValidPath(path) ? Optional.of(new Path(path)) : Optional.empty();
    }

    @Override
    public boolean isSuccessor(SootMethod source, SootMethod target) {
        validateInitialized();
        validateMethod(source, "source");
        validateMethod(target, "target");

        Iterator<Edge> edges = callGraph.edgesOutOf(source);
        while (edges.hasNext()) {
            Edge edge = edges.next();
            if (edge.tgt().equals(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<SootMethod> getReachableMethods(Set<SootMethod> sources) {
        validateInitialized();
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("Sources cannot be null or empty");
        }

        Set<SootMethod> reachable = new HashSet<>();
        Queue<SootMethod> queue = new LinkedList<>();

        // Initialize with all source methods
        for (SootMethod source : sources) {
            if (source != null && reachable.add(source)) {
                queue.add(source);
            }
        }

        // BFS traversal from all sources
        while (!queue.isEmpty()) {
            SootMethod current = queue.poll();

            Iterator<Edge> edges = callGraph.edgesOutOf(current);
            while (edges.hasNext()) {
                Edge edge = edges.next();
                SootMethod target = edge.tgt();

                if (target != null && reachable.add(target)) {
                    queue.add(target);
                }
            }
        }

        log.debug("Computed reachability from {} sources: {} reachable methods",
                sources.size(), reachable.size());

        return reachable;
    }

    /**
     * Compute path between two methods using breadth-first search.
     * <p>
     * Uses BFS to find the shortest path from origin to destination,
     * maintaining parent pointers for path reconstruction. Handles
     * cycles by tracking visited methods and terminates when target
     * is found or all reachable methods are explored.
     *
     * @param origin      Starting method for path computation
     * @param destination Target method for path computation
     * @return List of methods representing path, empty if no path exists
     */
    private List<SootMethod> computePath(SootMethod origin, SootMethod destination) {
        Queue<SootMethod> queue = new LinkedList<>();
        Set<SootMethod> visited = new HashSet<>();
        Map<SootMethod, SootMethod> parentMap = new HashMap<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {
            SootMethod current = queue.poll();

            if (current.equals(destination)) {
                return reconstructPath(current, parentMap);
            }

            Iterator<Edge> edges = callGraph.edgesOutOf(current);
            while (edges.hasNext()) {
                Edge edge = edges.next();
                SootMethod target = edge.tgt();

                if (target != null && !visited.contains(target)) {
                    queue.add(target);
                    visited.add(target);
                    parentMap.put(target, current);
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    /**
     * Reconstruct path from destination to origin using parent map.
     * <p>
     * Traverses the parent map backwards from the destination method
     * to reconstruct the complete path. The resulting path is ordered
     * from origin to destination.
     *
     * @param destination Final method in the path
     * @param parentMap   Map from method to its parent in the search tree
     * @return List of methods representing the complete path
     */
    private List<SootMethod> reconstructPath(SootMethod destination, Map<SootMethod, SootMethod> parentMap) {
        List<SootMethod> path = new ArrayList<>();
        SootMethod current = destination;

        while (current != null) {
            path.addFirst(current);
            current = parentMap.get(current);
        }

        return path;
    }

    /**
     * Validate that the path contains at least two methods.
     * <p>
     * A valid path for reachability analysis must contain at least
     * the source and target methods. Single-method paths indicate
     * self-references which are handled separately.
     *
     * @param path Path to validate
     * @return true if path is valid for reachability analysis
     */
    private boolean isValidPath(List<SootMethod> path) {
        return path != null && path.size() > 1;
    }

    /**
     * Validate that the strategy has been properly initialized.
     *
     * @throws IllegalStateException if initialize() has not been called
     */
    private void validateInitialized() {
        if (callGraph == null) {
            throw new IllegalStateException("Strategy must be initialized before use");
        }
    }

    /**
     * Validate that a method parameter is not null.
     *
     * @param method    Method to validate
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if method is null
     */
    private void validateMethod(SootMethod method, String paramName) {
        if (method == null) {
            throw new IllegalArgumentException(paramName + " method cannot be null");
        }
    }
}
