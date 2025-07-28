# Plano de Implementação - Análise de Reachability Extensível

## 1. Contexto e Objetivos

### 1.1 Visão Geral

O projeto implementa uma biblioteca extensível de análise de reachability para aplicações Android e JARs Java genéricos. A ferramenta analisa a alcançabilidade de métodos específicos (targets) a partir de entry points definidos automaticamente ou manualmente, fornecendo resultados em formatos CSV ou JSON.

### 1.2 Casos de Uso Principais

1. **Análise Android Completa**: Identificação de métodos alcançáveis desde componentes Android (Activities, Services, BroadcastReceivers, ContentProviders) até métodos target específicos
2. **Análise JAR Genérica**: Análise de reachability em aplicações Java a partir de métodos main() ou entry points customizados
3. **Extração Rápida**: Listagem de métodos com análise de chamadas diretas, sem construção de call graph

### 1.3 Arquitetura Extensível

A arquitetura suporta múltiplos tipos de aplicação através de implementações específicas de um conjunto comum de interfaces, permitindo adição futura de novos formatos (WAR, bibliotecas específicas) sem modificação do core algorítmico.

## 2. Arquitetura do Sistema

### 2.1 Estrutura Modular

```
reach-parent/
├── common/                 # Algoritmos e modelos genéricos
│   ├── model/             # AppInfo, ReachClass, ReachMethod
│   ├── analysis/          # ReachabilityAnalysis, interfaces
│   └── writer/            # Writers (CSV, JSON)
├── apk/                   # Leitura específica de APK
├── android/               # AndroidExtractor + componentes Android
├── jar/                   # JarExtractor (implementação futura)
└── main/                  # CLI + Main (fachada)
```

### 2.2 Hierarquia de Classes Principais

#### ApplicationExtractor Interface

```java
/**
 * Interface for extracting information and building analysis artifacts
 * from different application types (APK, JAR, etc.).
 *
 * Provides methods for both fast extraction scenarios and complete
 * reachability analysis scenarios with call graph construction.
 */
public interface ApplicationExtractor {
    
    /**
     * Initialize extractor with command line parameters.
     */
    void initialize(CommandLineArgs args);
    
    /**
     * Extract application information without target analysis.
     * Fast operation using basic Soot initialization.
     */
    AppInfo extractAppInfo();
    
    /**
     * Extract application information with direct call analysis.
     * Analyzes method bodies for direct invocations of target methods.
     */
    AppInfo extractAppInfo(Set<String> targetSignatures);
    
    /**
     * Build call graph for complete reachability analysis.
     * Heavy operation that initializes full Soot environment.
     */
    CallGraph buildCallGraph();
    
    /**
     * Extract entry points using application-specific logic.
     * Requires Soot to be initialized by buildCallGraph().
     */
    Set<EntryPoint> extractEntryPoints();
    
    /**
     * Resolve target method signatures to SootMethod objects.
     * Requires Soot to be initialized by buildCallGraph().
     */
    Set<SootMethod> resolveTargetMethods(Set<String> signatures);
}
```

#### AppInfo Genérico

```java
/**
 * Generic application information container.
 *
 * Represents application metadata and class structure for any
 * application type, with support for reachability analysis results.
 */
public class AppInfo {
    private String path;
    private String packageName;
    private String appName;
    private ApplicationType type;
    private Set<ReachClass> classes = new HashSet<>();
    
    public void addClass(ReachClass clazz) { ... }
    public Set<ReachClass> getClassesByComponentType(ComponentType type) { ... }
}

/**
 * Generic class representation with component type classification.
 */
public class ReachClass {
    private String className;
    private ComponentType componentType;
    private boolean isMainComponent;
    private Set<ReachMethod> methods = new HashSet<>();
}

/**
 * Method representation with reachability analysis results.
 */
public class ReachMethod {
    private String methodName;
    private String methodSignature;
    private boolean isEntryPoint;
    private boolean reachable;
    private boolean reachesTarget;
    private boolean directlyReachesTarget;
    private Set<String> reachableTargets = new HashSet<>();
    private List<Path> pathsToTargets = new ArrayList<>();
}
```

#### ComponentType Classification

```java
public enum ComponentType {
    // Android specific components
    ACTIVITY("activity"),
    SERVICE("service"), 
    RECEIVER("receiver"),
    PROVIDER("provider"),
    
    // Generic classifications based on Soot
    APPLICATION_CLASS("application"),  // Scene.v().getApplicationClasses()
    LIBRARY_CLASS("library"),          // Scene.v().getLibraryClasses()
    
    UNKNOWN("unknown");
}
```

### 2.3 Fluxo de Execução

#### CLI como Fachada

```java
public class Main {
    public static void main(String[] args) {
        CommandLineArgs cli = parseArgs(args);
        ApplicationExtractor extractor = ExtractorFactory.create(cli.getInputPath());
        extractor.initialize(cli);
        
        if (cli.isExtractOnly()) {
            // Fast extraction scenario
            AppInfo appInfo;
            if (cli.getTargetsFile() != null) {
                Set<String> targetSignatures = cli.getTargetMethods();
                appInfo = extractor.extractAppInfo(targetSignatures);
            } else {
                appInfo = extractor.extractAppInfo();
            }
            writeResults(new ReachabilityResult(appInfo), cli.getOutputFile());
            
        } else {
            // Complete reachability analysis scenario
            CallGraph callGraph = extractor.buildCallGraph();
            Set<EntryPoint> entryPoints = extractor.extractEntryPoints();
            Set<String> targetSignatures = cli.getTargetMethods();
            Set<SootMethod> targetMethods = extractor.resolveTargetMethods(targetSignatures);
            
            ReachabilityAnalysis analysis = new ReachabilityAnalysis();
            ReachabilityResult result = analysis.analyze(callGraph, entryPoints, targetMethods);
            
            writeResults(result, cli.getOutputFile());
        }
    }
}
```

## 3. Implementações Específicas

### 3.1 Android Extractor

