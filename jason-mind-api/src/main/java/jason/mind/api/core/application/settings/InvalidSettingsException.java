package jason.mind.api.core.application.settings;

/**
 * Exception thrown when new settings are outside the accepted limits.
 *
 * @author Gabriel Lima
 */
public class InvalidSettingsException extends RuntimeException {

    /**
     * Creates the exception with the description of the invalid setting.
     *
     * @param message description of what is invalid in the settings
     */
    public InvalidSettingsException(String message) {
        super(message);
    }
}
