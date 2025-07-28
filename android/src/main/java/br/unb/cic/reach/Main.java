package br.unb.cic.reach;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import com.beust.jcommander.JCommander;

import br.unb.cic.reach.apk.model.ActivityInfo;
import br.unb.cic.reach.apk.model.AppInfo;
import br.unb.cic.reach.apk.reader.AppReader;
import br.unb.cic.reach.analysis.ReachabilityAnalysis;
import br.unb.cic.reach.analysis.ReachabilityStrategy;
import br.unb.cic.reach.analysis.SootReachabilityStrategy;
import br.unb.cic.reach.cli.CommandLineArgs;
import br.unb.cic.reach.model.Path;
import br.unb.cic.reach.model.ReachClass;
import br.unb.cic.reach.writer.CsvWriter;
import br.unb.cic.reach.writer.Writer;
import br.unb.cic.reach.writer.WriterFactory;
import br.unb.cic.reach.writer.WriterType;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.SetupApplication;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public void execute(String apkPath, String targetMethodsFile, String androidPlatformsDir, String rtJarPath, String resultsFile, Writer writer, boolean checkOnlyInAppPackage, int timeout) throws Exception {
        log.info("Executing ...");

        // get application info
        AppInfo appInfo = AppReader.readApk(apkPath);
        log.info("App info: " + appInfo);

        // initialize soot (infoflow)
        SetupApplication infoflow = SootConfig.initialize(apkPath, androidPlatformsDir, rtJarPath, timeout);

        // target methods
        Set<SootMethod> targetMethods = getTargetMethods(targetMethodsFile);

        // the list of all activities (with inner classes)
        List<SootClass> activities = getActivitiesWithInnerClasses(appInfo);

        // Entrypoints: set of methods (public or protected) of each activity
        Set<SootMethod> entryPoints = getEntrypoints(activities, appInfo);

        log.info("Constructing callgraph ...");
        infoflow.constructCallgraph();

        ReachabilityStrategy<SootMethod, Path> analysisStrategy = new SootReachabilityStrategy(); // TODO vir como parametro (CLI)
//		ReachabilityStrategy<SootMethod, Path> analysisStrategy = new JGraphReachabilityStrategy();

        Set<ReachClass> result = reachabilityAnalysis(appInfo, targetMethods, entryPoints, analysisStrategy);

        writeResults(result, resultsFile, writer);
    }

    private Set<ReachClass> reachabilityAnalysis(AppInfo appInfo, Set<SootMethod> targetMethods, Set<SootMethod> entryPoints, ReachabilityStrategy<SootMethod, Path> analysisStrategy) throws IOException, XmlPullParserException {

//		System.out.println("*************************************");
//		ReachableMethods reachableMethods = Scene.v().getReachableMethods();
//		QueueReader<MethodOrMethodContext> listener = reachableMethods.listener();
//		while (listener.hasNext()) {
//			MethodOrMethodContext next = listener.next();
//			SootMethod method = next.method();
//			if (method.getDeclaringClass().getPackageName().contains(appInfo.getPackage())
//					|| method.getDeclaringClass().getPackageName().contains("java.security")) {
//				System.out.println(next.method().getSignature());
//			}
//		}
//		System.out.println("************************************* FIM");

        ReachabilityAnalysis analysis = new ReachabilityAnalysis(appInfo, targetMethods, entryPoints);
        return analysis.reachabilityAnalysis(analysisStrategy);
    }

    private void writeResults(Set<ReachClass> result, String resultsFile, Writer writer) {
        writer.write(result, new File(resultsFile));
        log.info("Results saved in: " + resultsFile);
    }

    private Set<SootMethod> getEntrypoints(List<SootClass> activities, AppInfo appInfo) {
        Set<SootMethod> entryPoints = new HashSet<>();

        List<SootClass> a = getActivities(appInfo);

        for (SootClass clazz : activities) {
            for (SootMethod method : clazz.getMethods()) {
                if (isValidEntrypoint(method, appInfo)) {
                    entryPoints.add(method);
                }
            }
        }
        log.info("EntryPoints: " + entryPoints.size());
        entryPoints.forEach(m -> log.debug(" - " + m.getSignature()));
        return entryPoints;
    }

    private List<SootClass> getActivities(AppInfo appInfo){
        List<SootClass> activities = new ArrayList<>();// all activities'window node
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            for (ActivityInfo activityInfo : appInfo.getActivities()) {
                if (clazz.getName().startsWith(activityInfo.getName())) { // include inner classes
//				if (actInfo.getName().equals(clazz.getName())) {
                    activities.add(clazz);
                }
            }
        }
        log.info("Activities: " + activities.size());
        activities.forEach(m -> log.debug(" - " + m.getName()));
        return activities;
    }

    private boolean isValidEntrypoint(SootMethod sootMethod, AppInfo appInfo) {
        return sootMethod.isConcrete() && !sootMethod.isConstructor() && !sootMethod.isPrivate();
    }

    private Set<SootMethod> getTargetMethods(String methodsFile) {
        log.info("Target methods file: " + methodsFile);
        Set<SootMethod> sootMethods = new HashSet<>();
        List<String> methodsSignatures = readMethodsFile(methodsFile);
        for (String methodSignature : methodsSignatures) {
            SootMethod sootMethod = Scene.v().getMethod(methodSignature);
            sootMethods.add(sootMethod);
        }
        return sootMethods;
    }

    public static List<String> readMethodsFile(String fileName) {
        List<String> signatures = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Trim leading/trailing whitespace from the line
                line = line.trim();
                // Ignore blank lines and lines starting with '#' or '//'
                if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith("//")) {
                    signatures.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
        return signatures;
    }

    private List<SootClass> getActivitiesWithInnerClasses(AppInfo appInfo) {
        List<SootClass> activities = new ArrayList<>();// all activities'window node
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            for (ActivityInfo activityInfo : appInfo.getActivities()) {
                if (clazz.getName().startsWith(activityInfo.getName())) { // include inner classes
//				if (actInfo.getName().equals(clazz.getName())) {
                    activities.add(clazz);
                }
            }
        }
        log.info("Activities: " + activities.size());
        activities.forEach(m -> log.debug(" - " + m.getName()));
        return activities;
    }

    public static void main(String[] args) {
        execute(args);
    }

    private static void execute(String[] args) {
        long start = System.currentTimeMillis();

//        executeCLI(args);
        executeTest();

        long time = System.currentTimeMillis() - start;
        log.info("Executed in " + (time / 1000) + " seconds.");
    }

    private static void executeCLI(String[] args) {
        CommandLineArgs jArgs = new CommandLineArgs();
        JCommander jc = JCommander.newBuilder().addObject(jArgs).build();

        if (args.length == 0) {
            jc.usage();
            return;
        }

        jc.parse(args);

        String androidPlatformsDir = jArgs.getAndroidDir();
        String methodsFile = jArgs.getMethodsFile();
        String rtJarPath = jArgs.getRtJar();
        String apk = jArgs.getApk();
        String outputFile = jArgs.getOutputFile();
        int timeout = jArgs.getTimeout();
        boolean checkOnlyInAppPackage = !jArgs.isFull();
        boolean debug = jArgs.isDebug();
        WriterType writerType = jArgs.getWriterType();
        Writer writer = WriterFactory.fromType(writerType);

        if (debug) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(ch.qos.logback.classic.Level.DEBUG);
        }

        log.info("Starting analysis ...");
        Main main = new Main();
        try {
            main.execute(apk, methodsFile, androidPlatformsDir, rtJarPath, outputFile, writer, checkOnlyInAppPackage, timeout);
            log.info("Analysis completed");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void executeTest() {
        String rvsecDir = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/rvsec";
        String methodsFile = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/methodsFile.txt";
//		String apksDir = rvsecDir + "/rv-android/apks_exp02/";
        String apksDir = "/home/pedro/desenvolvimento/RV_ANDROID/ALL_APKS";

        String androidPlatformsDir = "/home/pedro/desenvolvimento/aplicativos/android/sdk/platforms";
//		String androidPlatformsDir = "/home/pedro/desenvolvimento/aplicativos/android/platforms-sable";
        String rtJarPath = "/home/pedro/.sdkman/candidates/java/8.0.302-open/jre/lib/rt.jar";

//		String apk = apksDir + "cryptoapp.apk";
//		String apk = apksDir + "com.blogspot.e_kanivets.moneytracker_38.apk";
        String apk = apksDir + "/com.gianlu.dnshero_40.apk";
//		String apk = apksDir + "com.github.axet.hourlyreminder_476.apk";
//		String apk = apksDir + "com.pindroid_69.apk";
//		String apk = apksDir + "com.rafapps.simplenotes_7.apk";
//		String apk = apksDir + "com.thibaudperso.sonycamera_24.apk";
//		String apk = apksDir + "li.klass.fhem_141.apk";
//		String apk = apksDir + "org.pulpdust.lesserpad_42.apk";
//		String apk = apksDir + "org.secuso.privacyfriendlydicer_8.apk";
//		String apk = apksDir + "org.secuso.privacyfriendlyludo_5.apk";

        boolean checkOnlyInAppPackage = false;
        int timeout = 120; // 300 = 5 min

        Writer writer = new CsvWriter();
        String outFile = "/home/pedro/tmp/teste.csv";
//		Writer writer = new JsonWriter();
//		String outFile = "/home/pedro/tmp/teste.json";

        Main main = new Main();
        try {
            main.execute(apk, methodsFile, androidPlatformsDir, rtJarPath, outFile, writer, checkOnlyInAppPackage, timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
