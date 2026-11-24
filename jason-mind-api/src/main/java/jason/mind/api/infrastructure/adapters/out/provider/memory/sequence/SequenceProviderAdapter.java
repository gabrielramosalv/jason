package jason.mind.api.infrastructure.adapters.out.provider.memory.sequence;

import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Adapter that provides sequence numbers from an in-memory counter.
 * A single counter is shared by all agents, so sequences are globally unique and increasing, starting at 1.
 *
 * @author Gabriel Lima
 */
public class SequenceProviderAdapter implements SequenceProviderPort {

    /** Last sequence provided. */
    private final AtomicLong counter = new AtomicLong();

    @Override
    public long next(String agent) {
        return this.counter.incrementAndGet();
    }
}
