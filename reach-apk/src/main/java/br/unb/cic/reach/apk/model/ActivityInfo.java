package br.unb.cic.reach.apk.model;

import br.unb.cic.reach.common.model.ComponentType;

import java.util.Objects;

/**
 * Activity component information with activity-specific attributes.
 * <p>
 * Represents Android activity declarations from manifest with complete
 * attribute extraction including launch modes, task affinity, and
 * main activity identification for entry point analysis.
 * <p>
 * ### Architectural Decisions:
 * - Extends ComponentInfo for consistent component modeling
 * - Activity-specific attributes for comprehensive manifest representation
 * - Main activity identification for analysis prioritization
 * - Launch mode and task affinity support for behavioral analysis
 * <p>
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
    public boolean isMain() {
        return isMain;
    }

    public void setMain(boolean main) {
        this.isMain = main;
    }

    public String getTaskAffinity() {
        return taskAffinity;
    }

    public void setTaskAffinity(String taskAffinity) {
        this.taskAffinity = taskAffinity;
    }

    public String getLaunchMode() {
        return launchMode;
    }

    public void setLaunchMode(String launchMode) {
        this.launchMode = launchMode;
    }

    public String getScreenOrientation() {
        return screenOrientation;
    }

    public void setScreenOrientation(String screenOrientation) {
        this.screenOrientation = screenOrientation;
    }

    public String getLayoutFileName() {
        return layoutFileName;
    }

    public void setLayoutFileName(String layoutFileName) {
        this.layoutFileName = layoutFileName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ActivityInfo that)) return false;
        if (!super.equals(o)) return false;
        return isMain() == that.isMain() && Objects.equals(getTaskAffinity(), that.getTaskAffinity()) && Objects.equals(getLaunchMode(), that.getLaunchMode()) && Objects.equals(getScreenOrientation(), that.getScreenOrientation()) && Objects.equals(getLayoutFileName(), that.getLayoutFileName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isMain(), getTaskAffinity(), getLaunchMode(), getScreenOrientation(), getLayoutFileName());
    }

    @Override
    public String toString() {
        return String.format("ActivityInfo [name=%s, isMain=%s, launchMode=%s, enabled=%s]",
                name, isMain, launchMode, enabled);
    }
}