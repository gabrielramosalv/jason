package jason.mind.api.infrastructure.adapters.out.provider.memory.sequence;

import java.util.concurrent.atomic.AtomicLong;

import jason.mind.api.core.ports.out.sequence.SequenceProviderPort;

public class SequenceProviderAdapter implements SequenceProviderPort {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public long next(String agent) {
        return counter.incrementAndGet();
    }
}
