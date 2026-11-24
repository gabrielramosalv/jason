package jason.mind.api.core.application.history;

/**
 * Exception thrown when a query names an entity that does not exist or is not visible for the agent.
 *
 * @author Gabriel Lima
 */
public class EntityNotFoundException extends RuntimeException {

    /**
     * Creates the exception with the agent and the requested entity id.
     *
     * @param agent name of the agent being queried
     * @param id    formatted id of the requested entity
     */
    public EntityNotFoundException(String agent, String id) {
        super("entity " + id + " not found in " + agent);
    }
}
