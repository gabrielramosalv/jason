package jason.mind.api.core.ports.in.cycle;

/**
 * Port for listing the cycles of an agent.
 *
 * @author Gabriel Lima
 */
public interface ListCyclesPort {

    /**
     * Lists the cycles of an agent in a range.
     *
     * @param in agent and range
     * @return cycles in the range
     */
    CyclesOut execute(ListCyclesIn in);
}
