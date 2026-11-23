package jason.mind.api.core.ports.in.capture;

import java.util.List;

import jason.mind.api.core.domain.model.program.SourceFile;

public record LoadSourcesIn(String agent, List<SourceFile> files) {
}
