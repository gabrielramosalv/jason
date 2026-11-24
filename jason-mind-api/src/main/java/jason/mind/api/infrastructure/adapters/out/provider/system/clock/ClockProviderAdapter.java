package jason.mind.api.infrastructure.adapters.out.provider.system.clock;

import jason.mind.api.core.ports.out.clock.ClockProviderPort;

/**
 * Adapter that provides the current time from the system clock.
 * Returns the wall-clock time in milliseconds since the epoch.
 *
 * @author Gabriel Lima
 */
public class ClockProviderAdapter implements ClockProviderPort {

    @Override
    public long nowMilliseconds() {
        return System.currentTimeMillis();
    }
}
