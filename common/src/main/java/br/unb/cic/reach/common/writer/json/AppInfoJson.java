package br.unb.cic.reach.common.writer.json;

import br.unb.cic.reach.common.model.AppInfo;

/**
 * JSON representation of application information.
 */
public class AppInfoJson {
    private String path;
    private String packageName;
    private String appName;
    private String type;
    
    public AppInfoJson(AppInfo appInfo) {
        this.path = appInfo.getPath();
        this.packageName = appInfo.getPackageName();
        this.appName = appInfo.getAppName();
        this.type = appInfo.getType() != null ? appInfo.getType().name() : "UNKNOWN";
    }
    
    // Getters for JSON serialization
    public String getPath() { return path; }
    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public String getType() { return type; }
}