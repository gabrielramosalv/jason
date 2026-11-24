package jason.mind.api.core.application.settings;

import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.in.settings.UpdateSettingsIn;
import jason.mind.api.core.ports.in.settings.UpdateSettingsPort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that changes the retention of the history.
 * The new retention is validated against the accepted limits, saved, and reported together with the
 * entities still alive that started before the new horizon.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class UpdateSettingsUseCase implements UpdateSettingsPort {

    /** Repository where the settings are saved. */
    private final SettingsRepositoryPort settingsRepository;

    /** Read-only access to the history, used to find entities beyond the horizon. */
    private final HistoryQueryRepositoryPort historyQuery;

    @Override
    public SettingsOut execute(UpdateSettingsIn in) {
        Long n = in.retentionCycles();
        Long budget = in.memoryBudgetBytes();
        if (n == null && budget == null) {
            throw new InvalidSettingsException("retentionCycles or memoryBudgetBytes is required");
        }
        if (n != null && (n < Settings.MINIMUM_RETENTION || n > Settings.MAXIMUM_RETENTION)) {
            throw new InvalidSettingsException("retentionCycles must be between 1 and 10000000");
        }
        if (budget != null && budget < Settings.MINIMUM_BUDGET) {
            throw new InvalidSettingsException("memoryBudgetBytes must be at least 1048576");
        }
        Settings current = this.settingsRepository.load();
        Settings s = new Settings(n == null ? current.retentionCycles() : n.intValue(),
                budget == null ? current.memoryBudgetBytes() : budget, current.agentRetention());
        this.settingsRepository.save(s);
        return SettingsView.of(s, this.historyQuery, "nextCycleClose");
    }
}
