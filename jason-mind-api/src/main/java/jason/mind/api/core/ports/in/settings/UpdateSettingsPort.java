package jason.mind.api.core.ports.in.settings;

/**
 * Port for updating the history settings.
 *
 * @author Gabriel Lima
 */
public interface UpdateSettingsPort {

    /**
     * Updates the settings.
     *
     * @param in new values
     * @return settings after the update
     */
    SettingsOut execute(UpdateSettingsIn in);
}
