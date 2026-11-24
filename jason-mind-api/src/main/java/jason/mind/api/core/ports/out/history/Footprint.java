package jason.mind.api.core.ports.out.history;

/**
 * Count of what an agent keeps in memory, used to estimate the size of its retained history.
 *
 * @param entities        entities held, including the incoming messages
 * @param timelineEntries entries of every timeline and collection timeline of those entities
 * @param effects         occurrences the entities caused in the agent
 * @author Gabriel Lima
 */
public record Footprint(long entities, long timelineEntries, long effects) {

}
