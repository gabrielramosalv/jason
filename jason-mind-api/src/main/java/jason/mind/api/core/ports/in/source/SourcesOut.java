package jason.mind.api.core.ports.in.source;

import java.util.List;

public record SourcesOut(String agent, List<FileOut> files) {

    public record FileOut(String path, String content) {
    }
}
