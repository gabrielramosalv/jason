package jason.mind.api.core.application.settings;

import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.in.settings.UpdateSettingsIn;
import jason.mind.api.core.ports.in.settings.UpdateSettingsPort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;

public class UpdateSettingsUseCase implements UpdateSettingsPort {

    private final SettingsRepositoryPort settingsRepository;
    private final HistoryQueryRepositoryPort historyQuery;

    public UpdateSettingsUseCase(SettingsRepositoryPort settingsRepository, HistoryQueryRepositoryPort historyQuery) {
        this.settingsRepository = settingsRepository;
        this.historyQuery = historyQuery;
    }

    @Override
    public SettingsOut execute(UpdateSettingsIn in) {
        Long n = in.retentionCycles();
        if (n == null || n < Settings.MINIMUM_RETENTION || n > Settings.MAXIMUM_RETENTION)
            throw new InvalidSettingsException("retentionCycles must be between 1 and 10000000");
        Settings s = new Settings(n.intValue());
        settingsRepository.save(s);
        return new SettingsOut(s.retentionCycles(), "nextCycleClose", LiveBeyondHorizon.compute(historyQuery, s.retentionCycles()));
    }
}
