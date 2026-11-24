package jason.mind.api.core.ports.in.settings;

import java.util.List;

/**
 * Output with the history settings.
 *
 * @param memoryBudgetBytes bytes the history of all agents together may use; the largest long means no limit
 * @param agents            retention of each agent: the effective value and whether the agent overrides the default
 * @param retentionCycles   number of retained cycles
 * @param pruneAppliesAt    when the pruning takes effect
 * @param liveBeyondHorizon entities alive since before the retention horizon, per agent
 * @author Gabriel Lima
 */
public record SettingsOut(int retentionCycles, long memoryBudgetBytes, List<AgentRetention> agents, String pruneAppliesAt, List<LiveBeyondHorizon> liveBeyondHorizon) {

    /**
     * Entities of an agent alive since before the retention horizon.
     *
     * @param agent  name of the agent
     * @param count  number of such entities
     * @param oldest oldest of them
     */
    public record LiveBeyondHorizon(String agent, int count, Oldest oldest) {

    }

    /**
     * Oldest entity alive beyond the horizon.
     *
     * @param type       type of the entity
     * @param entityId   identifier of the entity
     * @param label      label of the entity
     * @param sinceCycle cycle in which it started
     */
    public record Oldest(String type, String entityId, String label, int sinceCycle) {

    }

    /**
     * Retention of one agent.
     *
     * @param agent           name of the agent
     * @param retentionCycles cycles the agent retains
     * @param overridden      true when the agent overrides the default of the system
     */
    public record AgentRetention(String agent, int retentionCycles, boolean overridden) {

    }
}
