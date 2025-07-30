package br.unb.cic.reach.mop;

import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

record MopMethod(String className, String methodName) {
}

public class Mop2Soot {

    public static void toSootMethods(String mopMethodsFilePath, CallGraph cg, Path outputPath) throws IOException {
        Set<SootMethod> methods = toSootMethods(mopMethodsFilePath, cg);
        saveSootMethods(methods, outputPath);
    }

    private static Set<SootMethod> toSootMethods(String mopMethodsFilePath, CallGraph cg) throws IOException {
        Set<SootMethod> sootMethods = new HashSet<>();
        Set<MopMethod> mopMethods = readMOPMethods(mopMethodsFilePath);
        for (Edge edge : cg) {
            SootMethod sootMethod = edge.getSrc().method();
            if (isMOPMethod(mopMethods, sootMethod)) {
                sootMethods.add(sootMethod);
            }
        }
        return sootMethods;
    }

    private static boolean isMOPMethod(Set<MopMethod> mopMethods, SootMethod sootMethod) {
        String className = sootMethod.getDeclaringClass().getName();
        String methodName = sootMethod.getName();
        return mopMethods.stream()
                .anyMatch(mopMethod -> mopMethod.className().equals(className)
                        && mopMethod.methodName().equals(methodName));
    }

    private static Set<MopMethod> readMOPMethods(String path) throws IOException {
        Set<MopMethod> methods = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine(); // Skip header line

            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",", 2); // Split into max 2 parts
                    if (parts.length == 2) {
                        String className = parts[0].trim();
                        String methodName = parts[1].trim();
                        methods.add(new MopMethod(className, methodName));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading MOP methods file: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return methods;
    }

    private static void saveSootMethods(Set<SootMethod> methods, Path outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (SootMethod method : methods) {
                writer.write(method.getSignature());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving Soot methods to '" + outputPath + "': " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

}
