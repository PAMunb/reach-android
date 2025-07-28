package br.unb.cic.reach.common.model;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generic application information container for reachability analysis.
 * <p>
 * This class serves as the central repository for application metadata and
 * class structure information, supporting multiple application types through
 * a unified interface. Contains analysis results and component organization
 * for comprehensive reachability computation.
 * <p>
 * ### Architectural Decisions:
 * - Generic design supporting multiple application formats
 * - Organized class collection with component type filtering
 * - Unified metadata structure across different application types
 * - Result container for reachability analysis outcomes
 * <p>
 * ### Role in the System:
 * - Central data structure for application information storage
 * - Interface between extractors and analysis algorithms
 * - Result container for reachability analysis output
 * - Foundation for writer implementations and result formatting
 */
public class AppInfo {
    private String path;
    private String fileName;
    private String packageName;
    private String appName;
    private String label;
    private ApplicationType type;

    private Set<ReachClass> classes = new HashSet<>();

    public AppInfo() {
    }

    public AppInfo(String path) {
        this.path = path;
        this.type = ApplicationType.fromPath(path);
        if (path != null) {
            this.fileName = extractFileName(path);
            this.label = extractLabel(path);
        }
    }

    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String[] parts = path.replace('\\', '/').split("/");
        return parts[parts.length - 1];
    }

    private String extractLabel(String path) {
        String name = extractFileName(path);
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : name;
    }

    // Getters and setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ApplicationType getType() {
        return type;
    }

    public void setType(ApplicationType type) {
        this.type = type;
    }

    public Set<ReachClass> getClasses() {
        return classes;
    }

    public void setClasses(Set<ReachClass> classes) {
        this.classes = classes;
    }

    /**
     * Adds a class to the application information.
     * <p>
     * Registers a class for inclusion in reachability analysis, maintaining
     * the collection of application classes with their component type
     * classification and method information.
     *
     * @param clazz The ReachClass to add to the application
     */
    public void addClass(ReachClass clazz) {
        if (clazz != null) {
            classes.add(clazz);
        }
    }

    /**
     * Retrieves classes filtered by component type.
     * <p>
     * Filters the application classes based on their component type
     * classification, enabling component-specific analysis and processing.
     *
     * @param componentType The component type to filter by
     * @return Set of classes matching the specified component type
     */
    public Set<ReachClass> getClassesByComponentType(ComponentType componentType) {
        return classes.stream()
                .filter(clazz -> clazz.getComponentType() == componentType)
                .collect(Collectors.toSet());
    }

    /**
     * Identifies the main component class in the application.
     * <p>
     * Locates the primary entry point class (main activity for Android,
     * main class for Java applications) to facilitate analysis prioritization.
     *
     * @return The main component class if found, null otherwise
     */
    public ReachClass getMainComponent() {
        return classes.stream()
                .filter(ReachClass::isMainComponent)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return String.format("AppInfo [path=%s, fileName=%s, appName=%s, label=%s, " +
                        "packageName=%s, type=%s, classes=%d]",
                path, fileName, appName, label, packageName, type, classes.size());
    }
}