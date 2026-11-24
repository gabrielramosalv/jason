package jason.mind.api.core.application.history;

/**
 * Exception thrown when a query names an agent that is not in the history.
 *
 * @author Gabriel Lima
 */
public class AgentNotFoundException extends RuntimeException {

    /**
     * Creates the exception with the name of the missing agent.
     *
     * @param agent name of the agent that was not found
     */
    public AgentNotFoundException(String agent) {
        super("agent not found: " + agent);
    }
}
