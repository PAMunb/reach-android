package br.unb.cic.reach.android;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.apk.model.ComponentInfo;
import br.unb.cic.reach.apk.util.AndroidUtil;
import br.unb.cic.reach.common.model.ComponentType;
import br.unb.cic.reach.common.model.EntryPoint;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

/**
 * Entry point extraction for Android applications across all component types.
 *
 * This class identifies all potential entry points for Android applications,
 * including lifecycle methods and public/protected methods that can be invoked
 * by the Android runtime or other components across Activities, Services,
 * BroadcastReceivers, and ContentProviders.
 *
 * ### Architectural Decisions:
 * - Component-specific lifecycle method identification
 * - Public and protected method inclusion for framework invocation
 * - Inner class support for comprehensive component analysis
 * - Configurable filtering based on component types and analysis scope
 *
 * ### Role in the System:
 * - Primary entry point identification for Android reachability analysis
 * - Bridge between component information and analysis algorithms
 * - Foundation for comprehensive Android application coverage
 * - Integration point for component-specific analysis configuration
 */
public class EntryPointExtractor {
    private static final Logger log = LoggerFactory.getLogger(EntryPointExtractor.class);

    /**
     * Extracts entry points from Android components with comprehensive coverage.
     *
     * Identifies all potential entry points across the provided Android components,
     * including lifecycle methods specific to each component type and public/protected
     * methods that can be invoked by the Android framework or other components.
     *
     * ### Entry Point Categories:
     * - Lifecycle methods specific to each component type
     * - Public and protected methods accessible to Android framework
     * - Methods in inner classes for comprehensive component coverage
     * - Callback methods registered through framework APIs
     *
     * @param components Set of Android component information objects
     * @param appInfo Application information for context and filtering
     * @return Set of EntryPoint objects representing analysis starting points
     */
    public static Set<EntryPoint> extractEntryPoints(Set<ComponentInfo> components,
                                                     br.unb.cic.reach.apk.model.AndroidAppInfo androidAppInfo) {

        log.info("Extracting entry points from {} components", components.size());

        Set<EntryPoint> entryPoints = new HashSet<>();

        for (ComponentInfo component : components) {
            log.debug("Processing component: {} ({})", component.getName(), component.getComponentType());

            // Get all classes for this component (including inner classes)
            List<SootClass> componentClasses = getComponentClasses(component, androidAppInfo);

            for (SootClass clazz : componentClasses) {
                // Extract lifecycle methods
                Set<EntryPoint> lifecycleEntryPoints = extractLifecycleEntryPoints(clazz, component);
                entryPoints.addAll(lifecycleEntryPoints);

                // Extract public and protected methods
                Set<EntryPoint> publicProtectedEntryPoints = extractPublicProtectedEntryPoints(clazz, component, androidAppInfo);
                entryPoints.addAll(publicProtectedEntryPoints);
            }
        }

        log.info("Extracted {} total entry points", entryPoints.size());

        if (log.isDebugEnabled()) {
            entryPoints.forEach(ep -> log.debug(" - {} [{}]",
                    ep.getSignature(), ep.getComponentType()));
        }

        return entryPoints;
    }

    /**
     * Extracts lifecycle methods specific to component type.
     *
     * Identifies and creates entry points for lifecycle methods that are
     * automatically invoked by the Android framework based on component
     * type and application lifecycle events.
     *
     * ### Lifecycle Method Coverage:
     * - Activity: onCreate, onStart, onResume, onPause, onStop, onDestroy, etc.
     * - Service: onCreate, onDestroy, onStartCommand, onBind, onUnbind, etc.
     * - BroadcastReceiver: onReceive
     * - ContentProvider: onCreate, query, insert, update, delete, getType
     *
     * @param clazz The SootClass to analyze for lifecycle methods
     * @param component Component information for context and type identification
     * @return Set of lifecycle method entry points
     */
    private static Set<EntryPoint> extractLifecycleEntryPoints(SootClass clazz, ComponentInfo component) {
        Set<EntryPoint> lifecycleEntryPoints = new HashSet<>();

        List<String> lifecycleMethodNames = component.getLifecycleMethods();
        ComponentType componentType = component.getComponentType();

        for (String methodName : lifecycleMethodNames) {
            try {
                // Find lifecycle method by name (may have different signatures)
                List<SootMethod> methods = clazz.getMethods().stream()
                        .filter(method -> method.getName().equals(methodName))
                        .filter(method -> isValidLifecycleMethod(method, methodName, componentType))
                        .collect(Collectors.toList());

                for (SootMethod method : methods) {
                    boolean isMainComponent = isMainComponent(component);
                    EntryPoint entryPoint = new EntryPoint(method, componentType, isMainComponent);
                    lifecycleEntryPoints.add(entryPoint);

                    log.debug("  + Lifecycle entry point: {}", method.getSignature());
                }

            } catch (RuntimeException e) {
                log.debug("Lifecycle method {} not found in {}: {}",
                        methodName, clazz.getName(), e.getMessage());
            }
        }

        return lifecycleEntryPoints;
    }

