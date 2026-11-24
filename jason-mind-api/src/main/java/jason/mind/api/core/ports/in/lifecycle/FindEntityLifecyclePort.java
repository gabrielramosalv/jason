package jason.mind.api.core.ports.in.lifecycle;

/**
 * Port for querying the lifecycle of an entity.
 *
 * @author Gabriel Lima
 */
public interface FindEntityLifecyclePort {

    /**
     * Finds a page of the lifecycle of an entity.
     *
     * @param in agent, entity and pagination
     * @return lifecycle entries of the entity
     */
    LifecycleOut execute(FindEntityLifecycleIn in);
}
