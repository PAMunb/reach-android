package br.unb.cic.reach.apk.model;

import br.unb.cic.reach.common.model.ComponentType;

/**
 * ContentProvider component information with provider-specific attributes.
 *
 * Represents Android content provider declarations from manifest including
 * authority configuration, permission settings, and URI grant specifications
 * for CRUD method entry point analysis.
 *
 * ### Architectural Decisions:
 * - Extends ComponentInfo for consistent component modeling
 * - Authority and permission configuration for security analysis
 * - URI grant support for cross-application data sharing analysis
 * - CRUD method identification for content provider entry points
 *
 * ### Role in the System:
 * - Represents content provider components in Android applications
 * - Provides provider-specific entry point identification
 * - Enables provider filtering and classification in analysis
 * - Foundation for content provider CRUD method extraction
 */
public class ContentProviderInfo extends ComponentInfo {
    private String authorities;
    private String readPermission;
    private String writePermission;
    private boolean grantUriPermissions = false;

    public ContentProviderInfo() {
        super();
    }

    public ContentProviderInfo(String name) {
        super(name);
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PROVIDER;
    }

    // Provider-specific getters and setters
    public String getAuthorities() { return authorities; }
    public void setAuthorities(String authorities) { this.authorities = authorities; }

    public String getReadPermission() { return readPermission; }
    public void setReadPermission(String readPermission) { this.readPermission = readPermission; }

    public String getWritePermission() { return writePermission; }
    public void setWritePermission(String writePermission) { this.writePermission = writePermission; }

    public boolean isGrantUriPermissions() { return grantUriPermissions; }
    public void setGrantUriPermissions(boolean grantUriPermissions) {
        this.grantUriPermissions = grantUriPermissions;
    }

    @Override
    public String toString() {
        return String.format("ContentProviderInfo [name=%s, authorities=%s, readPermission=%s, enabled=%s]",
                name, authorities, readPermission, enabled);
    }
}