#!/bin/sh

# Target methods file for JCA (Java Cryptography Architecture) analysis
MOP_JCA_TARGETS="mopMethods.txt"

# Test files
APK_FILE="cryptoapp.apk"
JAR_FILE="cryptolib.jar"

#echo "[+] Testing extract-only mode with APK..."
#java -jar ../reach-main/target/reach-analyzer.jar --extract-only -i $APK_FILE -o "extract_only.csv" -t $MOP_JCA_TARGETS
#
echo "[+] Testing full reachability analysis with APK..."
java -jar ../reach-main/target/reach-analyzer.jar -i $APK_FILE -o "full_analysis.csv" -t $MOP_JCA_TARGETS
#
#echo "[+] Testing analysis scope: all-methods vs reachable-only..."
#java -jar ../reach-main/target/reach-analyzer.jar -i $APK_FILE -o "all_methods.csv" -t $MOP_JCA_TARGETS --analysis-scope all-methods
#java -jar ../reach-main/target/reach-analyzer.jar -i $APK_FILE -o "reachable_only.csv" -t $MOP_JCA_TARGETS --analysis-scope reachable-only
#
#echo "[+] Testing JSON output format..."
#java -jar ../reach-main/target/reach-analyzer.jar -i $APK_FILE -o "output.json" -t $MOP_JCA_TARGETS --writer json

echo "[+] Testing JAR analysis (requires entry points)..."
# Note: JAR analysis requires entry points to be specified
# java -jar ../reach-main/target/reach-analyzer.jar -i $JAR_FILE -o "jar_analysis.csv" -t $MOP_JCA_TARGETS --entry-points entry_points.txt

echo "[+] All tests completed!"
