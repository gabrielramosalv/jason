package jason.mind.api.infrastructure.adapters.out.provider.system.clock;

import jason.mind.api.core.ports.out.clock.ClockProviderPort;

public class ClockProviderAdapter implements ClockProviderPort {

    @Override
    public long nowMilliseconds() {
        return System.currentTimeMillis();
    }
}
