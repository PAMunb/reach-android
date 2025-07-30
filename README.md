# Reach Analysis Tool

A reachability analysis tool for Android applications and Java programs. This tool analyzes method reachability from entry points to target methods, supporting both Android APK files and Java JAR files through a ConfigMatrix-driven architecture.

## Overview

The Reach Analysis Tool performs static analysis to determine which methods in an application can be reached from specific entry points and which methods can reach target APIs. It provides detailed reachability information for security analysis, API usage detection, and program understanding.

### Key Features

- **Multi-format Support**: Analyzes Android APK files and Java JAR files
- **Component Analysis**: Supports all Android component types (Activities, Services, BroadcastReceivers, ContentProviders)
- **Entry Point Detection**: Automatic entry point detection or custom entry point specification
- **Multiple Output Formats**: Results in CSV or JSON format with filtering and statistics
- **Extensible Architecture**: Modular design supporting new application types through ConfigMatrix interface
- **Optimized Algorithms**: O(N+E) reachability algorithms with configurable analysis scope
- **Direct and Indirect Call Analysis**: Detects both immediate API calls and indirect calls through application methods

## Architecture

The tool follows a modular architecture with clear separation of concerns, unified by the ConfigMatrix configuration system:

```
reach-parent/
├── reach-core/          # Common models, O(N+E) algorithms, ConfigMatrix, and writers
├── reach-apk/           # APK-specific parsing and component extraction  
├── reach-android/       # Android analysis implementation
├── reach-jar/           # JAR analysis implementation
├── reach-mop/           # MOP compatibility layer for method signature conversion
└── reach-main/          # CLI interface and ConfigMatrix orchestration
```

### Core Components

- **ConfigMatrix**: Unified configuration interface driving all analysis decisions
- **ApplicationExtractor**: ConfigMatrix-aware interface for extracting information from different application types
- **ReachabilityAnalysis**: O(N+E) reachability computation with BFS algorithm
- **AnalysisScope**: Configurable analysis scope (all-methods vs reachable-only)
- **ComponentInfo Hierarchy**: Unified representation of application components
- **AppInfo**: Central container for application metadata and analysis results
- **Writers**: CSV/JSON output with filtering, statistics, and metadata

## Installation

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- Android SDK (for APK analysis)
  - Set `ANDROID_HOME` environment variable pointing to Android SDK root
  - Android platforms directory typically at `$ANDROID_HOME/platforms`
- Java Runtime JAR (rt.jar)
  - Usually auto-detected from `JAVA_HOME`
  - May need explicit path for some Java distributions
- At least 8GB RAM for large applications

### Building

```bash
git clone <repository-url>
cd reach-android
mvn clean compile package
```

The build produces an executable JAR in `reach-main/target/reach-analyzer.jar`.

## Usage

### Command Line Interface

```bash
java -jar reach-main/target/reach-analyzer.jar [OPTIONS]
```

### Required Parameters

- `--input, -i`: Input file (APK or JAR)
- `--output, -o`: Output file for results

### Android-Specific Parameters

- `--android-dir, -d`: Android platforms directory (default: `$ANDROID_HOME/platforms`)
- `--rt-jar, -r`: Runtime JAR path (default: auto-detected)

### Optional Parameters

- `--targets, -t`: Target methods file containing method signatures
- `--entry-points, -e`: Custom entry points file (for JARs without main methods)
- `--extract-only`: Extract information only, skip reachability analysis
- `--analysis-scope`: Analysis scope (all-methods, reachable-only) - default: all-methods
- `--writer, -w`: Output format (csv, json) - default: csv
- `--timeout`: Analysis timeout in seconds - default: 300
- `--app-package-only`: Analyze only application package classes - default: true
- `--entry-point-types`: Component types for entry points (all, activities, services, receivers, providers) - default: all
- `--debug`: Enable debug logging
- `--help, -h`: Show help information


## Examples by Configuration

### Structure Extraction (Fastest)
```bash
java -jar reach-analyzer.jar --extract-only -i app.apk -o structure.csv
```

### Direct Call Analysis (Fast)
```bash
java -jar reach-analyzer.jar --extract-only -i app.apk -t targets.txt -o direct-calls.csv
```

### Complete Reachability Analysis (Comprehensive)
```bash
java -jar reach-analyzer.jar -i app.apk -t targets.txt -o full-analysis.csv
```

### Scope Variations
```bash
# Only reachable methods
java -jar reach-analyzer.jar -i app.apk --analysis-scope reachable-only -o reachable-methods.csv

# All application classes (not just manifest package)
java -jar reach-analyzer.jar --extract-only -i app.apk --app-package-only=false -o all-classes.csv

# Specific component types only
java -jar reach-analyzer.jar -i app.apk --entry-point-types activities,services -o activities-services.csv
```

## Input File Formats

### Target Methods File

Text file with one method signature per line:

```
<javax.crypto.Cipher: void init(int,java.security.Key)>
<javax.crypto.Cipher: byte[] doFinal(byte[])>
<java.net.HttpURLConnection: void connect()>
```

### Entry Points File (for JARs)

Text file with one method signature per line:

```
<com.example.Library: void processData(java.lang.String)>
<com.example.Service: void handleRequest()>
```

## Output Formats

### CSV Format

