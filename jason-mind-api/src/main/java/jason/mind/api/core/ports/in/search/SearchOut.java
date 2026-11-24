package jason.mind.api.core.ports.in.search;

import java.util.List;

/**
 * Output of a search.
 *
 * @param agent  name of the agent
 * @param query  text searched, or null
 * @param window retained cycle window
 * @param total  total number of hits, or null
 * @param hits   hits returned
 * @author Gabriel Lima
 */
public record SearchOut(String agent, String query, Window window, Integer total, List<Hit> hits) {

    /**
     * Retained cycle window.
     *
     * @param oldestCycle oldest retained cycle, or null
     * @param newestCycle newest closed cycle, or null
     */
    public record Window(Integer oldestCycle, Integer newestCycle) {

    }

    /**
     * Search hit.
     *
     * @param type       type of the entity
     * @param entityId   identifier of the entity
     * @param label      label of the entity
     * @param firstCycle first cycle of the hit
     * @param lastCycle  last cycle of the hit, or null
     */
    public record Hit(String type, String entityId, String label, int firstCycle, Integer lastCycle) {

    }
}
