package jason.mind.api.infrastructure.adapters.out.persistence.memory.source;

import jason.mind.api.core.domain.model.program.SourceFile;
import jason.mind.api.core.ports.out.source.SourceRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter that keeps the source files of the agents in memory.
 * Indexes the files by agent and by path.
 *
 * @author Gabriel Lima
 */
public class SourceRepositoryAdapter implements SourceRepositoryPort {

    /** Immutable list of source files of each agent, by agent name. */
    private final Map<String, List<SourceFile>> byAgent = new ConcurrentHashMap<>();

    /** Source files with a non-empty path, by path. */
    private final Map<String, SourceFile> byPath = new ConcurrentHashMap<>();

    @Override
    public List<SourceFile> sourcesOf(String agentName) {
        return this.byAgent.getOrDefault(agentName, List.of());
    }

    @Override
    public Optional<SourceFile> sourceAt(String path) {
        return Optional.ofNullable(this.byPath.get(path));
    }

    @Override
    public void register(String agentName, List<SourceFile> files) {
        List<SourceFile> list = new ArrayList<>(sourcesOf(agentName));
        for (SourceFile f : files) {
            if (!list.contains(f)) {
                list.add(f);
            }
            if (!f.path().isEmpty()) {
                this.byPath.put(f.path(), f);
            }
        }
        this.byAgent.put(agentName, List.copyOf(list));
    }
}
