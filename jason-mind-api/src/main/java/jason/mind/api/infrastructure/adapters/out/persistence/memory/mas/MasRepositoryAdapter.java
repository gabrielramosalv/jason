package jason.mind.api.infrastructure.adapters.out.persistence.memory.mas;

import jason.mind.api.core.ports.out.mas.MasRepositoryPort;

/**
 * Adapter that keeps the name of the multi-agent system in memory.
 * Stores an empty name when none is given.
 *
 * @author Gabriel Lima
 */
public class MasRepositoryAdapter implements MasRepositoryPort {

    /** Name of the multi-agent system, never {@code null}. */
    private volatile String name = "";

    @Override
    public void save(String name) {
        this.name = name == null ? "" : name;
    }

    @Override
    public String name() {
        return this.name;
    }
}
