package jason.mind.api.core.ports.in.capture;

/**
 * Port for recording the end of an agent.
 *
 * @author Gabriel Lima
 */
public interface EndAgentPort {

    /**
     * Records the end of an agent.
     *
     * @param in agent and its last cycle
     */
    void execute(EndAgentIn in);
}
