package jason.mind.api.core.domain.model.program;

import java.util.Objects;

public record SourceFile(String path, String content) {

    public SourceFile {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(content, "content");
    }

    public String fileName() {
        int i = Math.max(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')), path.lastIndexOf(':'));
        return i < 0 ? path : path.substring(i + 1);
    }
}
