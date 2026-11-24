package jason.mind.api.core.application.history;

/**
 * Exception thrown when a query asks for a cycle that was already pruned from the history.
 *
 * @author Gabriel Lima
 */
public class CyclePrunedException extends RuntimeException {

    /**
     * Creates the exception with the agent and the pruned cycle.
     *
     * @param agent name of the agent being queried
     * @param cycle number of the pruned cycle
     */
    public CyclePrunedException(String agent, int cycle) {
        super("cycle " + cycle + " of " + agent + " was pruned");
    }
}
