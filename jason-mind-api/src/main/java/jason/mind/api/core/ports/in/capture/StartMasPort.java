package jason.mind.api.core.ports.in.capture;

/**
 * Port for recording the start of a multi-agent system.
 *
 * @author Gabriel Lima
 */
public interface StartMasPort {

    /**
     * Records the start of a multi-agent system.
     *
     * @param in name of the system
     */
    void execute(StartMasIn in);
}