```java
/**
 * Android-specific application extractor.
 *
 * Handles APK files using InfoflowAndroid for call graph construction
 * and ProcessManifest for component information extraction.
 */
public class AndroidExtractor implements ApplicationExtractor {
    
    private String apkPath;
    private String androidPlatformsDir;
    private String rtJarPath;
    private int timeout;
    
    @Override
    public AppInfo extractAppInfo(Set<String> targetSignatures) {
        // Basic Soot initialization
        G.reset();
        Options.v().set_process_dir(Collections.singletonList(apkPath));
        Options.v().set_android_jars(androidPlatformsDir);
        Options.v().set_src_prec(Options.src_prec_apk);
        Scene.v().loadNecessaryClasses();
        
        // Resolve targets if provided
        Set<SootMethod> targetMethods = null;
        if (targetSignatures != null && !targetSignatures.isEmpty()) {
            targetMethods = resolveTargetMethodsBasic(targetSignatures);
        }
        
        // Extract manifest information
        br.unb.cic.reach.apk.model.AppInfo apkInfo = AppReader.readApk(apkPath);
        
        // Build generic AppInfo with direct call analysis
        return buildGenericAppInfo(apkInfo, targetMethods);
    }
    
    @Override
    public CallGraph buildCallGraph() {
        // Full Soot + InfoflowAndroid initialization
        SetupApplication infoflow = SootConfig.initialize(
            apkPath, androidPlatformsDir, rtJarPath, timeout);
        infoflow.constructCallgraph();
        return Scene.v().getCallGraph();
    }
    
    @Override
    public Set<EntryPoint> extractEntryPoints() {
        br.unb.cic.reach.apk.model.AppInfo apkInfo = AppReader.readApk(apkPath);
        return EntryPointExtractor.extractEntryPoints(apkInfo.getAllComponents())
                .stream()
                .map(this::createEntryPoint)
                .collect(Collectors.toSet());
    }
    
    private void analyzeDirectCalls(ReachMethod reachMethod, 
                                   SootMethod sootMethod, 
                                   Set<SootMethod> targetMethods) {
        if (!sootMethod.hasActiveBody()) return;
        
        Body body = sootMethod.getActiveBody();
        for (Unit unit : body.getUnits()) {
            InvokeExpr invokeExpr = extractInvokeExpr(unit);
            if (invokeExpr != null) {
                SootMethod calledMethod = invokeExpr.getMethod();
                if (targetMethods.contains(calledMethod)) {
                    reachMethod.setDirectlyReachesTarget(true);
                    reachMethod.addReachableTarget(calledMethod.getSignature());
                    reachMethod.setReachesTarget(true);
                }
            }
        }
    }
}
```

### 3.2 JAR Extractor (Implementação Futura)

```java
/**
 * JAR-specific application extractor.
 *
 * Handles JAR files using standard Soot with SPARK call graph algorithm.
 * Automatically detects main() methods or requires explicit entry point specification.
 */
public class JarExtractor implements ApplicationExtractor {
    
    @Override
    public Set<EntryPoint> extractEntryPoints() {
        Set<SootMethod> mainMethods = findMainMethods();
        
        if (mainMethods.isEmpty()) {
            throw new EntryPointNotFoundException(
                "No main() methods found. Specify entry points using --entry-points parameter.");
        }
        
        return mainMethods.stream()
                .map(method -> new EntryPoint(method, ComponentType.APPLICATION_CLASS, true))
                .collect(Collectors.toSet());
    }
    
    @Override
    public CallGraph buildCallGraph() {
        G.reset();
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(jarPath));
        Options.v().set_src_prec(Options.src_prec_class);
        Options.v().setPhaseOption("cg.spark", "on");
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
        return Scene.v().getCallGraph();
    }
    
    private Set<SootMethod> findMainMethods() {
        Set<SootMethod> mainMethods = new HashSet<>();
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            try {
                SootMethod main = clazz.getMethodByName("main");
                if (isValidMainMethod(main)) {
                    mainMethods.add(main);
                }
            } catch (RuntimeException ignored) {
                // Method not found
            }
        }
        return mainMethods;
    }
}
```

## 4. Análise de Reachability

### 4.1 Algoritmo Genérico

```java
/**
 * Generic reachability analysis implementation.
 *
 * Performs reachability computation using configurable strategies
 * for graph traversal and path finding.
 */
public class ReachabilityAnalysis {
    
    public ReachabilityResult analyze(CallGraph callGraph, 
                                    Set<EntryPoint> entryPoints,
                                    Set<SootMethod> targetMethods) {
        
        // Initialize strategy
        ReachabilityStrategy strategy = new SootReachabilityStrategy();
        strategy.initialize(callGraph);
        
        // Compute reachability relationships
        Map<SootMethod, ReachabilityInfo> reachabilityMap = 
            computeReachability(entryPoints, targetMethods, strategy);
            
        // Build result with populated AppInfo
        AppInfo appInfo = buildAppInfoFromResults(reachabilityMap, entryPoints);
        
        return new ReachabilityResult(appInfo, reachabilityMap);
    }
    
    private Map<SootMethod, ReachabilityInfo> computeReachability(
            Set<EntryPoint> entryPoints,
            Set<SootMethod> targetMethods, 
            ReachabilityStrategy strategy) {
        
        Map<SootMethod, ReachabilityInfo> results = new HashMap<>();
        
        // Forward reachability from entry points
        Set<SootMethod> reachableFromEntry = computeForwardReachability(entryPoints, strategy);
        
        // Backward reachability to targets
        Set<SootMethod> reachingTargets = computeBackwardReachability(targetMethods, strategy);
        
        // Intersection and path computation
        for (SootClass clazz : getApplicationClasses()) {
            for (SootMethod method : clazz.getMethods()) {
                ReachabilityInfo info = new ReachabilityInfo();
                
                info.setReachable(reachableFromEntry.contains(method));
                info.setReachesTarget(reachingTargets.contains(method));
                
                if (info.isReachable() && info.isReachesTarget()) {
                    computePaths(method, entryPoints, targetMethods, strategy, info);
                }
                
                results.put(method, info);
            }
        }
        
        return results;
    }
}
```

### 4.2 Strategy Pattern

