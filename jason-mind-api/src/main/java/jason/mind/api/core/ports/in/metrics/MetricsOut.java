package jason.mind.api.core.ports.in.metrics;

import java.util.List;

/**
 * Output with the monitoring indicators of the Mind.
 *
 * @param heapUsedBytes heap of the whole JVM in use, which mixes Mind, Jason and the user's application
 * @param heapMaxBytes  largest heap the JVM may use, or null when it is not defined
 * @param agents        indicators of each agent, sorted by name
 * @author Gabriel Lima
 */
public record MetricsOut(long heapUsedBytes, Long heapMaxBytes, List<AgentMetrics> agents) {

    /**
     * Indicators of one agent.
     *
     * @param name                  name of the agent
     * @param retainedCycles        number of cycles retained in the history, or 0 when none was closed
     * @param estimatedHistoryBytes approximate size of the retained history; an estimate, not a measure
     * @param meanCaptureMicros     mean time the capture took per cycle over the latest 100 cycles, in microseconds,
     *                              or null when no cycle was measured
     * @param entities              entities held
     * @param timelineEntries       timeline entries held
     * @param effects               caused occurrences held
     */
    public record AgentMetrics(String name, int retainedCycles, long estimatedHistoryBytes, Double meanCaptureMicros,
                               long entities, long timelineEntries, long effects) {

    }
}
