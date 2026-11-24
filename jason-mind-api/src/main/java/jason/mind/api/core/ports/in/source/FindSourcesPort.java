package jason.mind.api.core.ports.in.source;

/**
 * Port for querying the source files of an agent.
 *
 * @author Gabriel Lima
 */
public interface FindSourcesPort {

    /**
     * Finds the source files of an agent.
     *
     * @param in agent
     * @return source files of the agent
     */
    SourcesOut execute(FindSourcesIn in);
}
