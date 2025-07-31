package br.unb.cic.reach.common.analysis.strategy;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.common.model.Path;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

/**
 * JGraphT-optimized reachability strategy using Dijkstra's algorithm.
 *
 * This implementation converts the Soot CallGraph to a JGraphT representation
 * during initialization, then uses optimized graph algorithms for reachability
 * queries. It provides better performance for repeated queries on the same
 * call graph at the cost of higher memory usage and initialization overhead.
 *
 * ### Implementation Characteristics:
 * - Converts Soot CallGraph to JGraphT DefaultDirectedGraph during initialization
 * - Uses Dijkstra's shortest path algorithm for path finding
 * - Caches graph structure for multiple efficient queries
 * - Higher memory usage due to dual graph representation
 *
 * ### Performance Profile:
 * - Higher initialization cost due to graph conversion
 * - Faster individual path queries using optimized algorithms
 * - Efficient for dense call graphs with many edges per method
 * - Better performance when making many reachability queries
 *
 * ### Use Cases:
 * - Analysis scenarios requiring many path queries on the same call graph
 * - Dense call graphs where optimized algorithms provide significant benefits
 * - Applications where query performance is more important than memory usage
 * - Research scenarios comparing different graph algorithm implementations
 */
public class JGraphReachabilityStrategy implements ReachabilityStrategy {
    private static final Logger log = LoggerFactory.getLogger(JGraphReachabilityStrategy.class);

    private Graph<SootMethod, DefaultEdge> graph;
    private DijkstraShortestPath<SootMethod, DefaultEdge> dijkstra;

    @Override
    public void initialize(CallGraph callGraph) {
        if (callGraph == null) {
            throw new IllegalArgumentException("CallGraph cannot be null");
        }

        log.debug("Converting Soot CallGraph to JGraphT representation...");
        long startTime = System.currentTimeMillis();

        this.graph = convertToJGraph(callGraph);
        this.dijkstra = new DijkstraShortestPath<>(graph);

        long conversionTime = System.currentTimeMillis() - startTime;
        log.debug("JGraphT conversion completed: {} vertices, {} edges, {}ms",
                graph.vertexSet().size(), graph.edgeSet().size(), conversionTime);
    }

