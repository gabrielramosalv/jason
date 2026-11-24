package jason.mind.api.core.ports.in.metrics;

/**
 * Input port that finds the monitoring indicators of the Mind.
 *
 * @author Gabriel Lima
 */
public interface FindMetricsPort {

    /**
     * Finds the heap of the JVM and, for each agent, the estimated history size and the mean capture time.
     *
     * @return the indicators
     */
    MetricsOut execute();
}
