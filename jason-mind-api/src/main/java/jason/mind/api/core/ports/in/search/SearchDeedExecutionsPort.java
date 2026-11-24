package jason.mind.api.core.ports.in.search;

/**
 * Port for searching the executions of a plan deed.
 *
 * @author Gabriel Lima
 */
public interface SearchDeedExecutionsPort {

    /**
     * Searches the executions of a deed.
     *
     * @param in agent, plan and deed
     * @return hits of the search
     */
    SearchOut execute(SearchDeedExecutionsIn in);
}
