package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.mind.api.core.domain.model.program.SourceFile;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe cache of agent source files.
 * <p>
 * Reads each source file at most once, from the file system, from a {@code jar:} or {@code file:} URL, or from the
 * Jason jar resources when the path starts with {@code $jasonJar}. Unreadable files get empty content.
 *
 * @author Gabriel Lima
 */
final class SourceFiles {

    /** Source files already read, indexed by their path. */
    private final Map<String, SourceFile> files = new ConcurrentHashMap<>();

    /**
     * Reads the content of a source file.
     *
     * @param path path or URL of the source file
     * @return the UTF-8 content of the file, or an empty string when it cannot be read
     */
    private static String read(String path) {
        try {
            Path p = Path.of(path);
            if (Files.isRegularFile(p)) {
                return Files.readString(p, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
        try {
            String u = path.startsWith("jar:") || path.startsWith("file:") ? path : null;
            if (u == null && path.startsWith("$jasonJar")) {
                URL r = SourceFiles.class.getResource(path.substring("$jasonJar".length()));
                if (r != null) {
                    u = r.toString();
                }
            }
            if (u == null) {
                return "";
            }
            try (InputStream in = new URL(u).openStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the source file of a path, reading it on first access.
     *
     * @param path path or URL of the source file
     * @return the cached source file, or {@code null} when the path is {@code null} or empty
     */
    SourceFile get(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        return this.files.computeIfAbsent(path, p -> new SourceFile(p, read(p)));
    }
}
