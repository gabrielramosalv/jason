package jason.mind.api.core.ports.out.metrics;

import java.util.OptionalDouble;

/**
 * Port that keeps the time the capture took in the latest cycles of each agent.
 *
 * @author Gabriel Lima
 */
public interface CaptureTimeRepositoryPort {

    /**
     * Records the capture time of a closed cycle.
     *
     * @param agent name of the agent
     * @param nanos time spent inside the capture during the cycle, in nanoseconds
     */
    void record(String agent, long nanos);

    /**
     * Returns the mean capture time over the latest cycles recorded for an agent.
     *
     * @param agent name of the agent
     * @return mean in nanoseconds, or empty when no cycle was recorded
     */
    OptionalDouble meanNanos(String agent);
}
