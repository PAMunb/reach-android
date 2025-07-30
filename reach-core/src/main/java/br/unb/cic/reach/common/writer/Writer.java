package br.unb.cic.reach.common.writer;

import java.io.File;
import java.io.IOException;

import br.unb.cic.reach.common.analysis.ReachabilityResult;

/**
 * Interface for writing reachability analysis results to output files.
 * <p>
 * This interface provides a unified contract for formatting and outputting
 * reachability analysis results in various formats, enabling flexible
 * result presentation and integration with different analysis workflows.
 * <p>
 * ### Architectural Decisions:
 * - Unified interface for multiple output format support
 * - File-based output for standard analysis workflows
 * - ReachabilityResult parameter for comprehensive data access
 * <p>
 * ### Role in the System:
 * - Contract for result formatting implementations
 * - Abstraction enabling multiple output format support
 * - Interface between analysis results and external systems
 */
public interface Writer {

    /**
     * Writes reachability analysis results to the specified file.
     * <p>
     * Formats and outputs comprehensive reachability analysis results using
     * the writer's specific format, creating output suitable for further
     * analysis, reporting, or integration with external tools.
     *
     * @param result     The reachability analysis results to write
     * @param outputFile The file to write results to
     * @throws IOException if file writing operations fail
     */
    void write(ReachabilityResult result, File outputFile) throws IOException;
}