    /**
     * Extracts public and protected methods as potential entry points.
     *
     * Identifies methods that can be invoked by the Android framework,
     * other components, or reflection-based mechanisms, providing comprehensive
     * coverage for framework interactions and component communication.
     *
     * ### Method Selection Criteria:
     * - Public or protected visibility for external access
     * - Concrete implementation (not abstract)
     * - Non-constructor methods (handled separately)
     * - Application package methods to avoid framework noise
     *
     * @param clazz The SootClass to analyze for entry point methods
     * @param component Component information for context
     * @param appInfo Application information for filtering
     * @return Set of public/protected method entry points
     */
    private static Set<EntryPoint> extractPublicProtectedEntryPoints(SootClass clazz,
                                                                     ComponentInfo component,
                                                                     br.unb.cic.reach.apk.model.AndroidAppInfo androidAppInfo) {

        Set<EntryPoint> publicProtectedEntryPoints = new HashSet<>();
        ComponentType componentType = component.getComponentType();
        boolean isMainComponent = isMainComponent(component);

        for (SootMethod method : clazz.getMethods()) {
            if (isValidEntryPointMethod(method, androidAppInfo)) {
                EntryPoint entryPoint = new EntryPoint(method, componentType, isMainComponent);
                publicProtectedEntryPoints.add(entryPoint);

                log.debug("  + Public/Protected entry point: {}", method.getSignature());
            }
        }

        return publicProtectedEntryPoints;
    }

    /**
     * Gets all classes associated with a component including inner classes.
     *
     * Identifies all SootClass objects that correspond to the component,
     * including inner classes and nested classes that may contain entry
     * point methods for comprehensive component analysis coverage.
     *
     * ### Class Discovery Strategy:
     * - Exact name matching for primary component class
     * - Prefix matching for inner classes (ComponentName$InnerClass)
     * - Application package filtering to avoid system class inclusion
     * - Validation of class existence and accessibility
     *
     * @param component Component information for class identification
     * @param appInfo Application information for filtering context
     * @return List of SootClass objects representing component classes
     */
    private static List<SootClass> getComponentClasses(ComponentInfo component,
                                                       br.unb.cic.reach.apk.model.AndroidAppInfo androidAppInfo) {

        String componentName = component.getName();

        return Scene.v().getApplicationClasses().stream()
                .filter(clazz -> isComponentClass(clazz, componentName))
                .filter(clazz -> AndroidUtil.isClassInApplicationPackage(clazz, androidAppInfo))
                .collect(Collectors.toList());
    }

    /**
     * Determines if a class belongs to the specified component.
     *
     * Checks if the SootClass represents the component class or an inner
     * class of the component, enabling comprehensive component coverage
     * including nested class entry points.
     *
     * @param clazz The SootClass to check
     * @param componentName The component name to match against
     * @return true if the class belongs to the component
     */
    private static boolean isComponentClass(SootClass clazz, String componentName) {
        String className = clazz.getName();

        // Exact match for primary component class
        if (className.equals(componentName)) {
            return true;
        }

        // Inner class matching (ComponentName$InnerClass)
        return className.startsWith(componentName + "$");
    }

    /**
     * Validates if a method is a valid lifecycle method for the component type.
     *
     * Checks method signature compatibility with expected lifecycle method
     * patterns for the specific component type, ensuring accurate lifecycle
     * method identification across different Android API levels.
     *
     * @param method The SootMethod to validate
     * @param methodName The expected lifecycle method name
     * @param componentType The component type for context
     * @return true if the method is a valid lifecycle method
     */
    private static boolean isValidLifecycleMethod(SootMethod method, String methodName, ComponentType componentType) {
        // Basic validation for all lifecycle methods
        if (!method.isConcrete() || method.isPrivate()) {
            return false;
        }

        // Component-specific validation
        return switch (componentType) {
            case ACTIVITY -> validateActivityLifecycleMethod(method, methodName);
            case SERVICE -> validateServiceLifecycleMethod(method, methodName);
            case RECEIVER -> validateReceiverLifecycleMethod(method, methodName);
            case PROVIDER -> validateProviderLifecycleMethod(method, methodName);
            default -> true; // Default acceptance for unknown types
        };
    }

