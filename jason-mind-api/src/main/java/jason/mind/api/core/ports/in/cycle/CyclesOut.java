package jason.mind.api.core.ports.in.cycle;

import java.util.List;

/**
 * Output with the cycles of an agent in a range.
 *
 * @param agent       name of the agent
 * @param oldestCycle oldest retained cycle, or null
 * @param newestCycle newest closed cycle, or null
 * @param from        first cycle of the range, or null
 * @param to          last cycle of the range, or null
 * @param cycles      cycles in the range
 * @author Gabriel Lima
 */
public record CyclesOut(String agent, Integer oldestCycle, Integer newestCycle, Integer from, Integer to,
                        List<CycleItem> cycles) {

    /**
     * Cycle of the range.
     *
     * @param cycle  number of the cycle
     * @param phases phases that ran
     */
    public record CycleItem(int cycle, Phases phases) {

    }

    /**
     * Phases that ran in a cycle.
     *
     * @param sense      whether the sense phase ran
     * @param deliberate whether the deliberate phase ran
     * @param act        whether the act phase ran
     */
    public record Phases(boolean sense, boolean deliberate, boolean act) {

    }
}