```java
/**
 * Strategy interface for reachability computation algorithms.
 */
public interface ReachabilityStrategy {
    
    void initialize(CallGraph callGraph);
    
    Optional<Path> findPath(SootMethod source, SootMethod target);
    
    boolean isSuccessor(SootMethod source, SootMethod target);
    
    Set<SootMethod> getReachableMethods(Set<SootMethod> sources);
}

/**
 * Soot-based reachability strategy using BFS traversal.
 */
public class SootReachabilityStrategy implements ReachabilityStrategy {
    
    private CallGraph callGraph;
    
    @Override
    public void initialize(CallGraph callGraph) {
        this.callGraph = callGraph;
    }
    
    @Override
    public Optional<Path> findPath(SootMethod source, SootMethod target) {
        List<SootMethod> path = computePath(source, target);
        return path.size() > 1 ? Optional.of(new Path(path)) : Optional.empty();
    }
    
    private List<SootMethod> computePath(SootMethod origin, SootMethod destination) {
        Queue<SootMethod> queue = new LinkedList<>();
        Set<SootMethod> visited = new HashSet<>();
        Map<SootMethod, SootMethod> parentMap = new HashMap<>();
        
        queue.add(origin);
        visited.add(origin);
        
        while (!queue.isEmpty()) {
            SootMethod current = queue.poll();
            if (current.equals(destination)) {
                return reconstructPath(current, parentMap);
            }
            
            Iterator<Edge> edges = callGraph.edgesOutOf(current);
            while (edges.hasNext()) {
                SootMethod target = edges.next().tgt();
                if (!visited.contains(target)) {
                    queue.add(target);
                    visited.add(target);
                    parentMap.put(target, current);
                }
            }
        }
        
        return new ArrayList<>();
    }
}
```

## 5. Interface de Linha de Comando

### 5.1 Parâmetros CLI

```java
public class CommandLineArgs {
    
    @Parameter(names = {"--input", "-i"}, 
               description = "Input file (APK or JAR)", required = true)
    private String inputPath;
    
    @Parameter(names = {"--targets", "-t"}, 
               description = "Target methods file")
    private String targetsFile;
    
    @Parameter(names = {"--entry-points", "-e"}, 
               description = "Custom entry points file (required for JAR without main())")
    private String entryPointsFile;
    
    @Parameter(names = {"--output", "-o"}, 
               description = "Output file", required = true)
    private String outputFile;
    
    @Parameter(names = {"--extract-only"}, 
               description = "Extract information only, skip reachability analysis")
    private boolean extractOnly = false;
    
    @Parameter(names = {"--writer", "-w"}, 
               description = "Output format: csv, json")
    private WriterType writerType = WriterType.CSV;
    
    // Android-specific parameters
    @Parameter(names = {"--android-dir", "-d"}, 
               description = "Android platforms directory")
    private String androidDir;
    
    @Parameter(names = {"--rt-jar", "-r"}, 
               description = "Runtime JAR path")
    private String rtJar;
    
    @Parameter(names = {"--timeout"}, 
               description = "Analysis timeout in seconds")
    private int timeout = 300;
    
    @Parameter(names = {"--app-package-only"}, 
               description = "Analyze only application package classes")
    private boolean appPackageOnly = true;
    
    @Parameter(names = {"--entry-point-types"}, 
               description = "Component types for entry points: all, activities, services, receivers, providers")
    private String entryPointTypes = "all";
}
```

### 5.2 Casos de Uso

```bash
# Extract methods only (fast)
./reach-analyzer --extract-only -i app.apk -o methods.csv

# Extract with direct call analysis (fast)
./reach-analyzer --extract-only -i app.apk -t targets.txt -o direct-calls.csv

# Complete reachability analysis (slow)
./reach-analyzer -i app.apk -t targets.txt -o full-analysis.csv

# Android with custom entry points
./reach-analyzer -i app.apk -t targets.txt -e custom-entries.txt -o results.csv

# JAR analysis with automatic main() detection
./reach-analyzer -i app.jar -t api-methods.txt -o results.csv

# JAR analysis with custom entry points
./reach-analyzer -i library.jar -t methods.txt -e entry-points.txt -o results.csv

# JSON output format
./reach-analyzer -i app.apk -t targets.txt --writer json -o results.json

# Specific component types only
./reach-analyzer -i app.apk -t targets.txt --entry-point-types activities,services -o results.csv
```

## 6. Formato de Saída

### 6.1 CSV Output

```csv
class,method,signature,component_type,is_main,is_entry_point,reachable,reaches_targets,directly_reaches_targets,reachable_targets
com.app.MainActivity,onCreate,"<com.app.MainActivity: void onCreate(android.os.Bundle)>",activity,true,true,true,false,false,""
com.app.CryptoUtil,encrypt,"<com.app.CryptoUtil: String encrypt(String)>",application,false,false,true,true,true,"javax.crypto.Cipher.doFinal([B)"
com.app.DeadCode,unused,"<com.app.DeadCode: void unused()>",application,false,false,false,false,false,""
```

### 6.2 JSON Output

```json
{
  "app_info": {
    "path": "/path/to/app.apk",
    "package": "com.example.app",
    "type": "ANDROID"
  },
  "results": [
    {
      "class": "com.app.MainActivity",
      "component_type": "activity",
      "is_main": true,
      "methods": [
        {
          "name": "onCreate",
          "signature": "<com.app.MainActivity: void onCreate(android.os.Bundle)>",
          "is_entry_point": true,
          "reachable": true,
          "reaches_targets": false,
          "directly_reaches_targets": false,
          "reachable_targets": [],
          "paths": []
        }
      ]
    }
  ]
}
```

### 6.3 Writers Implementation

```java
/**
 * CSV output writer for reachability analysis results.
 */
public class CsvWriter implements Writer {
    
    @Override
    public void write(ReachabilityResult result, File output) {
        AppInfo appInfo = result.getAppInfo();
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(output))) {
            writeHeader(pw);
            
            for (ReachClass clazz : appInfo.getClasses()) {
                for (ReachMethod method : clazz.getMethods()) {
                    writeMethodRow(pw, clazz, method);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV file", e);
        }
    }
    
    private void writeMethodRow(PrintWriter pw, ReachClass clazz, ReachMethod method) {
        String targets = String.join(";", method.getReachableTargets());
        pw.println(String.format("%s,%s,\"%s\",%s,%b,%b,%b,%b,%b,\"%s\"",
            clazz.getClassName(),
            method.getMethodName(),
            method.getMethodSignature(),
            clazz.getComponentType().getDisplayName(),
            clazz.isMainComponent(),
            method.isEntryPoint(),
            method.isReachable(),
            method.isReachesTarget(),
            method.isDirectlyReachesTarget(),
            targets
        ));
    }
}
```

## 7. Plano de Implementação

### 7.1 Etapa 1: Módulo Common e Interfaces (3-4 dias)

**Objetivos**:
- Criar estrutura modular Maven
- Implementar modelos genéricos (AppInfo, ReachClass, ReachMethod)
- Definir interfaces principais (ApplicationExtractor, ReachabilityStrategy)

**Atividades**:
1. Configurar estrutura Maven multi-módulo:
   ```xml
   <modules>
       <module>common</module>
       <module>apk</module>
       <module>android</module>
       <module>jar</module>
       <module>main</module>
   </modules>
   ```

