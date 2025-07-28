package br.unb.cic.reach.common.writer.json;

import java.util.List;
import java.util.stream.Collectors;

import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.model.AppInfo;

/**
 * Root JSON output structure for reachability analysis results.
 *
 * This class provides the top-level structure for JSON output, organizing
 * application information and analysis results in a hierarchical format
 * suitable for comprehensive result reporting and programmatic access.
 */
public class JsonOutput {
    private AppInfoJson appInfo;
    private List<ReachClassJson> results;
    private AnalysisMetricsJson metrics;
    
    public JsonOutput(ReachabilityResult result) {
        this.appInfo = new AppInfoJson(result.getAppInfo());
        this.results = result.getAppInfo().getClasses().stream()
                .map(ReachClassJson::new)
                .collect(Collectors.toList());
        this.metrics = new AnalysisMetricsJson(result);
    }
    
    // Getters for JSON serialization
    public AppInfoJson getAppInfo() { return appInfo; }
    public List<ReachClassJson> getResults() { return results; }
    public AnalysisMetricsJson getMetrics() { return metrics; }
}