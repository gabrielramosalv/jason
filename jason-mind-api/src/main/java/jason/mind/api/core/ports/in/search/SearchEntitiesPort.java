package jason.mind.api.core.ports.in.search;

/**
 * Port for searching the entities of an agent.
 *
 * @author Gabriel Lima
 */
public interface SearchEntitiesPort {

    /**
     * Searches entities.
     *
     * @param in agent, text, types and range
     * @return hits of the search
     */
    SearchOut execute(SearchEntitiesIn in);
}