2. Implementar classes do módulo common:
   ```
   common/src/main/java/br/unb/cic/reach/common/
   ├── model/
   │   ├── AppInfo.java
   │   ├── ReachClass.java
   │   ├── ReachMethod.java
   │   ├── EntryPoint.java
   │   └── ComponentType.java (enum)
   ├── extractor/
   │   ├── ApplicationExtractor.java (interface)
   │   └── ExtractorFactory.java
   ├── analysis/
   │   ├── ReachabilityAnalysis.java
   │   ├── ReachabilityStrategy.java (interface)
   │   ├── SootReachabilityStrategy.java
   │   └── ReachabilityResult.java
   └── writer/
       ├── Writer.java (interface)
       ├── CsvWriter.java
       ├── JsonWriter.java
       └── WriterFactory.java
   ```

3. Implementar factory para detecção automática de tipo:
   ```java
   public class ExtractorFactory {
       public static ApplicationExtractor create(String inputPath) {
           if (inputPath.toLowerCase().endsWith(".apk")) {
               return new AndroidExtractor();
           } else if (inputPath.toLowerCase().endsWith(".jar")) {
               return new JarExtractor();
           }
           throw new UnsupportedFormatException("Unsupported format: " + inputPath);
       }
   }
   ```

**Critérios de Aceitação**:
- Compilação Maven bem-sucedida
- Interfaces definidas com documentação completa
- Modelos genéricos implementados
- Factory funcionando para detecção de tipos

### 7.2 Etapa 2: Módulo APK Refatorado (2-3 dias)

**Objetivos**:
- Refatorar módulo APK existente para usar ComponentInfo genérico
- Manter compatibilidade com ProcessManifest
- Implementar extração de todos os tipos de componentes

**Atividades**:
1. Mover código atual para estrutura modular:
   ```
   apk/src/main/java/br/unb/cic/reach/apk/
   ├── model/
   │   ├── AppInfo.java (Android-específico, mantido para compatibilidade)
   │   ├── ActivityInfo.java
   │   ├── ServiceInfo.java
   │   ├── BroadcastReceiverInfo.java
   │   └── ContentProviderInfo.java
   ├── reader/
   │   └── AppReader.java
   └── util/
       ├── AndroidUtil.java
       ├── FileUtil.java
       └── StringUtil.java
   ```

2. Expandir AppReader para todos os componentes:
   ```java
   public class AppReader {
       public static AppInfo readApk(String apkPath) throws IOException, XmlPullParserException {
           ProcessManifest manifest = new ProcessManifest(targetAPK, resources);
           AppInfo appInfo = new AppInfo(apkPath);
           
           extractActivities(manifest, appInfo);
           extractServices(manifest, appInfo);
           extractReceivers(manifest, appInfo);
           extractProviders(manifest, appInfo);
           
           return appInfo;
       }
   }
   ```

3. Implementar extração de Services, BroadcastReceivers, ContentProviders

**Critérios de Aceitação**:
- Todos os tipos de componentes Android extraídos corretamente
- Compatibilidade mantida com código existente
- Testes passando com APKs do dataset

### 7.3 Etapa 3: AndroidExtractor (3-4 dias)

**Objetivos**:
- Implementar AndroidExtractor usando módulo APK
- Configuração Soot para Android (básica e completa)
- Entry point extraction para componentes Android

**Atividades**:
1. Implementar AndroidExtractor completo:
   ```java
   public class AndroidExtractor implements ApplicationExtractor {
       @Override
       public AppInfo extractAppInfo(Set<String> targetSignatures) {
           initializeBasicSoot();
           br.unb.cic.reach.apk.model.AppInfo apkInfo = AppReader.readApk(apkPath);
           Set<SootMethod> targets = resolveTargetMethodsBasic(targetSignatures);
           return buildGenericAppInfo(apkInfo, targets);
       }
       
       @Override
       public CallGraph buildCallGraph() {
           SetupApplication infoflow = SootConfig.initialize(apkPath, androidPlatformsDir, rtJarPath, timeout);
           infoflow.constructCallgraph();
           return Scene.v().getCallGraph();
       }
   }
   ```

2. Implementar conversão de APK-específico para genérico:
   ```java
   private AppInfo buildGenericAppInfo(br.unb.cic.reach.apk.model.AppInfo apkInfo, 
                                      Set<SootMethod> targetMethods) {
       AppInfo generic = new AppInfo();
       // Conversão de estruturas
       // Análise de chamadas diretas se targetMethods != null
       return generic;
   }
   ```

3. Implementar EntryPointExtractor para Android:
   ```java
   public class AndroidEntryPointExtractor {
       public static Set<EntryPoint> extractEntryPoints(Set<ComponentInfo> components) {
           Set<EntryPoint> entryPoints = new HashSet<>();
           
           for (ComponentInfo component : components) {
               // Lifecycle methods + public/protected methods
               entryPoints.addAll(getLifecycleMethods(component));
               entryPoints.addAll(getPublicProtectedMethods(component));
           }
           
           return entryPoints;
       }
   }
   ```

**Critérios de Aceitação**:
- AndroidExtractor funcional para ambos cenários (extract-only e full analysis)
- Entry points corretos para todos os tipos de componentes
- Análise de chamadas diretas funcionando
- Performance aceitável para extract-only

### 7.4 Etapa 4: ReachabilityAnalysis Genérico (3-4 dias)

**Objetivos**:
- Implementar algoritmo genérico de reachability
- Strategy pattern para diferentes abordagens
- Otimização de travessia do call graph

**Atividades**:
1. Implementar ReachabilityAnalysis:
   ```java
   public class ReachabilityAnalysis {
       public ReachabilityResult analyze(CallGraph callGraph, 
                                       Set<EntryPoint> entryPoints,
                                       Set<SootMethod> targetMethods) {
           
           ReachabilityStrategy strategy = new SootReachabilityStrategy();
           strategy.initialize(callGraph);
           
           Map<SootMethod, ReachabilityInfo> results = computeReachability(
               entryPoints, targetMethods, strategy);
               
           AppInfo appInfo = buildAppInfoFromResults(results, entryPoints);
           return new ReachabilityResult(appInfo, results);
       }
   }
   ```