```csv
class,method,signature,component_type,is_main,is_entry_point,reachable,reaches_targets,directly_reaches_targets,reachable_targets
com.app.MainActivity,onCreate,"<com.app.MainActivity: void onCreate(android.os.Bundle)>",activity,true,true,true,false,false,""
com.app.CryptoUtil,encrypt,"<com.app.CryptoUtil: String encrypt(String)>",application,false,false,true,true,true,"javax.crypto.Cipher.doFinal([B)"
```

### JSON Format

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
          "reachable_targets": []
        }
      ]
    }
  ]
}
```

## Configuration Options

### Entry Point Types

- **all**: All Android component types (default)
- **activities**: Only Activity components
- **services**: Only Service components  
- **receivers**: Only BroadcastReceiver components
- **providers**: Only ContentProvider components
- **combinations**: Comma-separated combinations (e.g., "activities,services")

### Analysis Scope

- **all-methods**: Analyze all methods in the application (default)
- **reachable-only**: Analyze only methods reachable from entry points

### Application Scope

- **app-package-only=true**: Analyze only classes in the application package (default)
- **app-package-only=false**: Include all non-system classes

### Output Formats

- **csv**: Comma-separated values format (default)
- **json**: JSON format with hierarchical structure

## Configuration Matrix

The tool behavior is determined by the ConfigMatrix, which combines multiple parameters to create different analysis modes. The main decision points are:

### Analysis Mode Decision Tree

```
├── extract-only + targets = Direct/Indirect Call Analysis (no call graph)
├── extract-only (no targets) = Structure Extraction Only  
├── full + targets = Complete Reachability Analysis (with call graph)
└── full (no targets) = Entry Point Reachability (with call graph)
```

### Configuration Combinations

| extract-only | targets | analysis-scope | Result |
|------------|---------|---------------|---------|
| ✓ | ✓ | any | Direct + indirect call detection without call graph |
| ✓ | ✗ | any | Structure extraction only (fastest) |
| ✗ | ✓ | all-methods | All methods + target reachability via call graph |
| ✗ | ✓ | reachable-only | Entry-reachable methods + target reachability |
| ✗ | ✗ | all-methods | All methods + entry point reachability |
| ✗ | ✗ | reachable-only | Entry-reachable methods only |

### Scope Modifiers

Additional parameters modify the method set:

- **app-package-only**: `true` = manifest package only, `false` = all application classes
- **entry-point-types**: Filter components (activities, services, receivers, providers, all)

### Output Columns by Configuration

- **reachable**: Always false in extract-only mode, computed in full mode
- **reaches_targets**: Only populated when targets provided
- **directly_reaches_targets**: Only populated in direct call analysis (extract-only + targets)

## Performance Considerations

### Memory Requirements

- Small apps (< 10MB): 2-4GB RAM
- Medium apps (10-50MB): 4-8GB RAM  
- Large apps (> 50MB): 8-16GB RAM

### Analysis Time

- Extract-only: Seconds to minutes
- Direct call analysis: Minutes
- Full reachability: Minutes to hours (depending on app size)

### Performance by Configuration

| Configuration | Speed | Memory | Use Case |
|--------------|-------|---------|----------|
| extract-only (no targets) | Fastest | Lowest | Structure analysis |
| extract-only + targets | Fast | Low | API usage detection |
| full + reachable-only | Medium | Medium | Entry point analysis |
| full + all-methods | Slowest | Highest | Complete analysis |

### Optimization Tips

- Use `--extract-only` for quick information gathering
- Use `--analysis-scope reachable-only` to reduce method set
- Use `--app-package-only=true` to focus analysis scope
- Use specific `--entry-point-types` to reduce entry points
- Increase `--timeout` for large applications

## Component Types

The tool recognizes and analyzes the following Android component types:

- **Activities**: User interface components with lifecycle methods
- **Services**: Background processing components  
- **BroadcastReceivers**: Event handling components
- **ContentProviders**: Data sharing components
- **Application Classes**: Regular application classes

For Java applications, classes are classified as:

- **Application Classes**: Classes loaded from the JAR
- **Library Classes**: Referenced library classes

## Error Handling

Common error scenarios and solutions:

### APK Analysis Errors

- **"Android platforms directory not found"**: Set `--android-dir` parameter or `ANDROID_HOME` environment variable
- **"Runtime JAR not found"**: Set `--rt-jar` parameter to valid rt.jar location
- **"APK file not readable"**: Check file permissions and validity

### JAR Analysis Errors

- **"No main() methods found"**: Use `--entry-points` parameter to specify entry points manually
- **"Target methods file required"**: Provide `--targets` parameter or use `--extract-only` mode

### General Errors

- **"OutOfMemoryError"**: Increase JVM heap size with `-Xmx` parameter
- **"Analysis timeout"**: Increase `--timeout` parameter value
- **"Invalid method signature"**: Check target methods file format

## Extending the Tool

The modular architecture supports adding new application types:

1. Implement the `ApplicationExtractor` interface with ConfigMatrix support
2. Add file type detection to `ExtractorFactory`
3. Register the new extractor in the CLI
4. Ensure compatibility with the ConfigMatrix configuration system

### ConfigMatrix Integration

New extractors must implement ConfigMatrix-aware initialization and support the unified configuration interface for consistent behavior across all application types.
