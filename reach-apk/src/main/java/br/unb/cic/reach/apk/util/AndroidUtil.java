package br.unb.cic.reach.apk.util;

import java.util.Arrays;
import java.util.List;

import br.unb.cic.reach.apk.model.AndroidAppInfo;
import br.unb.cic.reach.common.model.AppInfo;
import soot.SootClass;
import soot.SootMethod;

/**
 * Utility class for Android-specific operations and classifications.
 * <p>
 * This class provides utility methods for identifying Android framework
 * methods, application package classification, and system package detection
 * to support filtering and analysis scope configuration.
 * <p>
 * ### Architectural Decisions:
 * - Static utility methods for framework integration
 * - Configurable system package lists for different Android versions
 * - Application package detection with build artifact exclusion
 * - Integration with both legacy and new AppInfo structures
 * <p>
 * ### Role in the System:
 * - Framework method identification for analysis filtering
 * - Application scope determination for package-based filtering
 * - System package detection for analysis scope configuration
 * - Utility foundation for Android-specific analysis operations
 */
public class AndroidUtil {

    private static final List<String> ANDROID_FRAMEWORK_PREFIXES = Arrays.asList(
            "android.", "com.google.android", "androidx."
    );

    private static final List<String> SYSTEM_PACKAGE_PREFIXES = Arrays.asList(
            "android.", "androidx.", "java.", "javax.",
            "sun.", "org.omg.", "org.w3c.dom.",
            "com.google.", "com.android."
    );

    /**
     * Checks if a method belongs to the Android framework.
     * <p>
     * Determines if the given method is part of the Android framework
     * based on package name prefixes, enabling framework method filtering
     * during analysis and entry point identification.
     *
     * @param sootMethod The method to check
     * @return true if the method belongs to Android framework, false otherwise
     */
    public static boolean isAndroidMethod(SootMethod sootMethod) {
        String className = sootMethod.getDeclaringClass().getName();
        return ANDROID_FRAMEWORK_PREFIXES.stream()
                .anyMatch(className::startsWith);
    }

    /**
     * Checks if a class belongs to the application package.
     * <p>
     * Determines if the given class is part of the application's main package
     * based on package name matching, excluding generated classes like R.java
     * and BuildConfig.java that are not relevant for analysis.
     *
     * @param clazz   The class to check
     * @param appInfo The application information containing package details
     * @return true if the class belongs to the application package
     */
    public static boolean isClassInApplicationPackage(SootClass clazz, AppInfo appInfo) {
        return isClassInApplicationPackage(clazz, appInfo.getPackageName());
    }
    
    /**
     * Checks if a class belongs to the Android application package.
     * 
     * @param clazz The class to check
     * @param androidAppInfo The Android application information containing package details
     * @return true if the class belongs to the application package
     */
    public static boolean isClassInApplicationPackage(SootClass clazz, AndroidAppInfo androidAppInfo) {
        return isClassInApplicationPackage(clazz, androidAppInfo.getPackageName());
    }

    /**
     * Checks if a class belongs to the specified application package.
     * <p>
     * Determines if the given class is part of the specified package
     * while excluding generated build artifacts that are not relevant
     * for reachability analysis.
     * <p>
     * ### Implementation Notes:
     * - Excludes R.java resource classes (app.package.R*)
     * - Excludes BuildConfig.java configuration classes
     * - Uses prefix matching for package hierarchy inclusion
     *
     * @param clazz      The class to check
     * @param appPackage The application package name
     * @return true if the class belongs to the application package
     */
    public static boolean isClassInApplicationPackage(SootClass clazz, String appPackage) {
        if (appPackage == null || appPackage.isEmpty()) {
            return false;
        }

        String className = clazz.getName();
        return className.startsWith(appPackage)
                && !className.startsWith(appPackage + ".R")
                && !className.startsWith(appPackage + ".BuildConfig");
    }

    /**
     * Checks if a class belongs to system packages.
     * <p>
     * Determines if the given class is part of the Android framework,
     * Java standard library, or other system packages that should
     * typically be excluded from application-specific analysis.
     * <p>
     * ### System Package Categories:
     * - Android framework (android.*, androidx.*)
     * - Java standard library (java.*, javax.*)
     * - Google services (com.google.*, com.android.*)
     * - Other system packages (sun.*, org.omg.*, org.w3c.dom.*)
     *
     * @param className The full class name to check
     * @return true if the class belongs to system packages, false otherwise
     */
    public static boolean isClassInSystemPackage(String className) {
        return isClassInSystemPackage(className, SYSTEM_PACKAGE_PREFIXES);
    }

    /**
     * Checks if a class belongs to system packages using custom prefix list.
     * <p>
     * Enables configurable system package detection for different Android
     * versions or analysis requirements by accepting custom package prefix lists.
     *
     * @param className      The full class name to check
     * @param systemPackages List of system package prefixes
     * @return true if the class belongs to any of the system packages
     */
    public static boolean isClassInSystemPackage(String className, List<String> systemPackages) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        return systemPackages.stream().anyMatch(className::startsWith);
    }

    /**
     * Determines if a class is an application class (non-system).
     * <p>
     * Convenience method for identifying classes that belong to the application
     * rather than system packages, useful for analysis scope determination.
     *
     * @param clazz The class to check
     * @return true if the class is an application class, false if system class
     */
    public static boolean isApplicationClass(SootClass clazz) {
        return !isClassInSystemPackage(clazz.getName());
    }

    /**
     * Extracts package name from full class name.
     * <p>
     * Utility method for extracting package information from class names
     * during manifest parsing and component analysis.
     *
     * @param className The full class name
     * @return Package name or empty string if no package
     */
    public static String extractPackageName(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }

    /**
     * Extracts simple class name from full class name.
     * <p>
     * Utility method for extracting class name without package information
     * for display and analysis purposes.
     *
     * @param className The full class name
     * @return Simple class name without package
     */
    public static String extractSimpleClassName(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(lastDot + 1) : className;
    }
}