package br.unb.cic.reach.main;

import br.unb.cic.reach.android.SootConfig;
import br.unb.cic.reach.common.model.ConfigMatrix;
import br.unb.cic.reach.mop.Mop2Soot;
import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.IOException;
import java.nio.file.Path;

public class TesteMop2Soot {

    public void run(ConfigMatrix config) throws IOException {
        CallGraph callGraph = createCallGraph(config);
        Mop2Soot.toSootMethods(config.getTargetsFile(), callGraph, Path.of(config.getOutputFile()));
    }

    private CallGraph createCallGraph(ConfigMatrix config) {
        SetupApplication infoflow = SootConfig.initialize(config);

        System.out.println("Constructing call graph...");
        infoflow.constructCallgraph();

        return Scene.v().getCallGraph();
    }

    public static void main(String[] args) {
        String apkPath = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/cryptoapp.apk";
        String androidPlatformsDir = "/home/pedro/desenvolvimento/aplicativos/android/sdk/platforms";
        String rtJarPath = "/home/pedro/.sdkman/candidates/java/8.0.302-open/jre/lib/rt.jar";
        int timeoutSeconds = 300;
        String mopMethodsFilePath = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/mop_extractor.csv";
        String outputFile = "/home/pedro/desenvolvimento/workspaces/workspaces-doutorado/workspace-rv/reach-android/examples/mop_soot_sig.txt";

        try {
            ConfigMatrix config = new ConfigMatrix.Builder()
                    .withInputPath(apkPath)
                    .withAndroidPlatformsDir(androidPlatformsDir)
                    .withRtJarPath(rtJarPath)
                    .withTimeout(300)
                    .withTargetsFile(mopMethodsFilePath)
                    .withOutputFile(outputFile)
                    .build();

            new TesteMop2Soot().run(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
