package br.unb.cic.reach.common.extractor;

/**
 * Exception thrown when an unsupported application format is encountered.
 * <p>
 * This exception indicates that the requested application format is not
 * supported by the current extractor factory configuration, providing
 * clear error messaging for unsupported file types.
 */
public class UnsupportedFormatException extends RuntimeException {

    public UnsupportedFormatException(String message) {
        super(message);
    }

    public UnsupportedFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}