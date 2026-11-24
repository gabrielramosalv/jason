package jason.mind.api.core.ports.in.cycle;

/**
 * Port for querying the reasoning of an agent in a cycle.
 *
 * @author Gabriel Lima
 */
public interface FindCycleReasoningPort {

    /**
     * Finds the reasoning of an agent in a cycle.
     *
     * @param in agent and cycle
     * @return reasoning steps of the cycle
     */
    CycleReasoningOut execute(FindCycleReasoningIn in);
}
