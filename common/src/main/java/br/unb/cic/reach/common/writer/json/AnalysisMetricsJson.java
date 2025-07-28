package br.unb.cic.reach.common.writer.json;

import br.unb.cic.reach.common.analysis.ReachabilityResult;

/**
 * JSON representation of analysis metrics and statistics.
 */
public class AnalysisMetricsJson {
    private long executionTime;
    private int entryPointCount;
    private int targetMethodCount;
    private int reachableMethodCount;
    private int targetReachingMethodCount;
    private int totalClasses;
    
    public AnalysisMetricsJson(ReachabilityResult result) {
        this.executionTime = result.getExecutionTime();
        this.entryPointCount = result.getEntryPointCount();
        this.targetMethodCount = result.getTargetMethodCount();
        this.reachableMethodCount = result.getReachableMethodCount();
        this.targetReachingMethodCount = result.getTargetReachingMethodCount();
        this.totalClasses = result.getAppInfo().getClasses().size();
    }
    
    // Getters for JSON serialization
    public long getExecutionTime() { return executionTime; }
    public int getEntryPointCount() { return entryPointCount; }
    public int getTargetMethodCount() { return targetMethodCount; }
    public int getReachableMethodCount() { return reachableMethodCount; }
    public int getTargetReachingMethodCount() { return targetReachingMethodCount; }
    public int getTotalClasses() { return totalClasses; }
}