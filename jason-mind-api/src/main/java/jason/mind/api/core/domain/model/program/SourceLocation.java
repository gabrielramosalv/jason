package jason.mind.api.core.domain.model.program;

/**
 * Location in the source code.
 * Points to a file and, optionally, a line.
 *
 * @param file source file
 * @param line line in the file, or null
 * @author Gabriel Lima
 */
public record SourceLocation(SourceFile file, Integer line) {

}
