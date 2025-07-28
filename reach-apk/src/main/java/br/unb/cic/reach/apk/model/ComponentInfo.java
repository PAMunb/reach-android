package br.unb.cic.reach.apk.model;

import java.util.List;
import java.util.Objects;

import br.unb.cic.reach.common.model.ComponentType;

/**
 * Abstract base class for Android component information.
 *
 * This class provides a unified interface for representing Android application
 * components with common attributes and behavior patterns across different
 * component types (Activities, Services, BroadcastReceivers, ContentProviders).
 *
 * ### Architectural Decisions:
 * - Abstract base class to enforce consistent component modeling
 * - Common attributes extraction to eliminate duplication
 * - Type-safe component identification through ComponentType enum
 * - Abstract lifecycle method identification for entry point analysis
 *
 * ### Role in the System:
 * - Foundation for all Android component information classes
 * - Enables polymorphic processing of different component types
 * - Provides consistent interface for manifest parsing and analysis
 * - Facilitates generic component handling in reachability analysis
 */
public abstract class ComponentInfo {
    protected String name;           // full class name
    protected String shortName;      // class name only
    protected String packageName;    // package portion
    protected boolean enabled = true;       // android:enabled
    protected boolean exported = false;     // android:exported
    protected String label;          // android:label
    protected String icon;           // android:icon

    protected ComponentInfo() {
    }

    protected ComponentInfo(String name) {
        setName(name);
    }

    /**
     * Returns the component type for this Android component.
     *
     * Each concrete component implementation must specify its type
     * to enable proper classification and entry point identification.
     *
     * @return ComponentType representing this component's type
     */
    public abstract ComponentType getComponentType();

    /**
     * Returns lifecycle methods specific to this component type.
     *
     * Each component type has specific lifecycle methods that serve as
     * entry points for reachability analysis. This method delegates to
     * the ComponentType enum for consistent lifecycle method identification.
     *
     * @return List of lifecycle method names for this component type
     */
    public List<String> getLifecycleMethods() {
        return getComponentType().getLifecycleMethods();
    }

    // Common getters and setters
    public String getName() { return name; }
    protected void setName(String name) {
        this.name = name;
        if (name != null && name.contains(".")) {
            this.packageName = name.substring(0, name.lastIndexOf('.'));
            this.shortName = name.substring(name.lastIndexOf('.') + 1);
        } else {
            this.packageName = "";
            this.shortName = name != null ? name : "";
        }
    }

    public String getShortName() { return shortName; }
    public String getPackageName() { return packageName; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isExported() { return exported; }
    public void setExported(boolean exported) { this.exported = exported; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ComponentInfo that = (ComponentInfo) obj;
        return Objects.equals(name, that.name);
    }

    @Override
    public String toString() {
        return String.format("%s [name=%s, enabled=%s, exported=%s]",
                getClass().getSimpleName(), name, enabled, exported);
    }
}