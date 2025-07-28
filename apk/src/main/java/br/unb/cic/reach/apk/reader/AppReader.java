package br.unb.cic.reach.apk.reader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import br.unb.cic.reach.apk.model.ActivityInfo;
import br.unb.cic.reach.apk.model.AndroidAppInfo;
import br.unb.cic.reach.apk.model.BroadcastReceiverInfo;
import br.unb.cic.reach.apk.model.ContentProviderInfo;
import br.unb.cic.reach.apk.model.IntentFilterInfo;
import br.unb.cic.reach.apk.model.ServiceInfo;
import br.unb.cic.reach.apk.util.FileUtil;
import brut.androlib.ApkDecoder;
import brut.androlib.exceptions.AndrolibException;
import brut.directory.DirectoryException;
import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestActivity;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestBroadcastReceiver;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestContentProvider;
import soot.jimple.infoflow.android.manifest.binary.BinaryManifestService;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

/**
 * Android APK application information reader and processor.
 * <p>
 * This class provides comprehensive APK analysis capabilities including
 * manifest parsing, component extraction, and application decompilation
 * for all Android component types (Activities, Services, BroadcastReceivers,
 * ContentProviders).
 * <p>
 * ### Architectural Decisions:
 * - ProcessManifest integration for reliable manifest parsing
 * - Comprehensive component extraction across all Android types
 * - Resource parsing support for advanced analysis scenarios
 * - Decompilation support for source code analysis workflows
 * <p>
 * ### Role in the System:
 * - Primary interface for APK information extraction
 * - Foundation for Android-specific analysis processing
 * - Bridge between APK files and analysis algorithms
 * - Component discovery and classification for entry point analysis
 */
public class AppReader {
    private static final Logger log = LoggerFactory.getLogger(AppReader.class);

    // XML attribute constants
    private static final String PACKAGE = "package";
    private static final String NAME = "name";
    private static final String ENABLED = "enabled";
    private static final String EXPORTED = "exported";
    private static final String LABEL = "label";
    private static final String ICON = "icon";

    // Intent filter constants
    private static final String INTENT_FILTER = "intent-filter";
    private static final String ACTION = "action";
    private static final String CATEGORY = "category";
    private static final String DATA = "data";
    private static final String MAIN_ACTION = "android.intent.action.MAIN";

    // Activity specific constants
    private static final String TASK_AFFINITY = "taskAffinity";
    private static final String LAUNCH_MODE = "launchMode";
    private static final String SCREEN_ORIENTATION = "screenOrientation";

    // Service specific constants
    private static final String FOREGROUND_SERVICE_TYPE = "foregroundServiceType";
    private static final String ISOLATED_PROCESS = "isolatedProcess";
    private static final String STOP_WITH_TASK = "stopWithTask";

    // Receiver specific constants
    private static final String PRIORITY = "priority";

    // Provider specific constants
    private static final String AUTHORITIES = "authorities";
    private static final String READ_PERMISSION = "readPermission";
    private static final String WRITE_PERMISSION = "writePermission";
    private static final String GRANT_URI_PERMISSIONS = "grantUriPermissions";

    /**
     * Reads comprehensive application information from APK file.
     * <p>
     * Parses the AndroidManifest.xml file to extract all component types,
     * application metadata, and configuration information needed for
     * reachability analysis and entry point identification.
     * <p>
     * ### Processing Workflow:
     * - Initialize ProcessManifest with APK and resources
     * - Extract basic application information (package, name, label)
     * - Process each component type using specific extraction methods
     * - Validate extracted information for consistency
     *
     * @param apkPath Absolute path to the APK file to analyze
     * @return AndroidAppInfo instance containing all extracted component information
     * @throws IOException            If APK file cannot be read or is corrupted
     * @throws XmlPullParserException If manifest parsing fails
     */
    public static AndroidAppInfo readApk(String apkPath) throws IOException, XmlPullParserException {
        log.info("Reading APK: {}", apkPath);
        final File targetAPK = new File(apkPath);

        if (!targetAPK.exists() || !targetAPK.canRead()) {
            throw new IOException("APK file not found or not readable: " + apkPath);
        }

        AndroidAppInfo appInfo = new AndroidAppInfo(apkPath);

        // Initialize resource parser
        ARSCFileParser resources = new ARSCFileParser();
        resources.parse(targetAPK.getAbsolutePath());

        try (ProcessManifest processManifest = new ProcessManifest(targetAPK, resources)) {
            log.debug("Processing manifest for: {}", apkPath);

            // Extract basic application information
            extractBasicInfo(processManifest, appInfo);

            // Extract all component types
            extractActivities(processManifest, appInfo);
            extractServices(processManifest, appInfo);
            extractBroadcastReceivers(processManifest, appInfo);
            extractContentProviders(processManifest, appInfo);

            log.info("APK analysis completed: {} - {} total components",
                    appInfo.getFileName(), appInfo.getTotalComponentCount());
        }

        return appInfo;
    }

