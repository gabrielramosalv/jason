package jason.mind.api.core.application.history;

/**
 * Exception thrown when a query names an entity that was already pruned from the history.
 *
 * @author Gabriel Lima
 */
public class EntityPrunedException extends RuntimeException {

    /**
     * Creates the exception with the agent and the pruned entity id.
     *
     * @param agent name of the agent being queried
     * @param id    formatted id of the pruned entity
     */
    public EntityPrunedException(String agent, String id) {
        super("entity " + id + " of " + agent + " was pruned");
    }
}
