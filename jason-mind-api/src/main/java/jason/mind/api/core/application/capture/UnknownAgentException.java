package jason.mind.api.core.application.capture;

/**
 * Exception thrown when an occurrence refers to an agent that was never started.
 *
 * @author Gabriel Lima
 */
public class UnknownAgentException extends RuntimeException {

    /**
     * Creates the exception with the name of the unknown agent.
     *
     * @param agent name of the agent that is not in the history
     */
    public UnknownAgentException(String agent) {
        super("unknown agent: " + agent);
    }
}
