package br.unb.cic.reach.apk.model;

import br.unb.cic.reach.common.model.ComponentType;

/**
 * Activity component information with activity-specific attributes.
 *
 * Represents Android activity declarations from manifest with complete
 * attribute extraction including launch modes, task affinity, and
 * main activity identification for entry point analysis.
 *
 * ### Architectural Decisions:
 * - Extends ComponentInfo for consistent component modeling
 * - Activity-specific attributes for comprehensive manifest representation
 * - Main activity identification for analysis prioritization
 * - Launch mode and task affinity support for behavioral analysis
 *
 * ### Role in the System:
 * - Represents activity components in Android applications
 * - Provides activity-specific entry point identification
 * - Enables activity filtering and classification in analysis
 * - Foundation for activity lifecycle method extraction
 */
public class ActivityInfo extends ComponentInfo {
    private boolean isMain = false;
    private String taskAffinity;
    private String launchMode;
    private String screenOrientation;
    private String layoutFileName;

    public ActivityInfo() {
        super();
    }

    public ActivityInfo(String name, boolean isMain) {
        super(name);
        this.isMain = isMain;
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.ACTIVITY;
    }

    // Activity-specific getters and setters
    public boolean isMain() { return isMain; }
    public void setMain(boolean main) { this.isMain = main; }

    public String getTaskAffinity() { return taskAffinity; }
    public void setTaskAffinity(String taskAffinity) { this.taskAffinity = taskAffinity; }

    public String getLaunchMode() { return launchMode; }
    public void setLaunchMode(String launchMode) { this.launchMode = launchMode; }

    public String getScreenOrientation() { return screenOrientation; }
    public void setScreenOrientation(String screenOrientation) { this.screenOrientation = screenOrientation; }

    public String getLayoutFileName() { return layoutFileName; }
    public void setLayoutFileName(String layoutFileName) { this.layoutFileName = layoutFileName; }

    @Override
    public String toString() {
        return String.format("ActivityInfo [name=%s, isMain=%s, launchMode=%s, enabled=%s]",
                name, isMain, launchMode, enabled);
    }
}