    @Override
    public Optional<Path> findPath(SootMethod source, SootMethod target) {
        validateInitialized();
        validateMethod(source, "source");
        validateMethod(target, "target");

        if (source.equals(target)) {
            return Optional.of(new Path(List.of(source)));
        }

        try {
            GraphPath<SootMethod, DefaultEdge> graphPath = dijkstra.getPath(source, target);
            if (graphPath != null && graphPath.getLength() > 0) {
                return Optional.of(new Path(graphPath.getVertexList()));
            }
        } catch (Exception e) {
            log.debug("Path computation failed between {} and {}: {}",
                    source.getSignature(), target.getSignature(), e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public boolean isSuccessor(SootMethod source, SootMethod target) {
        validateInitialized();
        validateMethod(source, "source");
        validateMethod(target, "target");

        return graph.containsEdge(source, target);
    }

    @Override
    public Set<SootMethod> getReachableMethods(Set<SootMethod> sources) {
        validateInitialized();
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("Sources cannot be null or empty");
        }

        Set<SootMethod> reachable = new HashSet<>();

        // For each source, find all reachable methods using graph traversal
        for (SootMethod source : sources) {
            if (source != null && graph.containsVertex(source)) {
                Set<SootMethod> reachableFromSource = computeReachableFromSource(source);
                reachable.addAll(reachableFromSource);
            }
        }

        log.debug("Computed reachability from {} sources: {} reachable methods",
                sources.size(), reachable.size());

        return reachable;
    }

    /**
     * Convert Soot CallGraph to JGraphT representation.
     *
     * Creates a DefaultDirectedGraph with SootMethod vertices and DefaultEdge
     * edges, filtering out invalid edges and handling duplicate edge cases.
     * This conversion is performed once during initialization to enable
     * efficient queries using JGraphT algorithms.
     *
     * ### Conversion Process:
     * - Iterates through all edges in the Soot CallGraph
     * - Adds source and target methods as vertices
     * - Creates directed edges between calling and called methods
     * - Filters self-loops and invalid edges based on validation criteria
     *
     * @param callGraph Soot CallGraph to convert
     * @return JGraphT representation of the call graph
     */
    private Graph<SootMethod, DefaultEdge> convertToJGraph(CallGraph callGraph) {
        Graph<SootMethod, DefaultEdge> jgraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        int validEdges = 0;
        int totalEdges = 0;

        for (Edge edge : callGraph) {
            totalEdges++;

            if (isValidEdge(edge)) {
                SootMethod source = edge.src();
                SootMethod target = edge.tgt();

                // Add vertices if not already present
                jgraph.addVertex(source);
                jgraph.addVertex(target);

                // Add edge if not already present (JGraphT handles duplicates)
                if (jgraph.addEdge(source, target) != null) {
                    validEdges++;
                }
            }
        }

        log.debug("CallGraph conversion: {}/{} edges included, {} vertices",
                validEdges, totalEdges, jgraph.vertexSet().size());

        return jgraph;
    }

    /**
     * Compute all methods reachable from a single source method.
     *
     * Uses depth-first traversal of the JGraphT graph to find all methods
     * reachable from the given source method. This is more efficient than
     * individual path queries when finding all reachable methods.
     *
     * @param source Starting method for reachability computation
     * @return Set of all methods reachable from the source
     */
    private Set<SootMethod> computeReachableFromSource(SootMethod source) {
        Set<SootMethod> reachable = new HashSet<>();
        Set<SootMethod> visited = new HashSet<>();

        computeReachableDFS(source, reachable, visited);

        return reachable;
    }

    /**
     * Recursive depth-first search for reachability computation.
     *
     * Performs DFS traversal from the current method, adding all encountered
     * methods to the reachable set. Uses visited set to handle cycles and
     * prevent infinite recursion.
     *
     * @param current Current method being processed
     * @param reachable Set to collect all reachable methods
     * @param visited Set to track visited methods and prevent cycles
     */
    private void computeReachableDFS(SootMethod current, Set<SootMethod> reachable, Set<SootMethod> visited) {
        if (!visited.add(current)) {
            return; // Already visited, prevent cycles
        }

        reachable.add(current);

        // Visit all outgoing neighbors
        for (DefaultEdge edge : graph.outgoingEdgesOf(current)) {
            SootMethod target = graph.getEdgeTarget(edge);
            computeReachableDFS(target, reachable, visited);
        }
    }

    /**
     * Validate that an edge should be included in the JGraphT conversion.
     *
     * Filters out self-loops and other invalid edges that might cause
     * issues in graph algorithms or are not relevant for reachability analysis.
     *
     * @param edge Edge to validate
     * @return true if edge should be included in JGraphT graph
     */
    private boolean isValidEdge(Edge edge) {
        if (edge == null || edge.src() == null || edge.tgt() == null) {
            return false;
        }

        // Filter self-loops to avoid trivial cycles
        return !edge.src().equals(edge.tgt());
    }

    /**
     * Validate that the strategy has been properly initialized.
     *
     * @throws IllegalStateException if initialize() has not been called
     */
    private void validateInitialized() {
        if (graph == null || dijkstra == null) {
            throw new IllegalStateException("Strategy must be initialized before use");
        }
    }

    /**
     * Validate that a method parameter is not null.
     *
     * @param method Method to validate
     * @param paramName Parameter name for error message
     * @throws IllegalArgumentException if method is null
     */
    private void validateMethod(SootMethod method, String paramName) {
        if (method == null) {
            throw new IllegalArgumentException(paramName + " method cannot be null");
        }
    }
}