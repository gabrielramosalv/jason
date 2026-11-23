package jason.mind.api.core.ports.out.settings;

import jason.mind.api.core.domain.model.settings.Settings;

public interface SettingsRepositoryPort {

    Settings load();

    void save(Settings settings);
}
