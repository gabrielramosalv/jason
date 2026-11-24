package jason.mind.api.core.ports.in.mas;

import java.util.List;

/**
 * Output with the multi-agent system and its agents.
 *
 * @param mas      name of the system
 * @param versions versions of the components
 * @param agents   agents of the system
 * @author Gabriel Lima
 */
public record MasOut(String mas, Versions versions, List<AgentItem> agents) {

    /**
     * Versions of the components.
     *
     * @param mind  version of the mind API
     * @param jason version of Jason
     */
    public record Versions(String mind, String jason) {

    }

    /**
     * Agent of the system.
     *
     * @param name        name of the agent
     * @param active      whether the agent is active
     * @param ended       whether the agent ended
     * @param oldestCycle oldest retained cycle, or null
     * @param newestCycle newest closed cycle, or null
     */
    public record AgentItem(String name, boolean active, boolean ended, Integer oldestCycle, Integer newestCycle) {

    }
}
