package br.unb.cic.reach.common.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import soot.SootMethod;

/**
 * Represents a path between methods in reachability analysis.
 *
 * This class encapsulates a sequence of method signatures that form a path
 * from a source method to a target method, enabling detailed path tracking
 * and analysis result reporting with complete call chain information.
 *
 * ### Architectural Decisions:
 * - String-based signature storage for serialization compatibility
 * - Immutable path representation for thread safety
 * - Support for SootMethod list conversion for analysis integration
 *
 * ### Role in the System:
 * - Path representation for reachability analysis results
 * - Foundation for detailed analysis reporting and visualization
 * - Data structure for path-based result filtering and analysis
 * - Interface between analysis algorithms and result output
 */
public class Path {
    private final List<String> path;
    
    public Path(List<SootMethod> sootMethods) {
        this.path = sootMethods.stream()
                .map(SootMethod::getSignature)
                .collect(Collectors.toList());
    }
    
    public Path(List<String> methodSignatures, boolean fromSignatures) {
        this.path = List.copyOf(methodSignatures);
    }
    
    public List<String> getPath() {
        return path;
    }
    
    /**
     * Returns the length of this path.
     *
     * Provides the number of methods in the call chain, useful for
     * path analysis and filtering operations.
     *
     * @return The number of methods in this path
     */
    public int length() {
        return path.size();
    }
    
    /**
     * Checks if this path is valid.
     *
     * A valid path contains at least two methods (source and target),
     * ensuring meaningful reachability relationships.
     *
     * @return true if the path contains at least two methods, false otherwise
     */
    public boolean isValid() {
        return path != null && path.size() > 1;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Path other = (Path) obj;
        return Objects.equals(path, other.path);
    }
    
    @Override
    public String toString() {
        return String.join(" -> ", path);
    }
}