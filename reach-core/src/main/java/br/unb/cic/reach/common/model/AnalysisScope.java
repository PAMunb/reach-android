package br.unb.cic.reach.common.model;

/**
 * Enumeration of analysis scope options for output filtering configuration.
 * <p>
 * This enum defines the available scopes for filtering analysis output based on
 * reachability status, enabling users to control what methods are included in
 * the final results across the 60-configuration matrix.
 * <p>
 * ### Architectural Decisions:
 * - Located in reach-core to avoid circular dependencies
 * - Type-safe enum for compile-time validation
 * - Clear value mapping for command line parameter processing
 * - Integration with configuration matrix as one of three dimensions
 * <p>
 * ### Role in the System:
 * - Core configuration type for output scope filtering
 * - Bridge between command line configuration and writer filtering
 * - Foundation for analysis scope validation and processing
 * - Key component of the 60-configuration matrix implementation
 */
public enum AnalysisScope {
    /**
     * Include all methods from selected classes regardless of reachability status.
     * <p>
     * This is the default scope that preserves backward compatibility and
     * provides complete method enumeration for comprehensive analysis.
     */
    ALL_METHODS("all-methods"),

    /**
     * Include only methods that are reachable from entry points.
     * <p>
     * This scope creates focused output by filtering methods based on
     * call graph reachability analysis, potentially reducing output size
     * dramatically in large applications.
     */
    REACHABLE_ONLY("reachable-only");

    private final String value;

    AnalysisScope(String value) {
        this.value = value;
    }

    /**
     * Gets the string representation of this scope for command line processing.
     *
     * @return String value used in command line parameters
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the description of this scope for user interface and documentation.
     *
     * @return Human-readable description of the analysis scope
     */
    public String getDescription() {
        return switch (this) {
            case ALL_METHODS -> "Include all methods from selected classes";
            case REACHABLE_ONLY -> "Include only methods reachable from entry points";
            default -> "Unknown scope";
        };
    }

    /**
     * Converts string parameter to enum with comprehensive validation.
     * <p>
     * Provides robust parsing of command line parameters with clear error
     * messages for invalid values, supporting the configuration matrix
     * validation process.
     *
     * @param value String representation of analysis scope
     * @return AnalysisScope enum value
     * @throws IllegalArgumentException if value is invalid with descriptive message
     */
    public static AnalysisScope fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ALL_METHODS; // Default fallback for backward compatibility
        }

        String trimmed = value.trim().toLowerCase();
        for (AnalysisScope scope : values()) {
            if (scope.value.equalsIgnoreCase(trimmed)) {
                return scope;
            }
        }

        throw new IllegalArgumentException("Invalid analysis scope: '" + value +
                "'. Valid options are: all-methods, reachable-only");
    }

    @Override
    public String toString() {
        return value;
    }
}