package jason.mind.api.core.application.settings;

public class InvalidSettingsException extends RuntimeException {

    public InvalidSettingsException(String message) {
        super(message);
    }
}
