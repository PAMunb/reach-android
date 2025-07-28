package br.unb.cic.reach.jar;

/**
 * Exception thrown when no entry points can be found in a JAR file.
 *
 * This exception is thrown when automatic main() method detection fails
 * and no custom entry points are specified, indicating that manual
 * entry point configuration is required for analysis.
 */
public class EntryPointNotFoundException extends RuntimeException {
    
    public EntryPointNotFoundException(String message) {
        super(message);
    }
    
    public EntryPointNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}