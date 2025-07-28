package br.unb.cic.reach.common.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a class with reachability analysis information.
 * <p>
 * This class encapsulates information about a single class within an
 * application, including its component type classification, main component
 * status, and collection of methods with their reachability analysis results.
 * <p>
 * ### Architectural Decisions:
 * - Generic class representation supporting multiple application types
 * - Component type classification for entry point identification
 * - Method collection with reachability analysis results
 * - Main component identification for analysis prioritization
 * <p>
 * ### Role in the System:
 * - Container for class-level reachability analysis results
 * - Component type classification for entry point extraction
 * - Method organization and access for analysis algorithms
 * - Data structure for result formatting and output generation
 */
public class ReachClass {
    private final String className;
    private final ComponentType componentType;
    private final boolean isMainComponent;
    private Set<ReachMethod> methods = new HashSet<>();

    public ReachClass(String className, ComponentType componentType, boolean isMainComponent) {
        this.className = className;
        this.componentType = componentType;
        this.isMainComponent = isMainComponent;
    }

    // Getters and setters
    public String getClassName() {
        return className;
    }

    public ComponentType getComponentType() {
        return componentType;
    }

    public boolean isMainComponent() {
        return isMainComponent;
    }

    public Set<ReachMethod> getMethods() {
        return methods;
    }

    public void setMethods(Set<ReachMethod> methods) {
        this.methods = methods;
    }

    /**
     * Adds a method to this class.
     * <p>
     * Registers a method for inclusion in reachability analysis results,
     * maintaining the collection of methods associated with this class.
     *
     * @param method The ReachMethod to add to this class
     */
    public void addMethod(ReachMethod method) {
        if (method != null) {
            methods.add(method);
        }
    }

    @Override
    public String toString() {
        return String.format("ReachClass [className=%s, componentType=%s, isMainComponent=%s, methods=%d]",
                className, componentType, isMainComponent, methods.size());
    }
}