2. Implementar SootReachabilityStrategy otimizado:
   ```java
   public class SootReachabilityStrategy implements ReachabilityStrategy {
       @Override
       public Set<SootMethod> getReachableMethods(Set<SootMethod> sources) {
           // Forward BFS from all sources simultaneously
           Set<SootMethod> reachable = new HashSet<>();
           Queue<SootMethod> queue = new LinkedList<>(sources);
           reachable.addAll(sources);
           
           while (!queue.isEmpty()) {
               SootMethod current = queue.poll();
               Iterator<Edge> edges = callGraph.edgesOutOf(current);
               
               while (edges.hasNext()) {
                   SootMethod target = edges.next().tgt();
                   if (reachable.add(target)) {
                       queue.add(target);
                   }
               }
           }
           
           return reachable;
       }
   }
   ```

3. Implementar algoritmo otimizado com menos travessias:
    - Forward reachability de todos os entry points
    - Backward reachability para todos os targets
    - Interseção para identificar métodos relevantes
    - Path reconstruction on-demand

**Critérios de Aceitação**:
- Algoritmo genérico funcionando para Android
- Performance melhorada em relação à implementação anterior
- Strategy pattern permitindo diferentes abordagens
- Resultados corretos comparados com implementação original

### 7.5 Etapa 5: CLI e Main (2-3 dias)

**Objetivos**:
- Implementar CLI completa com detecção automática
- Main como fachada orquestrando diferentes cenários
- Validação de parâmetros específicos por tipo

**Atividades**:
1. Implementar CommandLineArgs completo:
   ```java
   public class CommandLineArgs {
       // Validação condicional
       public void validate() {
           if (inputPath.toLowerCase().endsWith(".apk")) {
               if (androidDir == null) {
                   throw new ParameterException("--android-dir required for APK analysis");
               }
               if (rtJar == null) {
                   throw new ParameterException("--rt-jar required for APK analysis");
               }
           }
           
           if (inputPath.toLowerCase().endsWith(".jar")) {
               if (!extractOnly && targetsFile == null) {
                   throw new ParameterException("--targets required for JAR reachability analysis");
               }
           }
       }
   }
   ```

2. Implementar Main como fachada:
   ```java
   public class Main {
       public static void main(String[] args) {
           try {
               CommandLineArgs cli = parseAndValidateArgs(args);
               ApplicationExtractor extractor = ExtractorFactory.create(cli.getInputPath());
               extractor.initialize(cli);
               
               ReachabilityResult result = executeAnalysis(cli, extractor);
               writeResults(result, cli);
               
           } catch (Exception e) {
               handleError(e);
               System.exit(1);
           }
       }
       
       private static ReachabilityResult executeAnalysis(CommandLineArgs cli, 
                                                       ApplicationExtractor extractor) {
           if (cli.isExtractOnly()) {
               return executeExtractOnly(cli, extractor);
           } else {
               return executeFullAnalysis(cli, extractor);
           }
       }
   }
   ```

3. Implementar validação e mensagens de erro específicas:
   ```java
   private static void handleError(Exception e) {
       if (e instanceof EntryPointNotFoundException) {
           System.err.println("Entry point error: " + e.getMessage());
           System.err.println("For JAR files, use --entry-points to specify entry points manually");
       } else if (e instanceof UnsupportedFormatException) {
           System.err.println("Format error: " + e.getMessage());
           System.err.println("Supported formats: .apk, .jar");
       } else {
           System.err.println("Analysis error: " + e.getMessage());
           if (DEBUG_MODE) {
               e.printStackTrace();
           }
       }
   }
   ```

**Critérios de Aceitação**:
- CLI funcional para todos os cenários
- Detecção automática de tipo funcionando
- Validação apropriada de parâmetros
- Mensagens de erro claras e específicas

### 7.6 Etapa 6: Writers e Output (1-2 dias)

**Objetivos**:
- Implementar writers para CSV e JSON
- Suporte a diferentes níveis de detalhamento
- Validação de formato de saída

**Atividades**:
1. Implementar CsvWriter otimizado:
   ```java
   public class CsvWriter implements Writer {
       @Override
       public void write(ReachabilityResult result, File output) {
           AppInfo appInfo = result.getAppInfo();
           
           try (PrintWriter pw = new PrintWriter(new FileWriter(output))) {
               writeHeader(pw);
               
               for (ReachClass clazz : appInfo.getClasses()) {
                   for (ReachMethod method : clazz.getMethods()) {
                       writeMethodRow(pw, clazz, method);
                   }
               }
               
               log.info("CSV written: {} classes, {} methods", 
                       appInfo.getClasses().size(), 
                       appInfo.getClasses().stream()
                               .mapToInt(c -> c.getMethods().size())
                               .sum());
           }
       }
   }
   ```

2. Implementar JsonWriter com estrutura hierárquica:
   ```java
   public class JsonWriter implements Writer {
       @Override
       public void write(ReachabilityResult result, File output) {
           Gson gson = new GsonBuilder()
                   .setPrettyPrinting()
                   .create();
                   
           JsonOutput jsonOutput = new JsonOutput(result);
           
           try (FileWriter writer = new FileWriter(output)) {
               gson.toJson(jsonOutput, writer);
           }
       }
   }
   ```

3. Implementar WriterFactory com configurações:
   ```java
   public class WriterFactory {
       public static Writer create(WriterType type, boolean includeSystemClasses) {
           switch (type) {
               case CSV:
                   return new CsvWriter(includeSystemClasses);
               case JSON:
                   return new JsonWriter(includeSystemClasses);
               default:
                   throw new IllegalArgumentException("Unsupported writer type: " + type);
           }
       }
   }
   ```

**Critérios de Aceitação**:
- CSV e JSON gerados corretamente
- Formato compatível com ferramentas de análise
- Performance adequada para arquivos grandes
- Encoding correto (UTF-8)

### 7.7 Etapa 7: JAR Extractor Básico (2-3 dias)

**Objetivos**:
- Implementar JarExtractor básico para validar arquitetura
- Detecção automática de métodos main()
- Configuração Soot para JARs

