package jason.mind.api.core.ports.in.settings;

import java.util.List;

/**
 * Output with the history settings.
 *
 * @param retentionCycles   number of retained cycles
 * @param pruneAppliesAt    when the pruning takes effect
 * @param liveBeyondHorizon entities alive since before the retention horizon, per agent
 * @author Gabriel Lima
 */
public record SettingsOut(int retentionCycles, String pruneAppliesAt, List<LiveBeyondHorizon> liveBeyondHorizon) {

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
}
