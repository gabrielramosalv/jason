package jason.mind.api.core.application.settings;

import jason.mind.api.core.ports.in.settings.FindSettingsPort;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;
import lombok.RequiredArgsConstructor;

/**
 * Use case that reads the current settings.
 * Along with the retention, it reports the entities still alive that started before the retention horizon.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class FindSettingsUseCase implements FindSettingsPort {

    /** Repository that holds the settings. */
    private final SettingsRepositoryPort settingsRepository;

    /** Read-only access to the history, used to find entities beyond the horizon. */
    private final HistoryQueryRepositoryPort historyQuery;

    @Override
    public SettingsOut execute() {
        int retention = this.settingsRepository.load().retentionCycles();
        return new SettingsOut(retention, null, LiveBeyondHorizon.compute(this.historyQuery, retention));
    }
}
