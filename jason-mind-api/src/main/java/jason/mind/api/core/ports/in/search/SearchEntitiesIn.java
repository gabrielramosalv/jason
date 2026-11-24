package jason.mind.api.core.ports.in.search;

import java.util.Set;

/**
 * Input of the search of entities.
 *
 * @param agent name of the agent
 * @param q     text searched
 * @param types entity types to keep, or null for all
 * @param from  first cycle of the range, or null
 * @param to    last cycle of the range, or null
 * @param limit maximum number of hits, or null
 * @author Gabriel Lima
 */
public record SearchEntitiesIn(String agent, String q, Set<String> types, Integer from, Integer to, Integer limit) {

}
