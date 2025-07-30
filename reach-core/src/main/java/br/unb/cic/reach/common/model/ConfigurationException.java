package br.unb.cic.reach.common.model;

/**
 * Configuration exception for parameter validation failures.
 * 
 * This exception is thrown when configuration validation fails during
 * ConfigMatrix construction, providing detailed error messages to help
 * users correct configuration issues.
 */
public class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}