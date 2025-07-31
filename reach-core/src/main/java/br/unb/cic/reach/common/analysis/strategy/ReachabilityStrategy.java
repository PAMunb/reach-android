// common/src/main/java/br/unb/cic/reach/common/analysis/ReachabilityStrategyType.java

package br.unb.cic.reach.common.analysis.strategy;

import java.util.Optional;
import java.util.Set;

import br.unb.cic.reach.common.model.Path;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

/**
 * Strategy interface for different approaches to reachability computation.
 * <p>
 * This interface defines methods for analyzing reachability relationships
 * in call graphs using different algorithms and data structures. Implementations
 * can choose between direct Soot CallGraph traversal or conversion to optimized
 * graph representations like JGraphT.
 * <p>
 * ### Design Decisions:
 * - Uses Soot CallGraph as universal input since all Java analysis relies on Soot
 * - Allows implementations to choose internal representation after initialization
 * - Provides both individual path queries and batch reachability computation
 * - Strategy pattern enables performance optimization based on analysis requirements
 * <p>
 * ### Role in the System:
 * - Abstracts different graph traversal algorithms from the main analysis logic
 * - Enables users to choose between simple BFS and optimized algorithms
 * - Provides consistent interface for reachability queries regardless of implementation
 * - Facilitates testing and comparison of different algorithmic approaches
 * <p>
 * ### Implementation Notes:
 * - initialize() is called once with the CallGraph for setup and conversion
 * - Implementations may convert CallGraph to internal representations during initialization
 * - All query methods assume initialize() has been called successfully
 * - Thread safety is not required as strategies are used in single-threaded analysis
 */
public interface ReachabilityStrategy {

    /**
     * Initialize the strategy with a Soot call graph.
     * <p>
     * This method is called once before any reachability queries are made.
     * Implementations can use this opportunity to convert the CallGraph to
     * their preferred internal representation or perform other setup operations.
     * <p>
     * ### Implementation Guidelines:
     * - Store reference to CallGraph or convert to internal representation
     * - Perform any expensive preprocessing operations here rather than during queries
     * - Handle invalid or empty call graphs gracefully with appropriate exceptions
     * - Log initialization metrics for performance monitoring if needed
     *
     * @param callGraph The Soot call graph to analyze
     * @throws IllegalArgumentException if callGraph is null or invalid
     * @throws RuntimeException         if initialization fails due to memory or processing constraints
     */
    void initialize(CallGraph callGraph);

    /**
     * Find a path between source and target methods.
     * <p>
     * Computes a path from the source method to the target method using
     * the strategy's traversal algorithm. Returns the first path found,
     * which may not be the shortest path depending on the implementation.
     * <p>
     * ### Path Characteristics:
     * - Path includes both source and target methods as endpoints
     * - Empty path indicates no reachability relationship exists
     * - Path order represents call sequence from source to target
     * - Multiple valid paths may exist; implementation chooses which to return
     *
     * @param source The starting method for path computation
     * @param target The destination method for path computation
     * @return Optional containing path if reachable, empty otherwise
     * @throws IllegalStateException    if initialize() has not been called
     * @throws IllegalArgumentException if source or target is null
     */
    Optional<Path> findPath(SootMethod source, SootMethod target);

    /**
     * Check if target is a direct successor of source in the call graph.
     * <p>
     * Determines whether there is a direct call relationship from source
     * to target method. This is more efficient than path finding for
     * checking immediate relationships.
     * <p>
     * ### Direct Relationship Definition:
     * - Returns true if source method directly calls target method
     * - Does not consider transitive relationships through intermediate methods
     * - Useful for identifying immediate call dependencies
     * - More efficient than full path computation for simple queries
     *
     * @param source The calling method
     * @param target The potentially called method
     * @return true if source directly calls target, false otherwise
     * @throws IllegalStateException    if initialize() has not been called
     * @throws IllegalArgumentException if source or target is null
     */
    boolean isSuccessor(SootMethod source, SootMethod target);

    /**
     * Compute all methods reachable from the given source methods.
     * <p>
     * Performs forward reachability analysis from all source methods
     * simultaneously, returning the complete set of reachable methods.
     * This is more efficient than individual path queries when analyzing
     * reachability from multiple starting points.
     * <p>
     * ### Reachability Computation:
     * - Includes all source methods in the result set
     * - Follows all transitive call relationships from sources
     * - Uses breadth-first or optimized traversal depending on implementation
     * - Handles cycles in the call graph appropriately
     *
     * @param sources Set of methods to start reachability analysis from
     * @return Set of all methods reachable from any source method
     * @throws IllegalStateException    if initialize() has not been called
     * @throws IllegalArgumentException if sources is null or empty
     */
    Set<SootMethod> getReachableMethods(Set<SootMethod> sources);
}