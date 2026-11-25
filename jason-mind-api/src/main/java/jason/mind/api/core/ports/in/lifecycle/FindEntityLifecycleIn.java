package jason.mind.api.core.ports.in.lifecycle;

/**
 * Input of the query of the lifecycle of an entity.
 *
 * @param agent  name of the agent
 * @param id     identifier of the entity
 * @param after  cursor after which the page starts in ascending order, or null
 * @param before cursor before which the page starts in descending order, or null
 * @param limit  maximum number of entries, or null
 * @param order  {@code asc} (default, oldest first) or {@code desc} (newest first), or null
 * @author Gabriel Lima
 */
public record FindEntityLifecycleIn(String agent, String id, Long after, Long before, Integer limit, String order) {

}