    /**
     * Validates Activity lifecycle method signatures.
     */
    private static boolean validateActivityLifecycleMethod(SootMethod method, String methodName) {
        return switch (methodName) {
            case "onCreate" -> method.getParameterCount() == 1; // Bundle parameter
            case "onStart", "onResume", "onPause", "onStop", "onDestroy" -> method.getParameterCount() == 0;
            case "onSaveInstanceState", "onRestoreInstanceState" -> method.getParameterCount() == 1; // Bundle
            case "onActivityResult" -> method.getParameterCount() == 3; // int, int, Intent
            default -> true; // Allow other activity methods
        };
    }

    /**
     * Validates Service lifecycle method signatures.
     */
    private static boolean validateServiceLifecycleMethod(SootMethod method, String methodName) {
        return switch (methodName) {
            case "onCreate", "onDestroy" -> method.getParameterCount() == 0;
            case "onStartCommand" -> method.getParameterCount() == 3; // Intent, int, int
            case "onBind" -> method.getParameterCount() == 1; // Intent
            case "onUnbind", "onRebind" -> method.getParameterCount() == 1; // Intent
            default -> true; // Allow other service methods
        };
    }

    /**
     * Validates BroadcastReceiver lifecycle method signatures.
     */
    private static boolean validateReceiverLifecycleMethod(SootMethod method, String methodName) {
        return switch (methodName) {
            case "onReceive" -> method.getParameterCount() == 2; // Context, Intent
            default -> true; // Allow other receiver methods
        };
    }

    /**
     * Validates ContentProvider lifecycle method signatures.
     */
    private static boolean validateProviderLifecycleMethod(SootMethod method, String methodName) {
        return switch (methodName) {
            case "onCreate" -> method.getParameterCount() == 0;
            case "query" -> method.getParameterCount() >= 4; // Uri, projection, selection, selectionArgs, sortOrder
            case "insert" -> method.getParameterCount() == 2; // Uri, ContentValues
            case "update" -> method.getParameterCount() == 4; // Uri, ContentValues, selection, selectionArgs
            case "delete" -> method.getParameterCount() == 3; // Uri, selection, selectionArgs
            case "getType" -> method.getParameterCount() == 1; // Uri
            default -> true; // Allow other provider methods
        };
    }

    /**
     * Validates if a method is a valid entry point for reachability analysis.
     *
     * Checks method characteristics to determine if it can serve as an entry
     * point for reachability analysis, filtering out methods that cannot be
     * invoked externally or are not relevant for analysis.
     *
     * ### Validation Criteria:
     * - Concrete implementation (not abstract)
     * - Non-constructor methods
     * - Public or protected visibility
     * - Application-specific methods (not framework methods)
     *
     * @param method The SootMethod to validate
     * @param appInfo Application information for context filtering
     * @return true if the method is a valid entry point
     */
    private static boolean isValidEntryPointMethod(SootMethod method, br.unb.cic.reach.apk.model.AndroidAppInfo androidAppInfo) {
        // Basic method validation
        if (!method.isConcrete() || method.isConstructor() || method.isPrivate()) {
            return false;
        }

        // Visibility check - must be public or protected for external access
        if (!method.isPublic() && !method.isProtected()) {
            return false;
        }

        // Application package check to avoid framework method noise
        return AndroidUtil.isClassInApplicationPackage(method.getDeclaringClass(), androidAppInfo);
    }

    /**
     * Determines if a component is the main component of the application.
     *
     * Identifies main components (typically main activities) for analysis
     * prioritization and entry point classification.
     *
     * @param component The component to check
     * @return true if the component is the main component
     */
    private static boolean isMainComponent(ComponentInfo component) {
        return switch (component.getComponentType()) {
            case ACTIVITY -> {
                if (component instanceof br.unb.cic.reach.apk.model.ActivityInfo) {
                    yield ((br.unb.cic.reach.apk.model.ActivityInfo) component).isMain();
                }
                yield false;
            }
            default -> false; // Only activities can be main components in Android
        };
    }
}