    /**
     * Extracts basic application information from manifest.
     * <p>
     * Processes the root manifest node to extract package name, application
     * name, and other basic metadata needed for application identification
     * and analysis configuration.
     */
    private static void extractBasicInfo(ProcessManifest processManifest, AndroidAppInfo appInfo) {
        AXmlNode manifest = processManifest.getManifest();

        String manifestPackage = getAttributeAsString(PACKAGE, manifest);
        log.debug("Package: {}", manifestPackage);
        appInfo.setPackageName(manifestPackage);

        String appName = processManifest.getApplication().getName();
        appInfo.setAppName(appName);
        log.debug("Application name: {}", appName);
    }

    /**
     * Extracts all activity components from manifest.
     * <p>
     * Processes activity declarations to extract comprehensive activity
     * information including launch modes, task affinity, and main activity
     * identification for entry point analysis.
     */
    private static void extractActivities(ProcessManifest processManifest, AndroidAppInfo appInfo) {
        log.debug("Extracting activities");

        for (BinaryManifestActivity binaryActivity : processManifest.getActivities()) {
            ActivityInfo activityInfo = readActivity(binaryActivity, appInfo.getPackageName());
            appInfo.addActivity(activityInfo);
        }

        log.debug("Extracted {} activities", appInfo.getActivities().size());
    }

    /**
     * Extracts all service components from manifest.
     * <p>
     * Processes service declarations to extract service-specific information
     * including foreground service configuration and process isolation settings.
     */
    private static void extractServices(ProcessManifest processManifest, AndroidAppInfo appInfo) {
        log.debug("Extracting services");

        for (BinaryManifestService binaryService : processManifest.getServices()) {
            ServiceInfo serviceInfo = readService(binaryService, appInfo.getPackageName());
            appInfo.addService(serviceInfo);
        }

        log.debug("Extracted {} services", appInfo.getServices().size());
    }

    /**
     * Extracts all broadcast receiver components from manifest.
     * <p>
     * Processes receiver declarations to extract receiver-specific information
     * including intent filters, priority settings, and action/category specifications.
     */
    private static void extractBroadcastReceivers(ProcessManifest processManifest, AndroidAppInfo appInfo) {
        log.debug("Extracting broadcast receivers");

        for (BinaryManifestBroadcastReceiver binaryReceiver : processManifest.getBroadcastReceivers()) {
            BroadcastReceiverInfo receiverInfo = readBroadcastReceiver(binaryReceiver, appInfo.getPackageName());
            appInfo.addBroadcastReceiver(receiverInfo);
        }

        log.debug("Extracted {} broadcast receivers", appInfo.getBroadcastReceivers().size());
    }

    /**
     * Extracts all content provider components from manifest.
     * <p>
     * Processes provider declarations to extract provider-specific information
     * including authority configuration, permission settings, and URI grants.
     */
    private static void extractContentProviders(ProcessManifest processManifest, AndroidAppInfo appInfo) {
        log.debug("Extracting content providers");

        for (BinaryManifestContentProvider binaryProvider : processManifest.getContentProviders()) {
            ContentProviderInfo providerInfo = readContentProvider(binaryProvider, appInfo.getPackageName());
            appInfo.addContentProvider(providerInfo);
        }

        log.debug("Extracted {} content providers", appInfo.getContentProviders().size());
    }

    /**
     * Reads detailed activity information from binary manifest.
     */
    private static ActivityInfo readActivity(BinaryManifestActivity binaryActivity, String manifestPackage) {
        AXmlNode activityNode = binaryActivity.getAXmlNode();
        String activityName = resolveComponentName(getAttributeAsString(NAME, activityNode), manifestPackage);

        log.debug("Reading activity: {}", activityName);

        ActivityInfo activityInfo = new ActivityInfo(activityName, false);

        // Extract basic attributes
        extractCommonAttributes(activityNode, activityInfo);

        // Extract activity-specific attributes
        activityInfo.setTaskAffinity(getAttributeAsString(TASK_AFFINITY, activityNode));
        activityInfo.setLaunchMode(getAttributeAsString(LAUNCH_MODE, activityNode));
        activityInfo.setScreenOrientation(getAttributeAsString(SCREEN_ORIENTATION, activityNode));

        // Check for main activity through intent filters
        boolean isMain = hasMainAction(activityNode);
        activityInfo.setMain(isMain);

        return activityInfo;
    }

