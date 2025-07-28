package br.unb.cic.reach.common.writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.model.ReachClass;
import br.unb.cic.reach.common.model.ReachMethod;

/**
 * CSV format writer for reachability analysis results.
 *
 * This writer outputs reachability analysis results in CSV format, providing
 * structured data suitable for spreadsheet analysis, database import, and
 * automated processing workflows.
 *
 * ### Architectural Decisions:
 * - Comma-separated format for wide tool compatibility
 * - Quoted fields for handling special characters safely
 * - Comprehensive column set for complete analysis information
 * - Flattened structure for efficient processing and filtering
 *
 * ### Role in the System:
 * - Primary output format for reachability analysis results
 * - Interface for spreadsheet and database analysis workflows
 * - Foundation for automated result processing and reporting
 */
public class CsvWriter implements Writer {
    private static final Logger log = LoggerFactory.getLogger(CsvWriter.class);
    
    @Override
    public void write(ReachabilityResult result, File outputFile) {
        log.info("Writing CSV results to: {}", outputFile.getAbsolutePath());
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile))) {
            writeHeader(pw);
            writeRows(result, pw);
            
            log.info("CSV output completed: {} classes processed", 
                    result.getAppInfo().getClasses().size());
        } catch (IOException e) {
            log.error("Error writing CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to write CSV output", e);
        }
    }
    
    /**
     * Writes CSV header with column definitions.
     */
    private void writeHeader(PrintWriter pw) {
        pw.println("class,method,signature,component_type,is_main,is_entry_point," +
                  "reachable,reaches_targets,directly_reaches_targets,reachable_targets");
    }
    
    /**
     * Writes data rows for all classes and methods.
     */
    private void writeRows(ReachabilityResult result, PrintWriter pw) {
        for (ReachClass clazz : result.getAppInfo().getClasses()) {
            for (ReachMethod method : clazz.getMethods()) {
                writeMethodRow(pw, clazz, method);
            }
        }
    }
    
    /**
     * Writes a single method row with proper CSV formatting.
     */
    private void writeMethodRow(PrintWriter pw, ReachClass clazz, ReachMethod method) {
        String reachableTargets = String.join(";", method.getReachableTargets());
        
        pw.printf("%s,%s,\"%s\",%s,%b,%b,%b,%b,%b,\"%s\"%n",
                clazz.getClassName(),
                method.getMethodName(),
                method.getMethodSignature(),
                clazz.getComponentType().getDisplayName(),
                clazz.isMainComponent(),
                method.isEntryPoint(),
                method.isReachable(),
                method.isReachesTarget(),
                method.isDirectlyReachesTarget(),
                reachableTargets);
    }
}