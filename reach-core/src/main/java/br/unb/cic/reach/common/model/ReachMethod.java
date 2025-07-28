package br.unb.cic.reach.common.model;

import soot.SootMethod;
import soot.Type;

import java.util.*;

/**
 * Represents a method with comprehensive reachability analysis results.
 * <p>
 * This class encapsulates all reachability analysis information for a single
 * method, including reachability status, target method relationships, paths,
 * and entry point classification. Serves as the primary data structure for
 * method-level analysis results.
 * <p>
 * ### Architectural Decisions:
 * - Comprehensive result container for all reachability information
 * - Generic target method support (not limited to MOP specifications)
 * - Path collection for detailed analysis tracing
 * - Entry point identification for analysis starting points
 * <p>
 * ### Role in the System:
 * - Primary data structure for method-level reachability results
 * - Container for path information and target method relationships
 * - Foundation for result formatting and output generation
 * - Interface between analysis algorithms and result writers
 */
public class ReachMethod {
    private final String methodName;
    private final List<String> methodParams;
    private final String methodSignature;
    private final int modifiers;

    // Entry point classification
    private boolean isEntryPoint = false;

    // Reachability status from entry points
    private boolean reachable = false;

    // Target method reachability
    private boolean reachesTarget = false;
    private boolean directlyReachesTarget = false;
    private Set<String> reachableTargets = new HashSet<>();

    // Path information
    private Path possiblePath;
    private List<Path> pathsToTargets = new ArrayList<>();

    public ReachMethod(SootMethod sootMethod) {
        this.methodName = sootMethod.getName();
        this.methodParams = sootMethod.getParameterTypes().stream()
                .map(Type::toString)
                .toList();
        this.methodSignature = sootMethod.getSignature();
        this.modifiers = sootMethod.getModifiers();
    }

    // Getters and setters
    public String getMethodName() {
        return methodName;
    }

    public List<String> getMethodParams() {
        return methodParams;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isEntryPoint() {
        return isEntryPoint;
    }

    public void setEntryPoint(boolean entryPoint) {
        this.isEntryPoint = entryPoint;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public boolean isReachesTarget() {
        return reachesTarget;
    }

    public void setReachesTarget(boolean reachesTarget) {
        this.reachesTarget = reachesTarget;
    }

    public boolean isDirectlyReachesTarget() {
        return directlyReachesTarget;
    }

    public void setDirectlyReachesTarget(boolean directlyReachesTarget) {
        this.directlyReachesTarget = directlyReachesTarget;
    }

    public Set<String> getReachableTargets() {
        return reachableTargets;
    }

    public void setReachableTargets(Set<String> reachableTargets) {
        this.reachableTargets = reachableTargets;
    }

    public Path getPossiblePath() {
        return possiblePath;
    }

    public void setPossiblePath(Path possiblePath) {
        this.possiblePath = possiblePath;
    }

    public List<Path> getPathsToTargets() {
        return pathsToTargets;
    }

    public void setPathsToTargets(List<Path> pathsToTargets) {
        this.pathsToTargets = pathsToTargets;
    }

    /**
     * Adds a reachable target method signature.
     * <p>
     * Records that this method can reach the specified target method,
     * maintaining the collection of all reachable targets for result output.
     *
     * @param targetSignature The signature of the reachable target method
     */
    public void addReachableTarget(String targetSignature) {
        if (targetSignature != null && !targetSignature.isEmpty()) {
            reachableTargets.add(targetSignature);
            reachesTarget = true;
        }
    }

    /**
     * Adds a path to a target method.
     * <p>
     * Records a complete path from this method to a target method,
     * enabling detailed analysis result reporting and path visualization.
     *
     * @param path The path from this method to a target method
     */
    public void addPathToTarget(Path path) {
        if (path != null) {
            pathsToTargets.add(path);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodSignature);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ReachMethod that = (ReachMethod) obj;
        return Objects.equals(methodSignature, that.methodSignature);
    }

    @Override
    public String toString() {
        return String.format("ReachMethod [methodName=%s, signature=%s, entryPoint=%s, reachable=%s, " +
                        "reachesTarget=%s, directlyReachesTarget=%s, targets=%d]",
                methodName, methodSignature, isEntryPoint, reachable,
                reachesTarget, directlyReachesTarget, reachableTargets.size());
    }
}