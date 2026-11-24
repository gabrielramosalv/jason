package jason.mind.api.core.ports.out.clock;

/**
 * Port for reading the current wall-clock time.
 *
 * @author Gabriel Lima
 */
public interface ClockProviderPort {

    /**
     * Returns the current time.
     *
     * @return current wall-clock time, in milliseconds
     */
    long nowMilliseconds();
}
