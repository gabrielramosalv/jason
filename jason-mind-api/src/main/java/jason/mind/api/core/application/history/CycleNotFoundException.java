package jason.mind.api.core.application.history;

/**
 * Exception thrown when a query asks for a cycle that is not closed yet or does not exist.
 *
 * @author Gabriel Lima
 */
public class CycleNotFoundException extends RuntimeException {

    /**
     * Creates the exception with the agent and the requested cycle.
     *
     * @param agent name of the agent being queried
     * @param cycle number of the requested cycle
     */
    public CycleNotFoundException(String agent, int cycle) {
        super("cycle " + cycle + " of " + agent + " is not closed");
    }
}
