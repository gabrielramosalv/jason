package jason.mind.api.core.application.settings;

import jason.mind.api.core.ports.in.settings.FindSettingsPort;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;

public class FindSettingsUseCase implements FindSettingsPort {

    private final SettingsRepositoryPort settingsRepository;
    private final HistoryQueryRepositoryPort historyQuery;

    public FindSettingsUseCase(SettingsRepositoryPort settingsRepository, HistoryQueryRepositoryPort historyQuery) {
        this.settingsRepository = settingsRepository;
        this.historyQuery = historyQuery;
    }

    @Override
    public SettingsOut execute() {
        int retention = settingsRepository.load().retentionCycles();
        return new SettingsOut(retention, null, LiveBeyondHorizon.compute(historyQuery, retention));
    }
}
