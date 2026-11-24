package jason.mind.api.core.ports.in.capture;

/**
 * Port for registering the source files of an agent.
 *
 * @author Gabriel Lima
 */
public interface LoadSourcesPort {

    /**
     * Registers the source files of an agent.
     *
     * @param in agent and its source files
     */
    void execute(LoadSourcesIn in);
}
