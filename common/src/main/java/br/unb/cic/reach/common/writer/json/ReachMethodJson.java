package br.unb.cic.reach.common.writer.json;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import br.unb.cic.reach.common.model.ReachMethod;

/**
 * JSON representation of a method with reachability information.
 */
public class ReachMethodJson {
    private String methodName;
    private String methodSignature;
    private boolean isEntryPoint;
    private boolean reachable;
    private boolean reachesTarget;
    private boolean directlyReachesTarget;
    private Set<String> reachableTargets;
    private List<List<String>> pathsToTargets;
    
    public ReachMethodJson(ReachMethod method) {
        this.methodName = method.getMethodName();
        this.methodSignature = method.getMethodSignature();
        this.isEntryPoint = method.isEntryPoint();
        this.reachable = method.isReachable();
        this.reachesTarget = method.isReachesTarget();
        this.directlyReachesTarget = method.isDirectlyReachesTarget();
        this.reachableTargets = method.getReachableTargets();
        this.pathsToTargets = method.getPathsToTargets().stream()
                .map(path -> path.getPath())
                .collect(Collectors.toList());
    }
    
    // Getters for JSON serialization
    public String getMethodName() { return methodName; }
    public String getMethodSignature() { return methodSignature; }
    public boolean isEntryPoint() { return isEntryPoint; }
    public boolean isReachable() { return reachable; }
    public boolean isReachesTarget() { return reachesTarget; }
    public boolean isDirectlyReachesTarget() { return directlyReachesTarget; }
    public Set<String> getReachableTargets() { return reachableTargets; }
    public List<List<String>> getPathsToTargets() { return pathsToTargets; }
}