package jason.mind.api.infrastructure.adapters.in.jason.capture;

import jason.mind.api.core.domain.model.program.SourceFile;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class SourceFiles {

    private final Map<String, SourceFile> files = new ConcurrentHashMap<>();

    SourceFile get(String path) {
        if (path == null || path.isEmpty()) return null;
        return files.computeIfAbsent(path, p -> new SourceFile(p, read(p)));
    }

    private static String read(String path) {
        try {
            Path p = Path.of(path);
            if (Files.isRegularFile(p)) return Files.readString(p, StandardCharsets.UTF_8);
        } catch (Exception ignored) {

        }
        try {
            String u = path.startsWith("jar:") || path.startsWith("file:") ? path : null;
            if (u == null && path.startsWith("$jasonJar")) {
                URL r = SourceFiles.class.getResource(path.substring("$jasonJar".length()));
                if (r != null) u = r.toString();
            }
            if (u == null) return "";
            try (InputStream in = new URL(u).openStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "";
        }
    }
}
