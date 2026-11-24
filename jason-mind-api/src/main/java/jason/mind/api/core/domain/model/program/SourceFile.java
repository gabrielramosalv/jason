package jason.mind.api.core.domain.model.program;

import java.util.Objects;

/**
 * Source code file of an agent.
 * Holds its path and its content.
 *
 * @param path    path of the file
 * @param content text of the file
 * @author Gabriel Lima
 */
public record SourceFile(String path, String content) {

    /**
     * Validates the file.
     *
     * @throws NullPointerException if the path or the content is null
     */
    public SourceFile {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(content, "content");
    }

    /**
     * Returns the name of the file.
     *
     * @return last segment of the path after a slash, backslash or colon
     */
    public String fileName() {
        int i = Math.max(Math.max(this.path.lastIndexOf('/'), this.path.lastIndexOf('\\')), this.path.lastIndexOf(':'));
        return i < 0 ? this.path : this.path.substring(i + 1);
    }
}
