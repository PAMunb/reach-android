package br.unb.cic.reach.common.analysis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.unb.cic.reach.common.model.Path;

/**
 * Container for reachability analysis information for a single method.
 *
 * This class encapsulates all reachability analysis results for a method,
 * serving as an intermediate data structure during analysis computation
 * before final result consolidation into ReachMethod objects.
 */
public class ReachabilityInfo {
    private boolean isEntryPoint = false;
    private boolean reachable = false;
    private boolean reachesTarget = false;
    private boolean directlyReachesTarget = false;
    private Set<String> reachableTargets = new HashSet<>();
    private List<Path> pathsToTargets = new ArrayList<>();
    
    // Getters and setters
    public boolean isEntryPoint() { return isEntryPoint; }
    public void setEntryPoint(boolean entryPoint) { this.isEntryPoint = entryPoint; }
    
    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }
    
    public boolean isReachesTarget() { return reachesTarget; }
    public void setReachesTarget(boolean reachesTarget) { this.reachesTarget = reachesTarget; }
    
    public boolean isDirectlyReachesTarget() { return directlyReachesTarget; }
    public void setDirectlyReachesTarget(boolean directlyReachesTarget) { 
        this.directlyReachesTarget = directlyReachesTarget; 
    }
    
    public Set<String> getReachableTargets() { return reachableTargets; }
    public void setReachableTargets(Set<String> reachableTargets) { 
        this.reachableTargets = reachableTargets; 
    }
    
    public List<Path> getPathsToTargets() { return pathsToTargets; }
    public void setPathsToTargets(List<Path> pathsToTargets) { this.pathsToTargets = pathsToTargets; }
    
    /**
     * Adds a reachable target method signature.
     */
    public void addReachableTarget(String targetSignature) {
        if (targetSignature != null && !targetSignature.isEmpty()) {
            reachableTargets.add(targetSignature);
            reachesTarget = true;
        }
    }
    
    /**
     * Adds a path to a target method.
     */
    public void addPathToTarget(Path path) {
        if (path != null) {
            pathsToTargets.add(path);
        }
    }
}