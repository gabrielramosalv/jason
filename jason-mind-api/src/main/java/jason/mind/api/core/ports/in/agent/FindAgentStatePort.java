package jason.mind.api.core.ports.in.agent;

/**
 * Port for querying the mental state of an agent at a cycle.
 *
 * @author Gabriel Lima
 */
public interface FindAgentStatePort {

    /**
     * Finds the mental state of an agent.
     *
     * @param in agent, cycle and sections to include
     * @return state of the agent at the cycle
     */
    AgentStateOut execute(FindAgentStateIn in);
}
