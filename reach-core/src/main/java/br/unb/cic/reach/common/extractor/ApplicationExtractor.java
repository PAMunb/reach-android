package br.unb.cic.reach.common.extractor;

import java.util.Set;

import br.unb.cic.reach.common.model.AppInfo;
import br.unb.cic.reach.common.model.ConfigMatrix;
import br.unb.cic.reach.common.model.EntryPoint;
import soot.Body;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

/**
 * Interface for extracting information from different application types.
 * <p>
 * This interface provides a unified contract for processing various application
 * formats (APK, JAR, etc.), supporting both lightweight extraction scenarios
 * and comprehensive reachability analysis with call graph construction.
 * <p>
 * ### Architectural Decisions:
 * - Unified interface for multiple application format support
 * - Separate methods for different analysis complexity levels
 * - Initialization pattern for configuration dependency injection
 * - Clear separation between metadata extraction and analysis preparation
 * <p>
 * ### Role in the System:
 * - Foundation for factory pattern implementation
 * - Contract for application-specific processing logic
 * - Interface between CLI configuration and analysis execution
 * - Abstraction enabling extensible application format support
 */
public interface ApplicationExtractor {

    /**
     * Initializes the extractor with command line configuration.
     * <p>
     * Configures the extractor with user-provided parameters and settings,
     * preparing it for subsequent extraction and analysis operations.
     * <p>
     * ### Implementation Notes:
     * - Must validate configuration parameters for the specific application type
     * - Should prepare internal state for extraction operations
     * - May perform initial validation of input files
     *
     * @param config The command line configuration containing analysis parameters
     */
    void initialize(ConfigMatrix config);

    /**
     * Extracts basic application information without target analysis.
     * <p>
     * Performs lightweight extraction of application metadata and structure
     * without constructing call graphs or performing reachability analysis.
     * Suitable for fast information extraction scenarios.
     * <p>
     * ### Implementation Notes:
     * - Must initialize basic Soot environment for class loading
     * - Should extract component information and application metadata
     * - Must populate AppInfo with class and method structure
     *
     * @return AppInfo containing basic application structure and metadata
     */
    AppInfo extractAppInfo();

    /**
     * Extracts application information with direct call analysis.
     * <p>
     * Performs extraction with lightweight analysis of direct method calls
     * to target methods, providing basic reachability information without
     * full call graph construction. Balances performance with analysis depth.
     * <p>
     * ### Implementation Notes:
     * - Must initialize Soot environment for method body analysis
     * - Should analyze method bodies for direct target method invocations
     * - Must populate ReachMethod objects with direct call information
     *
     * @param targetSignatures Set of target method signatures to analyze
     * @return AppInfo with direct call analysis results
     */
    AppInfo extractAppInfo(Set<String> targetSignatures);

    /**
     * Constructs call graph for comprehensive reachability analysis.
     * <p>
     * Initializes complete Soot environment and constructs call graph using
     * application-specific algorithms, preparing for comprehensive reachability
     * analysis with full path discovery capabilities.
     * <p>
     * ### Implementation Notes:
     * - Must configure Soot with appropriate analysis parameters
     * - Should use application-specific call graph construction
     * - Must ensure call graph quality for accurate reachability analysis
     *
     * @return CallGraph representing method call relationships
     */
    CallGraph buildCallGraph();

    /**
     * Extracts entry points using application-specific logic.
     * <p>
     * Identifies entry point methods based on application type conventions,
     * such as Android component lifecycle methods or Java main methods.
     * Requires initialized Soot environment from buildCallGraph().
     * <p>
     * ### Implementation Notes:
     * - Must identify application-specific entry point patterns
     * - Should classify entry points by component type
     * - Must return SootMethod objects for analysis integration
     *
     * @return Set of EntryPoint objects representing analysis starting points
     */
    Set<EntryPoint> extractEntryPoints();

    /**
     * Resolves target method signatures to SootMethod objects.
     * <p>
     * Converts string signatures to SootMethod objects for analysis integration,
     * handling signature resolution and validation. Requires initialized Soot
     * environment from buildCallGraph().
     * <p>
     * ### Implementation Notes:
     * - Must handle signature parsing and resolution errors gracefully
     * - Should validate target method existence in the application
     * - Must return valid SootMethod objects for analysis algorithms
     *
     * @param signatures Set of method signatures to resolve
     * @return Set of resolved SootMethod objects
     */
    Set<SootMethod> resolveTargetMethods(Set<String> signatures);

    /**
     * Safely retrieves method body handling Soot's lazy loading.
     */
    default Body getMethodBody(SootMethod method) {
        if (method == null) {
            return null;
        }

        try {
            if (method.hasActiveBody()) {
                return method.getActiveBody();
            } else {
                return method.retrieveActiveBody();
            }
        } catch (Exception e) {
            return null;
        }
    }
}