    /**
     * Reads detailed service information from binary manifest.
     */
    private static ServiceInfo readService(BinaryManifestService binaryService, String manifestPackage) {
        AXmlNode serviceNode = binaryService.getAXmlNode();
        String serviceName = resolveComponentName(getAttributeAsString(NAME, serviceNode), manifestPackage);

        log.debug("Reading service: {}", serviceName);

        ServiceInfo serviceInfo = new ServiceInfo(serviceName);

        // Extract basic attributes
        extractCommonAttributes(serviceNode, serviceInfo);

        // Extract service-specific attributes
        serviceInfo.setForegroundServiceType(getAttributeAsString(FOREGROUND_SERVICE_TYPE, serviceNode));
        serviceInfo.setIsolatedProcess(getAttributeAsBoolean(ISOLATED_PROCESS, serviceNode, false));
        serviceInfo.setStopWithTask(getAttributeAsBoolean(STOP_WITH_TASK, serviceNode, false));

        return serviceInfo;
    }

    /**
     * Reads detailed broadcast receiver information from binary manifest.
     */
    private static BroadcastReceiverInfo readBroadcastReceiver(BinaryManifestBroadcastReceiver binaryReceiver,
                                                               String manifestPackage) {
        AXmlNode receiverNode = binaryReceiver.getAXmlNode();
        String receiverName = resolveComponentName(getAttributeAsString(NAME, receiverNode), manifestPackage);

        log.debug("Reading broadcast receiver: {}", receiverName);

        BroadcastReceiverInfo receiverInfo = new BroadcastReceiverInfo(receiverName);

        // Extract basic attributes
        extractCommonAttributes(receiverNode, receiverInfo);

        // Extract receiver-specific attributes
        receiverInfo.setPriority(getAttributeAsInt(PRIORITY, receiverNode, 0));

        // Extract intent filters
        List<IntentFilterInfo> intentFilters = extractIntentFilters(receiverNode);
        receiverInfo.setIntentFilters(intentFilters);

        return receiverInfo;
    }

    /**
     * Reads detailed content provider information from binary manifest.
     */
    private static ContentProviderInfo readContentProvider(BinaryManifestContentProvider binaryProvider,
                                                           String manifestPackage) {
        AXmlNode providerNode = binaryProvider.getAXmlNode();
        String providerName = resolveComponentName(getAttributeAsString(NAME, providerNode), manifestPackage);

        log.debug("Reading content provider: {}", providerName);

        ContentProviderInfo providerInfo = new ContentProviderInfo(providerName);

        // Extract basic attributes
        extractCommonAttributes(providerNode, providerInfo);

        // Extract provider-specific attributes
        providerInfo.setAuthorities(getAttributeAsString(AUTHORITIES, providerNode));
        providerInfo.setReadPermission(getAttributeAsString(READ_PERMISSION, providerNode));
        providerInfo.setWritePermission(getAttributeAsString(WRITE_PERMISSION, providerNode));
        providerInfo.setGrantUriPermissions(getAttributeAsBoolean(GRANT_URI_PERMISSIONS, providerNode, false));

        return providerInfo;
    }

    /**
     * Extracts common attributes shared across all component types.
     */
    private static void extractCommonAttributes(AXmlNode componentNode,
                                                br.unb.cic.reach.apk.model.ComponentInfo componentInfo) {
        componentInfo.setEnabled(getAttributeAsBoolean(ENABLED, componentNode, true));
        componentInfo.setExported(getAttributeAsBoolean(EXPORTED, componentNode, false));
        componentInfo.setLabel(getAttributeAsString(LABEL, componentNode));
        componentInfo.setIcon(getAttributeAsString(ICON, componentNode));
    }

    /**
     * Extracts intent filter information from component node.
     */
    private static List<IntentFilterInfo> extractIntentFilters(AXmlNode componentNode) {
        List<IntentFilterInfo> intentFilters = new ArrayList<>();

        for (AXmlNode child : componentNode.getChildren()) {
            if (INTENT_FILTER.equals(child.getTag())) {
                IntentFilterInfo intentFilter = readIntentFilter(child);
                intentFilters.add(intentFilter);
            }
        }

        return intentFilters;
    }

