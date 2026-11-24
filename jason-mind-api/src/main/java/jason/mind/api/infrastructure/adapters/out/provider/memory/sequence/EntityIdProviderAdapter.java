package jason.mind.api.infrastructure.adapters.out.provider.memory.sequence;

import jason.mind.api.core.ports.out.sequence.EntityIdProviderPort;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Adapter that provides entity ids from an in-memory counter.
 * Ids are unique and increasing, starting at 1.
 *
 * @author Gabriel Lima
 */
public class EntityIdProviderAdapter implements EntityIdProviderPort {

    /** Last id provided. */
    private final AtomicLong counter = new AtomicLong();

    @Override
    public long next() {
        return this.counter.incrementAndGet();
    }
}
