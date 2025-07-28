package br.unb.cic.reach.common.model;

import java.util.Objects;

import soot.SootMethod;

/**
 * Represents an entry point for reachability analysis.
 *
 * This class encapsulates information about methods that serve as starting
 * points for reachability analysis, including their component type context
 * and main component status for analysis prioritization.
 *
 * ### Architectural Decisions:
 * - SootMethod reference for direct analysis integration
 * - Component type classification for entry point categorization
 * - Main component identification for analysis prioritization
 *
 * ### Role in the System:
 * - Starting point identification for reachability analysis
 * - Component-specific entry point classification
 * - Analysis configuration and filtering foundation
 * - Interface between extractors and analysis algorithms
 */
public class EntryPoint {
    private SootMethod method;
    private ComponentType componentType;
    private boolean isMainComponent;
    
    public EntryPoint(SootMethod method, ComponentType componentType, boolean isMainComponent) {
        this.method = method;
        this.componentType = componentType;
        this.isMainComponent = isMainComponent;
    }
    
    // Getters and setters
    public SootMethod getSootMethod() { return method; }
    public void setSootMethod(SootMethod method) { this.method = method; }
    
    public ComponentType getComponentType() { return componentType; }
    public void setComponentType(ComponentType componentType) { this.componentType = componentType; }
    
    public boolean isMainEntry() { return isMainComponent; }
    public void setMainEntry(boolean mainEntry) { this.isMainComponent = mainEntry; }
    
    /**
     * Returns the method signature for this entry point.
     *
     * Provides a string representation of the method signature for
     * logging, debugging, and result output purposes.
     *
     * @return The method signature string
     */
    public String getSignature() {
        return method != null ? method.getSignature() : "";
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(method);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EntryPoint that = (EntryPoint) obj;
        return Objects.equals(method, that.method);
    }
    
    @Override
    public String toString() {
        return String.format("EntryPoint [method=%s, componentType=%s, isMainComponent=%s]",
                           method != null ? method.getSignature() : "null", 
                           componentType, isMainComponent);
    }
}