package jason.mind.api.core.application.capture;

/**
 * Exception thrown when an occurrence refers to an entity id that is not in the history of the agent.
 *
 * @author Gabriel Lima
 */
public class UnknownEntityException extends RuntimeException {

    /**
     * Creates the exception with the agent and the unknown entity id.
     *
     * @param agent name of the agent whose history was searched
     * @param id    id of the entity that was not found
     */
    public UnknownEntityException(String agent, long id) {
        super("entity " + id + " unknown in agent " + agent);
    }
}
