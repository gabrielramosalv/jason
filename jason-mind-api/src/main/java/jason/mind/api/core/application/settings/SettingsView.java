package jason.mind.api.core.application.settings;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the answer that describes the settings together with the retention of each agent and the alive entities
 * beyond each horizon.
 *
 * @author Gabriel Lima
 */
final class SettingsView {

    /**
     * Prevents instantiation of this utility class.
     */
    private SettingsView() {
    }

    /**
     * Builds the settings answer.
     *
     * @param settings       settings in force
     * @param query          read-only access to the history
     * @param pruneAppliesAt when a change takes effect, or null for a plain read
     * @return the answer
     */
    static SettingsOut of(Settings settings, HistoryQueryRepositoryPort query, String pruneAppliesAt) {
        List<SettingsOut.AgentRetention> agents = new ArrayList<>();
        for (Agent a : query.agents()) {
            agents.add(new SettingsOut.AgentRetention(a.getName(), settings.retentionFor(a.getName()),
                    settings.agentRetention().containsKey(a.getName())));
        }
        agents.sort(Comparator.comparing(SettingsOut.AgentRetention::agent));
        return new SettingsOut(settings.retentionCycles(), settings.memoryBudgetBytes(), agents, pruneAppliesAt,
                LiveBeyondHorizon.compute(query, settings));
    }
}
