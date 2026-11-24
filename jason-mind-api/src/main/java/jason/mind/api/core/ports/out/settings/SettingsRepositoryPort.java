package jason.mind.api.core.ports.out.settings;

import jason.mind.api.core.domain.model.settings.Settings;

/**
 * Port for storing the history settings.
 *
 * @author Gabriel Lima
 */
public interface SettingsRepositoryPort {

    /**
     * Loads the settings.
     *
     * @return current settings
     */
    Settings load();

    /**
     * Saves the settings.
     *
     * @param settings settings to save
     */
    void save(Settings settings);
}
