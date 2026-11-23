package jason.mind.api.infrastructure.adapters.out.persistence.memory.mas;

import jason.mind.api.core.ports.out.mas.MasRepositoryPort;

public class MasRepositoryAdapter implements MasRepositoryPort {

    private volatile String name = "";

    @Override
    public void save(String name) {
        this.name = name == null ? "" : name;
    }

    @Override
    public String name() {
        return name;
    }
}
