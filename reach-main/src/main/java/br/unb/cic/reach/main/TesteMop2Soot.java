package br.unb.cic.reach.main;

import br.unb.cic.reach.android.SootConfig;
import br.unb.cic.reach.mop.Mop2Soot;
import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.IOException;
import java.nio.file.Path;

public class TesteMop2Soot {

    public void run(String apkPath, String androidPlatformsDir, String rtJarPath, int timeoutSeconds, String mopMethodsFilePath, Path outputPath) throws IOException {
        CallGraph callGraph = createCallGraph(apkPath, androidPlatformsDir, rtJarPath, timeoutSeconds);
        Mop2Soot.toSootMethods(mopMethodsFilePath, callGraph, outputPath);
    }

    private CallGraph createCallGraph(String apkPath, String androidPlatformsDir, String rtJarPath, int timeoutSeconds) {
        SetupApplication infoflow = SootConfig.initialize(apkPath, androidPlatformsDir, rtJarPath, timeoutSeconds);

        System.out.println("Constructing call graph...");
        infoflow.constructCallgraph();

        return Scene.v().getCallGraph();
    }

    public static void main(String[] args) {
        String apkPath = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/cryptoapp.apk";
        String androidPlatformsDir = "/home/pedro/desenvolvimento/aplicativos/android/sdk/platforms";
        String rtJarPath = "/home/pedro/.sdkman/candidates/java/8.0.302-open/jre/lib/rt.jar";
        int timeoutSeconds = 300;
        String mopMethodsFilePath = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/mop_jca.csv";
        Path outputPath = Path.of("/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/mopMethods.txt");

        try {
            new TesteMop2Soot().run(apkPath, androidPlatformsDir, rtJarPath, timeoutSeconds, mopMethodsFilePath, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