**Atividades**:
1. Implementar JarExtractor:
   ```java
   public class JarExtractor implements ApplicationExtractor {
       @Override
       public AppInfo extractAppInfo(Set<String> targetSignatures) {
           initializeBasicSoot();
           
           AppInfo appInfo = new AppInfo();
           appInfo.setPath(jarPath);
           appInfo.setType(ApplicationType.JAR);
           
           // Detectar package principal
           String mainPackage = detectMainPackage();
           appInfo.setPackageName(mainPackage);
           
           // Processar classes
           for (SootClass clazz : Scene.v().getApplicationClasses()) {
               ReachClass reachClass = createReachClass(clazz);
               if (targetSignatures != null) {
                   analyzeDirectCallsInClass(reachClass, targetSignatures);
               }
               appInfo.addClass(reachClass);
           }
           
           return appInfo;
       }
       
       @Override
       public CallGraph buildCallGraph() {
           G.reset();
           Options.v().set_whole_program(true);
           Options.v().set_process_dir(Collections.singletonList(jarPath));
           Options.v().set_src_prec(Options.src_prec_class);
           Options.v().setPhaseOption("cg.spark", "on");
           Options.v().setPhaseOption("cg.spark", "verbose:false");
           
           Scene.v().loadNecessaryClasses();
           PackManager.v().runPacks();
           
           return Scene.v().getCallGraph();
       }
       
       @Override
       public Set<EntryPoint> extractEntryPoints() {
           Set<SootMethod> mainMethods = findMainMethods();
           
           if (mainMethods.isEmpty()) {
               throw new EntryPointNotFoundException(
                   "No main() methods found in JAR. Use --entry-points to specify entry points manually.");
           }
           
           return mainMethods.stream()
                   .map(method -> new EntryPoint(method, ComponentType.APPLICATION_CLASS, true))
                   .collect(Collectors.toSet());
       }
   }
   ```

2. Implementar detecção de package principal:
   ```java
   private String detectMainPackage() {
       Map<String, Integer> packageCounts = new HashMap<>();
       
       for (SootClass clazz : Scene.v().getApplicationClasses()) {
           String packageName = clazz.getPackageName();
           if (!packageName.isEmpty()) {
               packageCounts.merge(packageName, 1, Integer::sum);
           }
       }
       
       return packageCounts.entrySet().stream()
               .max(Map.Entry.comparingByValue())
               .map(Map.Entry::getKey)
               .orElse("default");
   }
   ```

3. Implementar validação de main methods:
   ```java
   private boolean isValidMainMethod(SootMethod method) {
       return method.isStatic() 
               && method.isPublic() 
               && method.getParameterCount() == 1
               && method.getParameterType(0).toString().equals("java.lang.String[]")
               && method.getReturnType().toString().equals("void");
   }
   ```

**Critérios de Aceitação**:
- JarExtractor funcional para JARs básicos
- Detecção automática de main() funcionando
- Extract-only funcionando para JARs
- Integração com CLI funcionando

### 7.8 Etapa 8: Testes e Validação (3-4 dias)

**Objetivos**:
- Validar funcionalidade completa com dataset existente
- Comparar resultados com implementação anterior
- Testes de performance e correção

**Atividades**:
1. Criar suite de testes automatizados:
   ```java
   @Test
   public class AndroidExtractorTest {
       @Test
       public void testExtractOnlyMode() {
           AndroidExtractor extractor = new AndroidExtractor();
           CommandLineArgs args = createTestArgs("test.apk", true);
           extractor.initialize(args);
           
           AppInfo appInfo = extractor.extractAppInfo();
           
           assertNotNull(appInfo);
           assertTrue(appInfo.getClasses().size() > 0);
           // Verificar estrutura esperada
       }
       
       @Test
       public void testFullAnalysisMode() {
           AndroidExtractor extractor = new AndroidExtractor();
           CommandLineArgs args = createTestArgs("test.apk", false);
           extractor.initialize(args);
           
           CallGraph callGraph = extractor.buildCallGraph();
           Set<EntryPoint> entryPoints = extractor.extractEntryPoints();
           
           assertNotNull(callGraph);
           assertTrue(entryPoints.size() > 0);
           // Verificar resultados esperados
       }
   }
   ```

2. Executar validação com dataset de APKs:
   ```bash
   #!/bin/bash
   # validation_script.sh
   
   APK_DIR="/path/to/test/apks"
   TARGETS_FILE="/path/to/targets.txt"
   
   for apk in "$APK_DIR"/*.apk; do
       echo "Testing: $apk"
       
       # Extract-only test
       ./reach-analyzer --extract-only -i "$apk" -o "results/extract_$(basename "$apk").csv"
       
       # Full analysis test
       ./reach-analyzer -i "$apk" -t "$TARGETS_FILE" -o "results/full_$(basename "$apk").csv"
       
       # Validate CSV format
       python validate_csv.py "results/full_$(basename "$apk").csv"
   done
   ```

3. Implementar comparação com implementação anterior:
   ```java
   public class ResultComparator {
       public ComparisonResult compare(File oldResults, File newResults) {
           List<ReachMethod> oldMethods = parseOldFormat(oldResults);
           List<ReachMethod> newMethods = parseNewFormat(newResults);
           
           ComparisonResult result = new ComparisonResult();
           result.setTotalMethods(newMethods.size());
           result.setReachableMethodsOld(countReachable(oldMethods));
           result.setReachableMethodsNew(countReachable(newMethods));
           result.setDifferences(findDifferences(oldMethods, newMethods));
           
           return result;
       }
   }
   ```

4. Implementar métricas de performance:
   ```java
   public class PerformanceMonitor {
       public void logAnalysisMetrics(String apkPath, ReachabilityResult result) {
           long analysisTime = result.getExecutionTime();
           int entryPointCount = result.getEntryPointCount();
           int reachableMethodCount = result.getReachableMethodCount();
           int csvLines = result.getCsvLineCount();
           
           log.info("Performance metrics for {}: {}ms, {} entry points, {} reachable methods, {} CSV lines",
                   apkPath, analysisTime, entryPointCount, reachableMethodCount, csvLines);
       }
   }
   ```

**Critérios de Aceitação**:
- Todos os testes automatizados passando
- Validação com dataset real bem-sucedida
- Performance dentro dos limites esperados
- Compatibilidade de resultados validada

### 7.9 Etapa 9: Documentação e Guias (2 dias)

**Objetivos**:
- Documentação completa de uso
- Guias para diferentes cenários
- Documentação técnica da API

**Atividades**:
1. Criar README.md principal:
   ```markdown
   # Reach Analysis Tool
   
   ## Overview
   Extensible reachability analysis tool for Android APKs and Java JARs.
   
   ## Quick Start
   
   ### Android APK Analysis
   ```bash
   # Extract methods only
   ./reach-analyzer --extract-only -i app.apk -o methods.csv
   
   # Full reachability analysis
   ./reach-analyzer -i app.apk -t targets.txt -d $ANDROID_HOME/platforms -r $JAVA_HOME/jre/lib/rt.jar -o results.csv
   ```

   ### Java JAR Analysis
   ```bash
   # Automatic main() detection
   ./reach-analyzer -i app.jar -t targets.txt -o results.csv
   
   # Custom entry points
   ./reach-analyzer -i library.jar -t targets.txt -e entry-points.txt -o results.csv
   ```
   ```

