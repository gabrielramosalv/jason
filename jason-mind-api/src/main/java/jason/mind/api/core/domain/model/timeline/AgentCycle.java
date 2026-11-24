package jason.mind.api.core.domain.model.timeline;

import java.util.Objects;

/**
 * Reasoning cycle of a specific agent.
 *
 * @param agent name of the agent
 * @param cycle number of the cycle
 * @author Gabriel Lima
 */
public record AgentCycle(String agent, int cycle) {

    /**
     * Validates the agent cycle.
     *
     * @throws NullPointerException if the agent is null
     */
    public AgentCycle {
        Objects.requireNonNull(agent, "agent");
    }
}
