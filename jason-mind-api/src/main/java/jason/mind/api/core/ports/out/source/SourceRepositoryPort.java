package jason.mind.api.core.ports.out.source;

import java.util.List;
import java.util.Optional;

import jason.mind.api.core.domain.model.program.SourceFile;

public interface SourceRepositoryPort {

    List<SourceFile> sourcesOf(String agentName);

    Optional<SourceFile> sourceAt(String path);

    void register(String agentName, List<SourceFile> files);
}
