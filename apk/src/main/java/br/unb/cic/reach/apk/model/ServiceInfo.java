package br.unb.cic.reach.apk.model;

import br.unb.cic.reach.common.model.ComponentType;

/**
 * Service component information with service-specific attributes.
 *
 * Represents Android service declarations from manifest including
 * foreground service configuration, process isolation settings,
 * and lifecycle method identification for entry point analysis.
 *
 * ### Architectural Decisions:
 * - Extends ComponentInfo for consistent component modeling
 * - Service-specific attributes for comprehensive manifest representation
 * - Foreground service type classification for API level compliance
 * - Process isolation support for security analysis
 *
 * ### Role in the System:
 * - Represents service components in Android applications
 * - Provides service-specific entry point identification
 * - Enables service filtering and classification in analysis
 * - Foundation for service lifecycle method extraction
 */
public class ServiceInfo extends ComponentInfo {
    private String foregroundServiceType;
    private boolean isolatedProcess = false;
    private boolean stopWithTask = false;

    public ServiceInfo() {
        super();
    }

    public ServiceInfo(String name) {
        super(name);
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.SERVICE;
    }

    // Service-specific getters and setters
    public String getForegroundServiceType() { return foregroundServiceType; }
    public void setForegroundServiceType(String foregroundServiceType) {
        this.foregroundServiceType = foregroundServiceType;
    }

    public boolean isIsolatedProcess() { return isolatedProcess; }
    public void setIsolatedProcess(boolean isolatedProcess) { this.isolatedProcess = isolatedProcess; }

    public boolean isStopWithTask() { return stopWithTask; }
    public void setStopWithTask(boolean stopWithTask) { this.stopWithTask = stopWithTask; }

    @Override
    public String toString() {
        return String.format("ServiceInfo [name=%s, foregroundServiceType=%s, isolatedProcess=%s, enabled=%s]",
                name, foregroundServiceType, isolatedProcess, enabled);
    }
}