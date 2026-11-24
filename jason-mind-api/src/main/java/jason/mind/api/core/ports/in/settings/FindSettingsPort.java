package jason.mind.api.core.ports.in.settings;

/**
 * Port for querying the history settings.
 *
 * @author Gabriel Lima
 */
public interface FindSettingsPort {

    /**
     * Finds the current settings.
     *
     * @return current settings
     */
    SettingsOut execute();
}
