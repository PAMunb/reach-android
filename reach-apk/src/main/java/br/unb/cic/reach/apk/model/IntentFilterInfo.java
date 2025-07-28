package br.unb.cic.reach.apk.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Intent filter information for Android components.
 *
 * Represents intent-filter declarations from manifest including
 * actions, categories, data specifications, and priority settings
 * for determining component activation patterns.
 *
 * ### Architectural Decisions:
 * - Comprehensive intent filter attribute support
 * - List-based collections for multiple actions/categories
 * - Data specification support for URI matching
 * - Priority configuration for filter ordering
 *
 * ### Role in the System:
 * - Represents intent filter configurations in Android manifest
 * - Enables intent matching analysis for component activation
 * - Provides data for broadcast receiver and activity analysis
 * - Foundation for intent-based entry point identification
 */
public class IntentFilterInfo {
    private List<String> actions = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private List<String> dataSchemes = new ArrayList<>();
    private List<String> dataMimeTypes = new ArrayList<>();
    private int priority = 0;

    public IntentFilterInfo() {
    }

    // Getters and setters
    public List<String> getActions() { return actions; }
    public void setActions(List<String> actions) { this.actions = actions; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public List<String> getDataSchemes() { return dataSchemes; }
    public void setDataSchemes(List<String> dataSchemes) { this.dataSchemes = dataSchemes; }

    public List<String> getDataMimeTypes() { return dataMimeTypes; }
    public void setDataMimeTypes(List<String> dataMimeTypes) { this.dataMimeTypes = dataMimeTypes; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    // Convenience methods for adding elements
    public void addAction(String action) {
        if (action != null && !action.isEmpty()) {
            actions.add(action);
        }
    }

    public void addCategory(String category) {
        if (category != null && !category.isEmpty()) {
            categories.add(category);
        }
    }

    public void addDataScheme(String scheme) {
        if (scheme != null && !scheme.isEmpty()) {
            dataSchemes.add(scheme);
        }
    }

    public void addDataMimeType(String mimeType) {
        if (mimeType != null && !mimeType.isEmpty()) {
            dataMimeTypes.add(mimeType);
        }
    }

    @Override
    public String toString() {
        return String.format("IntentFilterInfo [actions=%s, categories=%s, priority=%d]",
                actions, categories, priority);
    }
}