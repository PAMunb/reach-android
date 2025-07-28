package br.unb.cic.reach.common.writer;

/**
 * Factory for creating result writers based on output format type.
 * <p>
 * This factory creates appropriate writer instances based on the requested
 * output format, enabling flexible result output and format selection
 * through configuration parameters.
 * <p>
 * ### Architectural Decisions:
 * - Factory pattern for writer instantiation
 * - Type-safe writer creation based on enumeration
 * - Default fallback to CSV format for compatibility
 * <p>
 * ### Role in the System:
 * - Central point for writer creation and format selection
 * - Abstraction enabling multiple output format support
 * - Interface between configuration and result output
 */
public class WriterFactory {

    /**
     * Creates a writer instance for the specified format type.
     *
     * @param type The output format type
     * @return Writer instance for the specified format
     */
    public static Writer create(WriterType type) {
        return switch (type) {
            case JSON -> new JsonWriter();
            case CSV -> new CsvWriter();
            default -> new CsvWriter(); // Default fallback
        };
    }

    /**
     * Creates a writer instance from string format specification.
     *
     * @param typeString String representation of the output format
     * @return Writer instance for the specified format
     */
    public static Writer create(String typeString) {
        WriterType type = WriterType.fromString(typeString);
        return create(type);
    }

    /**
     * Creates the default CSV writer.
     *
     * @return CsvWriter instance as default output format
     */
    public static Writer createDefault() {
        return new CsvWriter();
    }
}