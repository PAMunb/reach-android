package br.unb.cic.reach.apk.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Android-specific application information container.
 * 
 * This class encapsulates comprehensive information extracted from Android
 * APK manifests, including all component types, permissions, and Android-specific
 * metadata. Designed for complete Android application analysis and can be used
 * independently of the generic analysis framework.
 * 
 * ### Architectural Decisions:
 * - Contains complete Android manifest information for standalone APK analysis
 * - Separates Android-specific data from generic analysis models
 * - Provides organized access to different component types
 * - Maintains all manifest metadata for comprehensive Android tooling
 * 
 * ### Role in the System:
 * - Primary data structure for APK manifest parsing results
 * - Source of Android-specific information for analysis conversion
 * - Foundation for Android component discovery and classification
 * - Complete representation enabling APK module independence
 * 
 * ### Key Features:
 * - All Android component types with complete metadata
 * - Permission and intent filter information preservation
 * - Android-specific attributes and configurations
 * - Organized component access and filtering capabilities
 */
public class AndroidAppInfo {
    
    // Basic application information
    private String path;
    private String fileName;
    private String appName;
    private String label;
    private String packageName;
    private String versionName;
    private int versionCode;
    private int minSdkVersion;
    private int targetSdkVersion;
    
    // Component collections
    private Set<ActivityInfo> activities = new HashSet<>();
    private Set<ServiceInfo> services = new HashSet<>();
    private Set<BroadcastReceiverInfo> broadcastReceivers = new HashSet<>();
    private Set<ContentProviderInfo> contentProviders = new HashSet<>();
    
    // Android-specific information
    private Set<String> permissions = new HashSet<>();
    private Set<String> usesPermissions = new HashSet<>();
    private boolean debuggable = false;
    private boolean allowBackup = true;
    
    // Constructors
    public AndroidAppInfo() {}
    
    public AndroidAppInfo(String path) {
        this.path = path;
        if (path != null) {
            int lastSlash = path.lastIndexOf('/');
            this.fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        }
    }
    
    // Basic application information getters/setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    
    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }
    
    public int getVersionCode() { return versionCode; }
    public void setVersionCode(int versionCode) { this.versionCode = versionCode; }
    
    public int getMinSdkVersion() { return minSdkVersion; }
    public void setMinSdkVersion(int minSdkVersion) { this.minSdkVersion = minSdkVersion; }
    
    public int getTargetSdkVersion() { return targetSdkVersion; }
    public void setTargetSdkVersion(int targetSdkVersion) { this.targetSdkVersion = targetSdkVersion; }
    
    // Component collections getters/setters
    public Set<ActivityInfo> getActivities() { return activities; }
    public void setActivities(Set<ActivityInfo> activities) { this.activities = activities; }
    
    public Set<ServiceInfo> getServices() { return services; }
    public void setServices(Set<ServiceInfo> services) { this.services = services; }
    
    public Set<BroadcastReceiverInfo> getBroadcastReceivers() { return broadcastReceivers; }
    public void setBroadcastReceivers(Set<BroadcastReceiverInfo> broadcastReceivers) { 
        this.broadcastReceivers = broadcastReceivers; 
    }
    
    public Set<ContentProviderInfo> getContentProviders() { return contentProviders; }
    public void setContentProviders(Set<ContentProviderInfo> contentProviders) { 
        this.contentProviders = contentProviders; 
    }
    
    // Android-specific information getters/setters
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
    
    public Set<String> getUsesPermissions() { return usesPermissions; }
    public void setUsesPermissions(Set<String> usesPermissions) { this.usesPermissions = usesPermissions; }
    
    public boolean isDebuggable() { return debuggable; }
    public void setDebuggable(boolean debuggable) { this.debuggable = debuggable; }
    
    public boolean isAllowBackup() { return allowBackup; }
    public void setAllowBackup(boolean allowBackup) { this.allowBackup = allowBackup; }
    
    // Convenience methods for component management
    public void addActivity(ActivityInfo activity) {
        if (activity != null) {
            activities.add(activity);
        }
    }
    
    public void addService(ServiceInfo service) {
        if (service != null) {
            services.add(service);
        }
    }
    
    public void addBroadcastReceiver(BroadcastReceiverInfo receiver) {
        if (receiver != null) {
            broadcastReceivers.add(receiver);
        }
    }
    
    public void addContentProvider(ContentProviderInfo provider) {
        if (provider != null) {
            contentProviders.add(provider);
        }
    }
    
    public void addPermission(String permission) {
        if (permission != null && !permission.trim().isEmpty()) {
            permissions.add(permission.trim());
        }
    }
    
    public void addUsesPermission(String permission) {
        if (permission != null && !permission.trim().isEmpty()) {
            usesPermissions.add(permission.trim());
        }
    }
    
    /**
     * Get all components regardless of type.
     * 
     * @return Set containing all component information objects
     */
    public Set<ComponentInfo> getAllComponents() {
        Set<ComponentInfo> allComponents = new HashSet<>();
        allComponents.addAll(activities);
        allComponents.addAll(services);
        allComponents.addAll(broadcastReceivers);
        allComponents.addAll(contentProviders);
        return allComponents;
    }
    
    /**
     * Get the main activity if one exists.
     * 
     * @return ActivityInfo of the main activity, or null if none found
     */
    public ActivityInfo getMainActivity() {
        return activities.stream()
                .filter(ActivityInfo::isMain)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get the main component (typically the main activity).
     * 
     * @return ComponentInfo of the main component, or null if none found
     */
    public ComponentInfo getMainComponent() {
        return getMainActivity();
    }
    
    /**
     * Get total component count across all types.
     * 
     * @return Total number of components in this application
     */
    public int getTotalComponentCount() {
        return activities.size() + services.size() + 
               broadcastReceivers.size() + contentProviders.size();
    }
    
    @Override
    public String toString() {
        return String.format("AndroidAppInfo{package='%s', components=%d, activities=%d, services=%d, receivers=%d, providers=%d}",
                packageName, getTotalComponentCount(), activities.size(), services.size(), 
                broadcastReceivers.size(), contentProviders.size());
    }
}