package jason.mind.api.infrastructure.adapters.out.persistence.memory.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jason.mind.api.core.domain.model.program.SourceFile;
import jason.mind.api.core.ports.out.source.SourceRepositoryPort;

public class SourceRepositoryAdapter implements SourceRepositoryPort {

    private final Map<String, List<SourceFile>> byAgent = new ConcurrentHashMap<>();
    private final Map<String, SourceFile> byPath = new ConcurrentHashMap<>();

    @Override
    public List<SourceFile> sourcesOf(String agentName) {
        return byAgent.getOrDefault(agentName, List.of());
    }

    @Override
    public Optional<SourceFile> sourceAt(String path) {
        return Optional.ofNullable(byPath.get(path));
    }

    @Override
    public void register(String agentName, List<SourceFile> files) {
        List<SourceFile> list = new ArrayList<>(sourcesOf(agentName));
        for (SourceFile f : files) {
            if (!list.contains(f)) list.add(f);
            if (!f.path().isEmpty()) byPath.put(f.path(), f);
        }
        byAgent.put(agentName, List.copyOf(list));
    }
}
