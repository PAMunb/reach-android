package br.unb.cic.reach.common.writer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import br.unb.cic.reach.common.analysis.ReachabilityResult;
import br.unb.cic.reach.common.writer.json.JsonOutput;

/**
 * JSON format writer for reachability analysis results.
 *
 * This writer outputs reachability analysis results in structured JSON format,
 * providing hierarchical data representation suitable for programmatic access,
 * web applications, and detailed analysis with path information.
 *
 * ### Architectural Decisions:
 * - Hierarchical JSON structure for natural data organization
 * - Pretty printing for human readability and debugging
 * - Comprehensive data model including paths and metadata
 * - Structured output suitable for programmatic processing
 *
 * ### Role in the System:
 * - Advanced output format for detailed analysis results
 * - Interface for web applications and programmatic access
 * - Foundation for visualization and interactive analysis tools
 */
public class JsonWriter implements Writer {
    private static final Logger log = LoggerFactory.getLogger(JsonWriter.class);
    
    @Override
    public void write(ReachabilityResult result, File outputFile) {
        log.info("Writing JSON results to: {}", outputFile.getAbsolutePath());
        
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        JsonOutput jsonOutput = new JsonOutput(result);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(jsonOutput, writer);
            
            log.info("JSON output completed: {} classes processed", 
                    result.getAppInfo().getClasses().size());
        } catch (IOException e) {
            log.error("Error writing JSON file: {}", e.getMessage());
            throw new RuntimeException("Failed to write JSON output", e);
        }
    }
}