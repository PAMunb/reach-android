package br.unb.cic.reach.common.writer;

/**
 * Enumeration of supported output writer types.
 *
 * This enumeration defines the available output formats for reachability
 * analysis results, enabling format selection through configuration and
 * command line parameters.
 */
public enum WriterType {
    CSV("csv"),
    JSON("json");
    
    private final String extension;
    
    WriterType(String extension) {
        this.extension = extension;
    }
    
    public String getExtension() {
        return extension;
    }
    
    /**
     * Parses writer type from string representation.
     *
     * Converts string format specification to WriterType enumeration,
     * enabling command line parameter processing and configuration parsing.
     *
     * @param typeString The string representation of the writer type
     * @return WriterType corresponding to the string, CSV as default
     */
    public static WriterType fromString(String typeString) {
        if (typeString == null || typeString.isEmpty()) {
            return CSV;
        }
        
        String lowerType = typeString.toLowerCase();
        for (WriterType type : values()) {
            if (type.extension.equals(lowerType)) {
                return type;
            }
        }
        
        return CSV; // Default fallback
    }
}