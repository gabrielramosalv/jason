package jason.mind.api.core.ports.in.capture;

/**
 * Port for recording the end of a reasoning cycle.
 *
 * @author Gabriel Lima
 */
public interface FinishCyclePort {

    /**
     * Records the end of a reasoning cycle.
     *
     * @param in agent, cycle and phases that ran
     */
    void execute(FinishCycleIn in);
}
