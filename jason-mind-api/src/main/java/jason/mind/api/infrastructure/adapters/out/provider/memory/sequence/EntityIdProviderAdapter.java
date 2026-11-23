package jason.mind.api.infrastructure.adapters.out.provider.memory.sequence;

import java.util.concurrent.atomic.AtomicLong;

import jason.mind.api.core.ports.out.sequence.EntityIdProviderPort;

public class EntityIdProviderAdapter implements EntityIdProviderPort {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public long next() {
        return counter.incrementAndGet();
    }
}
