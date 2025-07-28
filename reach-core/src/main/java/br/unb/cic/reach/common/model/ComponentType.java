package br.unb.cic.reach.common.model;

import java.util.List;

/**
 * Represents the type of component within an application.
 *
 * This enumeration provides a unified classification system for different
 * types of components across various application formats, supporting both
 * Android-specific components and generic Java class classifications.
 *
 * ### Architectural Decisions:
 * - Unified enum for Android and Java component types
 * - Display names for user-friendly output formatting
 * - Lifecycle method mapping for entry point identification
 *
 * ### Role in the System:
 * - Enables component-specific entry point extraction
 * - Provides consistent component classification across formats
 * - Supports filtering and analysis configuration by component type
 */
public enum ComponentType {
    // Android specific components
    ACTIVITY("activity"),
    SERVICE("service"), 
    RECEIVER("receiver"),
    PROVIDER("provider"),
    
    // Generic classifications for Java applications
    APPLICATION_CLASS("application"),
    LIBRARY_CLASS("library"),
    
    UNKNOWN("unknown");

    private final String displayName;

    ComponentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retrieves lifecycle method names for this component type.
     *
     * Returns the standard lifecycle methods that serve as entry points
     * for the specific component type, enabling automatic entry point
     * detection during reachability analysis.
     *
     * @return List of lifecycle method names for this component type
     */
    public List<String> getLifecycleMethods() {
        return switch (this) {
            case ACTIVITY -> List.of("onCreate", "onStart", "onResume", "onPause", 
                                   "onStop", "onDestroy", "onSaveInstanceState", 
                                   "onRestoreInstanceState", "onActivityResult");
            case SERVICE -> List.of("onCreate", "onDestroy", "onStartCommand", 
                                  "onBind", "onUnbind", "onRebind");
            case RECEIVER -> List.of("onReceive");
            case PROVIDER -> List.of("onCreate", "query", "insert", "update", 
                                   "delete", "getType");
            default -> List.of();
        };
    }
}