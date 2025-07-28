package br.unb.cic.reach.apk.model;

import java.util.ArrayList;
import java.util.List;

import br.unb.cic.reach.common.model.ComponentType;

/**
 * BroadcastReceiver component information with receiver-specific attributes.
 *
 * Represents Android broadcast receiver declarations from manifest including
 * intent filter configuration, priority settings, and action/category
 * specifications for entry point analysis.
 *
 * ### Architectural Decisions:
 * - Extends ComponentInfo for consistent component modeling
 * - Intent filter collection for comprehensive broadcast handling
 * - Priority support for receiver ordering analysis
 * - Action and category extraction for intent matching analysis
 *
 * ### Role in the System:
 * - Represents broadcast receiver components in Android applications
 * - Provides receiver-specific entry point identification
 * - Enables receiver filtering and classification in analysis
 * - Foundation for broadcast receiver lifecycle method extraction
 */
public class BroadcastReceiverInfo extends ComponentInfo {
    private int priority = 0;
    private List<IntentFilterInfo> intentFilters = new ArrayList<>();

    public BroadcastReceiverInfo() {
        super();
    }

    public BroadcastReceiverInfo(String name) {
        super(name);
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.RECEIVER;
    }

    // Receiver-specific getters and setters
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<IntentFilterInfo> getIntentFilters() { return intentFilters; }
    public void setIntentFilters(List<IntentFilterInfo> intentFilters) { this.intentFilters = intentFilters; }

    /**
     * Adds an intent filter to this receiver.
     *
     * @param intentFilter The intent filter to add
     */
    public void addIntentFilter(IntentFilterInfo intentFilter) {
        if (intentFilter != null) {
            intentFilters.add(intentFilter);
        }
    }

    @Override
    public String toString() {
        return String.format("BroadcastReceiverInfo [name=%s, priority=%d, intentFilters=%d, enabled=%s]",
                name, priority, intentFilters.size(), enabled);
    }
}