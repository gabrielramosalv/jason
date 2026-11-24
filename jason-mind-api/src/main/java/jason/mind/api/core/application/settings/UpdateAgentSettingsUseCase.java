package jason.mind.api.core.application.settings;

import jason.mind.api.core.application.history.HistoryView;
import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.in.settings.UpdateAgentSettingsIn;
import jason.mind.api.core.ports.in.settings.UpdateAgentSettingsPort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that sets the retention of one agent, or removes it so that the agent goes back to the default.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class UpdateAgentSettingsUseCase implements UpdateAgentSettingsPort {

    /** Where the settings are kept. */
    private final SettingsRepositoryPort settingsRepository;

    /** Read-only access to the agents. */
    private final HistoryQueryRepositoryPort historyQuery;

    @Override
    public SettingsOut execute(UpdateAgentSettingsIn in) {
        HistoryView.agent(this.historyQuery, in.agent());
        Long n = in.retentionCycles();
        if (n != null && (n < Settings.MINIMUM_RETENTION || n > Settings.MAXIMUM_RETENTION)) {
            throw new InvalidSettingsException("retentionCycles must be between 1 and 10000000");
        }
        Settings s = this.settingsRepository.load().withAgentRetention(in.agent(), n == null ? null : n.intValue());
        this.settingsRepository.save(s);
        return SettingsView.of(s, this.historyQuery, "nextCycleClose");
    }
}
