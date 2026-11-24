package jason.mind.api.core.ports.in.prune;

/**
 * Input port that asks for the history to be pruned up to a cycle.
 *
 * @author Gabriel Lima
 */
public interface RequestPrunePort {

    /**
     * Registers the request; each agent applies it at the end of its next cycle.
     *
     * @param in agent (or all agents) and the oldest cycle to keep
     * @return what was requested for each agent
     */
    PruneOut execute(RequestPruneIn in);
}