    /**
     * Reads intent filter information from XML node.
     */
    private static IntentFilterInfo readIntentFilter(AXmlNode intentFilterNode) {
        IntentFilterInfo intentFilter = new IntentFilterInfo();

        // Extract priority
        intentFilter.setPriority(getAttributeAsInt(PRIORITY, intentFilterNode, 0));

        // Extract actions, categories, and data
        for (AXmlNode child : intentFilterNode.getChildren()) {
            switch (child.getTag()) {
                case ACTION:
                    String action = getAttributeAsString(NAME, child);
                    if (action != null) {
                        intentFilter.addAction(action);
                    }
                    break;
                case CATEGORY:
                    String category = getAttributeAsString(NAME, child);
                    if (category != null) {
                        intentFilter.addCategory(category);
                    }
                    break;
                case DATA:
                    String scheme = getAttributeAsString("scheme", child);
                    if (scheme != null) {
                        intentFilter.addDataScheme(scheme);
                    }
                    String mimeType = getAttributeAsString("mimeType", child);
                    if (mimeType != null) {
                        intentFilter.addDataMimeType(mimeType);
                    }
                    break;
            }
        }

        return intentFilter;
    }

    /**
     * Checks if an activity has the main action intent filter.
     */
    private static boolean hasMainAction(AXmlNode activityNode) {
        for (AXmlNode child : activityNode.getChildren()) {
            if (INTENT_FILTER.equals(child.getTag())) {
                for (AXmlNode grandchild : child.getChildren()) {
                    if (ACTION.equals(grandchild.getTag())) {
                        String actionName = getAttributeAsString(NAME, grandchild);
                        if (MAIN_ACTION.equals(actionName)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Resolves component name handling relative names.
     */
    private static String resolveComponentName(String componentName, String manifestPackage) {
        if (componentName == null) {
            return null;
        }

        if (componentName.startsWith(".")) {
            return manifestPackage + componentName;
        }

        return componentName;
    }

    /**
     * Extracts string attribute value from XML node.
     */
    private static String getAttributeAsString(String attributeName, AXmlNode node) {
        AXmlAttribute<?> attribute = node.getAttribute(attributeName);
        if (attribute == null) {
            return null;
        }
        Object value = attribute.getValue();
        return value != null ? value.toString() : null;
    }

    /**
     * Extracts boolean attribute value from XML node.
     */
    private static boolean getAttributeAsBoolean(String attributeName, AXmlNode node, boolean defaultValue) {
        String value = getAttributeAsString(attributeName, node);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Extracts integer attribute value from XML node.
     */
    private static int getAttributeAsInt(String attributeName, AXmlNode node, int defaultValue) {
        String value = getAttributeAsString(attributeName, node);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.debug("Invalid integer value for {}: {}", attributeName, value);
            return defaultValue;
        }
    }

    /**
     * Decompiles APK file for source code analysis.
     * <p>
     * Creates decompiled source code from APK file for advanced analysis
     * scenarios that require access to application source code and resources.
     * <p>
     * ### Implementation Notes:
     * - Creates temporary directory for decompiled content
     * - Uses ApkDecoder for reliable APK decompilation
     * - Provides comprehensive error handling and cleanup
     * - Returns directory containing decompiled application resources
     *
     * @param appInfo Application information for context and naming
     * @return File representing directory containing decompiled content
     * @throws IOException if decompilation fails or file system errors occur
     */
    public static File decompileApp(AndroidAppInfo appInfo) throws IOException {
        log.info("Decompiling app: {}", appInfo.getPath());

        String appLabel = appInfo.getLabel();
        File outDir = FileUtil.createTempDirectory(appLabel);
        ApkDecoder decoder = new ApkDecoder(new File(appInfo.getPath()));

        try {
            // Clean any existing content
            FileUtil.delete(outDir);

            // Perform decompilation
            decoder.decode(outDir);

            log.info("Decompiled '{}' successfully to: {}", appLabel, outDir.getAbsolutePath());
            return outDir;

        } catch (AndrolibException | DirectoryException e) {
            log.error("Decompilation failed for '{}': {}", appLabel, e.getMessage(), e);

            // Cleanup on failure
            try {
                FileUtil.delete(outDir);
            } catch (IOException cleanupError) {
                log.warn("Failed to cleanup after decompilation error: {}", cleanupError.getMessage());
            }

            throw new IOException("Error decompiling APK: " + appInfo.getPath(), e);
        }
    }
}