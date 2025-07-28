package br.unb.cic.reach.common.analysis;

import java.util.Map;

import br.unb.cic.reach.common.model.AppInfo;
import soot.SootMethod;

/**
 * Container for complete reachability analysis results.
 *
 * This class encapsulates the comprehensive results of reachability analysis,
 * including the populated AppInfo with analysis results and detailed
 * reachability information for individual methods.
 *
 * ### Architectural Decisions:
 * - Comprehensive result container for analysis outcomes
 * - AppInfo integration for result organization and output
 * - Detailed reachability mapping for analysis introspection
 *
 * ### Role in the System:
 * - Result container for reachability analysis execution
 * - Interface between analysis algorithms and result writers
 * - Foundation for result formatting and output generation
 */
public class ReachabilityResult {
    private AppInfo appInfo;
    private Map<SootMethod, ReachabilityInfo> reachabilityMap;
    private long executionTime;
    private int entryPointCount;
    private int targetMethodCount;
    
    public ReachabilityResult(AppInfo appInfo) {
        this.appInfo = appInfo;
    }
    
    public ReachabilityResult(AppInfo appInfo, Map<SootMethod, ReachabilityInfo> reachabilityMap) {
        this.appInfo = appInfo;
        this.reachabilityMap = reachabilityMap;
    }
    
    public ReachabilityResult(AppInfo appInfo, Map<SootMethod, ReachabilityInfo> reachabilityMap, long executionTime) {
        this.appInfo = appInfo;
        this.reachabilityMap = reachabilityMap;
        this.executionTime = executionTime;
    }
    
    // Getters and setters
    public AppInfo getAppInfo() { return appInfo; }
    public void setAppInfo(AppInfo appInfo) { this.appInfo = appInfo; }
    
    public Map<SootMethod, ReachabilityInfo> getReachabilityMap() { return reachabilityMap; }
    public void setReachabilityMap(Map<SootMethod, ReachabilityInfo> reachabilityMap) { 
        this.reachabilityMap = reachabilityMap; 
    }
    
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    
    public int getEntryPointCount() { return entryPointCount; }
    public void setEntryPointCount(int entryPointCount) { this.entryPointCount = entryPointCount; }
    
    public int getTargetMethodCount() { return targetMethodCount; }
    public void setTargetMethodCount(int targetMethodCount) { this.targetMethodCount = targetMethodCount; }
    
    /**
     * Returns count of reachable methods in the analysis.
     */
    public int getReachableMethodCount() {
        if (reachabilityMap == null) {
            return 0;
        }
        return (int) reachabilityMap.values().stream()
                .filter(ReachabilityInfo::isReachable)
                .count();
    }
    
    /**
     * Returns count of methods that can reach target methods.
     */
    public int getTargetReachingMethodCount() {
        if (reachabilityMap == null) {
            return 0;
        }
        return (int) reachabilityMap.values().stream()
                .filter(ReachabilityInfo::isReachesTarget)
                .count();
    }
}