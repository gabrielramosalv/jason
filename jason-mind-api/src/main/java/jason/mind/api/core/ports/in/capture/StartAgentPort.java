package jason.mind.api.core.ports.in.capture;

/**
 * Port for recording the start of an agent.
 *
 * @author Gabriel Lima
 */
public interface StartAgentPort {

    /**
     * Records the start of an agent.
     *
     * @param in agent, configuration and creation context
     */
    void execute(StartAgentIn in);
}
