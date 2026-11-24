package jason.mind.api.core.ports.in.prune;

import java.util.List;

/**
 * Output of the manual prune request. The request is only registered: the agent applies it at the end of its next
 * cycle, and the client sees it done when {@code oldestCycle} in the system listing reaches the cycle.
 *
 * @param requests what was requested for each agent
 * @author Gabriel Lima
 */
public record PruneOut(List<Request> requests) {

    /**
     * Request registered for one agent.
     *
     * @param agent name of the agent
     * @param until oldest cycle that will be kept, never after the newest closed cycle of the agent
     */
    public record Request(String agent, int until) {

    }
}