2. Criar guias específicos:
   ```
   docs/
   ├── installation.md        # Instalação e configuração
   ├── android-analysis.md    # Análise de APKs Android
   ├── jar-analysis.md        # Análise de JARs Java
   ├── output-formats.md      # Formatos de saída
   ├── configuration.md       # Opções de configuração
   ├── troubleshooting.md     # Solução de problemas
   └── api-reference.md       # Referência da API
   ```

3. Documentar casos de uso específicos:
   ```markdown
   ## Android Analysis Scenarios
   
   ### Scenario 1: Security API Usage Analysis
   Analyze which Android components can reach cryptographic APIs.
   
   ### Scenario 2: Custom Entry Points
   Use specific activity methods as entry points instead of all lifecycle methods.
   
   ### Scenario 3: Package Mismatch Handling
   Include classes outside the manifest package for comprehensive analysis.
   ```

4. Criar documentação técnica:
   ```markdown
   ## Architecture Overview
   
   ### Module Structure
   - `common/`: Generic models and algorithms
   - `android/`: Android-specific implementations
   - `jar/`: JAR-specific implementations
   - `main/`: CLI and application entry point
   
   ### Extension Points
   To add support for new application types:
   1. Implement `ApplicationExtractor` interface
   2. Add detection logic to `ExtractorFactory`
   3. Register new file extensions in CLI
   ```

**Critérios de Aceitação**:
- README.md completo e claro
- Guias de uso para todos os cenários
- Documentação técnica para desenvolvedores
- Exemplos funcionais testados

### 7.10 Etapa 10: Otimização e Refinamento (2-3 dias)

**Objetivos**:
- Otimizações de performance identificadas durante testes
- Refinamentos na interface CLI
- Correções de bugs encontrados

**Atividades**:
1. Otimizar algoritmo de reachability baseado em profiling:
   ```java
   public class OptimizedReachabilityAnalysis {
       public ReachabilityResult analyze(CallGraph callGraph, 
                                       Set<EntryPoint> entryPoints,
                                       Set<SootMethod> targetMethods) {
           
           // Cache for repeated path queries
           Map<Pair<SootMethod, SootMethod>, Optional<Path>> pathCache = new HashMap<>();
           
           // Precompute reachable sets
           Set<SootMethod> reachableFromEntry = precomputeReachableSet(entryPoints, callGraph);
           Set<SootMethod> reachingTargets = precomputeReachingSet(targetMethods, callGraph);
           
           // Process only intersection
           Set<SootMethod> relevantMethods = Sets.intersection(reachableFromEntry, reachingTargets);
           
           return buildResults(relevantMethods, pathCache);
       }
   }
   ```

2. Implementar configurações avançadas:
   ```java
   public class AdvancedConfig {
       private int maxPathLength = 50;
       private boolean enablePathCompression = true;
       private boolean skipSystemClasses = true;
       private Set<String> excludePackages = new HashSet<>();
       
       public static AdvancedConfig fromCLI(CommandLineArgs args) {
           // Parse advanced configuration from CLI or config file
       }
   }
   ```

3. Adicionar métricas detalhadas:
   ```java
   public class DetailedMetrics {
       private long sootInitTime;
       private long callGraphTime;
       private long reachabilityTime;
       private long outputTime;
       private int totalMethods;
       private int reachableMethods;
       private int targetReachingMethods;
       
       public void report() {
           log.info("=== Analysis Metrics ===");
           log.info("Soot initialization: {}ms", sootInitTime);
           log.info("Call graph construction: {}ms", callGraphTime);
           log.info("Reachability analysis: {}ms", reachabilityTime);
           log.info("Output generation: {}ms", outputTime);
           log.info("Total methods: {}", totalMethods);
           log.info("Reachable methods: {} ({:.1f}%)", 
                   reachableMethods, 
                   100.0 * reachableMethods / totalMethods);
       }
   }
   ```

4. Implementar validação robusta de entrada:
   ```java
   public class InputValidator {
       public void validateAPK(String apkPath) throws ValidationException {
           File apkFile = new File(apkPath);
           if (!apkFile.exists()) {
               throw new ValidationException("APK file not found: " + apkPath);
           }
           if (!apkFile.canRead()) {
               throw new ValidationException("APK file not readable: " + apkPath);
           }
           // Validate APK format
           try (ZipFile zip = new ZipFile(apkFile)) {
               if (zip.getEntry("AndroidManifest.xml") == null) {
                   throw new ValidationException("Invalid APK: missing AndroidManifest.xml");
               }
           }
       }
       
       public void validateTargetsFile(String targetsPath) throws ValidationException {
           List<String> signatures = readMethodsFile(targetsPath);
           for (String signature : signatures) {
               if (!isValidSignature(signature)) {
                   throw new ValidationException("Invalid method signature: " + signature);
               }
           }
       }
   }
   ```

**Critérios de Aceitação**:
- Performance otimizada baseada em profiling
- Validação robusta de entrada implementada
- Métricas detalhadas disponíveis
- Bugs identificados corrigidos

## 8. Cronograma e Recursos

### 8.1 Estimativa de Tempo

| Etapa | Atividade | Duração | Dependências |
|-------|-----------|---------|--------------|
| 1 | Módulo Common e Interfaces | 3-4 dias | - |
| 2 | Módulo APK Refatorado | 2-3 dias | Etapa 1 |
| 3 | AndroidExtractor | 3-4 dias | Etapas 1,2 |
| 4 | ReachabilityAnalysis Genérico | 3-4 dias | Etapa 1 |
| 5 | CLI e Main | 2-3 dias | Etapas 1,3,4 |
| 6 | Writers e Output | 1-2 dias | Etapa 1 |
| 7 | JAR Extractor Básico | 2-3 dias | Etapas 1,4 |
| 8 | Testes e Validação | 3-4 dias | Todas anteriores |
| 9 | Documentação e Guias | 2 dias | Todas anteriores |
| 10 | Otimização e Refinamento | 2-3 dias | Etapa 8 |

**Total: 23-32 dias úteis**

### 8.2 Recursos Necessários

- **Desenvolvedor Principal**: Implementação core e arquitetura
- **Dataset de APKs**: Para validação e testes
- **Ambiente de Desenvolvimento**:
    - Java 21
    - Maven 3.8+
    - Android SDK (platforms directory)
    - Ferramentas de profiling (JProfiler/VisualVM)

### 8.3 Critérios de Sucesso

1. **Funcionalidade**: Análise funcionando para Android e JAR
2. **Performance**: Tempo de análise comparável ou melhor que implementação anterior
3. **Usabilidade**: CLI intuitiva com documentação clara
4. **Extensibilidade**: Arquitetura suporta novos tipos facilmente
5. **Qualidade**: Cobertura de testes > 80%, documentação completa

## 9. Considerações Técnicas

### 9.1 Dependências Maven

```xml
<!-- common/pom.xml -->
<dependencies>
    <!-- Soot Core -->
    <dependency>
        <groupId>org.soot-oss</groupId>
        <artifactId>soot</artifactId>
        <version>4.4.1</version>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
    
    <!-- Graph Algorithms -->
    <dependency>
        <groupId>org.jgrapht</groupId>
        <artifactId>jgrapht-core</artifactId>
        <version>1.5.2</version>
    </dependency>
    
    <!-- CLI Processing -->
    <dependency>
        <groupId>com.beust</groupId>
        <artifactId>jcommander</artifactId>
        <version>1.82</version>
    </dependency>
</dependencies>

<!-- android/pom.xml -->
<dependencies>
    <dependency>
        <groupId>br.unb.cic.reach</groupId>
        <artifactId>common</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <dependency>
        <groupId>br.unb.cic.reach</groupId>
        <artifactId>apk</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- FlowDroid -->
    <dependency>
        <groupId>de.fraunhofer.sit.sse.flowdroid</groupId>
        <artifactId>soot-infoflow-android</artifactId>
        <version>2.10.0</version>
    </dependency>
</dependencies>
```

### 9.2 Configurações de Performance

```java
public class PerformanceConfig {
    // Memory settings
    public static final double MEMORY_THRESHOLD = 0.9;
    public static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    
    // Analysis settings
    public static final int DEFAULT_TIMEOUT = 300; // seconds
    public static final int MAX_PATH_LENGTH = 100;
    public static final int MAX_METHODS_PER_CLASS = 1000;
    
    // Soot settings
    public static void configureSootForPerformance() {
        Options.v().set_verbose(false);
        Options.v().set_debug(false);
        Options.v().set_validate(false);
        Options.v().setPhaseOption("jb", "use-original-names:false");
    }
}
```

### 9.3 Tratamento de Erros

```java
public class ErrorHandler {
    public static void handleSootError(Exception e) {
        if (e.getMessage().contains("OutOfMemoryError")) {
            log.error("Analysis failed due to memory constraints. Try increasing heap size with -Xmx");
        } else if (e.getMessage().contains("TimeoutException")) {
            log.error("Analysis timed out. Try increasing timeout with --timeout parameter");
        } else {
            log.error("Soot analysis failed: {}", e.getMessage());
        }
    }
    
    public static void handleManifestError(Exception e) {
        log.error("Failed to parse Android manifest: {}", e.getMessage());
        log.error("Ensure the APK file is valid and not corrupted");
    }
}
```

## 10. Casos de Uso Validados

### 10.1 Análise Android

```bash
# Análise básica de segurança
./reach-analyzer -i banking-app.apk \
                 -t crypto-apis.txt \
                 -d $ANDROID_HOME/platforms \
                 -r $JAVA_HOME/jre/lib/rt.jar \
                 -o crypto-analysis.csv

# Análise com componentes específicos
./reach-analyzer -i social-app.apk \
                 -t network-apis.txt \
                 --entry-point-types activities,services \
                 -o network-usage.csv

# Extração rápida com análise direta
./reach-analyzer --extract-only \
                 -i utility-app.apk \
                 -t permission-apis.txt \
                 -o direct-calls.csv
```

### 10.2 Análise JAR

```bash
# Aplicação com main() automático
./reach-analyzer -i desktop-app.jar \
                 -t security-apis.txt \
                 -o security-analysis.csv

# Biblioteca sem main()
./reach-analyzer -i crypto-library.jar \
                 -t target-methods.txt \
                 -e library-entry-points.txt \
                 -o library-analysis.csv

# Análise de framework
./reach-analyzer -i web-framework.jar \
                 -t servlet-apis.txt \
                 -e controller-methods.txt \
                 --writer json \
                 -o framework-analysis.json
```

### 10.3 Análise Comparativa

```bash
# Análise de múltiplas versões
for version in v1.0 v2.0 v3.0; do
    ./reach-analyzer -i "app-${version}.apk" \
                     -t apis.txt \
                     -o "analysis-${version}.csv"
done

# Comparação de resultados
python compare-versions.py analysis-v*.csv
```

## 11. Extensões Futuras

### 11.1 Suporte a Novos Formatos

A arquitetura suporta adição de novos extractors:

```java
// Exemplo: WAR files
public class WarExtractor implements ApplicationExtractor {
    @Override
    public Set<EntryPoint> extractEntryPoints() {
        // Detectar servlets, filters, listeners
        return extractServletEntryPoints();
    }
    
    @Override
    public CallGraph buildCallGraph() {
        // Configuração Soot para web applications
        return buildWebApplicationCallGraph();
    }
}

// Registro no factory
public class ExtractorFactory {
    public static ApplicationExtractor create(String inputPath) {
        if (inputPath.endsWith(".war")) {
            return new WarExtractor();
        }
        // ... outros tipos
    }
}
```

### 11.2 Análise Incremental

```java
public class IncrementalAnalysis {
    public ReachabilityResult analyzeIncremental(
            String currentApk, 
            String previousApk,
            ReachabilityResult previousResult) {
        
        // Detectar mudanças
        ChangeSet changes = detectChanges(currentApk, previousApk);
        
        // Reutilizar resultados anteriores
        return updateAnalysis(previousResult, changes);
    }
}
```

### 11.3 Análise Distribuída

```java
public class DistributedAnalysis {
    public ReachabilityResult analyzeDistributed(
            List<String> apkPaths,
            String targetMethods) {
        
        return apkPaths.parallelStream()
                .map(apk -> analyzeSingle(apk, targetMethods))
                .collect(new ReachabilityResultCollector());
    }
}
```

---

*Este plano de implementação define uma arquitetura extensível e robusta para análise de reachability, com foco inicial em Android APKs e preparação para expansão futura para outros formatos de aplicação.*