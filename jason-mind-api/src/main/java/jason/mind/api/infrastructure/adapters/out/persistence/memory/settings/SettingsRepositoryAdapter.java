package jason.mind.api.infrastructure.adapters.out.persistence.memory.settings;

import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;

public class SettingsRepositoryAdapter implements SettingsRepositoryPort {

    public static final String RETENTION_PROPERTY = "mind.retentionCycles";

    private volatile Settings settings;

    public SettingsRepositoryAdapter() {
        int initial = Settings.DEFAULT_RETENTION;
        String p = System.getProperty(RETENTION_PROPERTY);
        if (p != null) {
            try {
                initial = Integer.parseInt(p.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (initial < Settings.MINIMUM_RETENTION || initial > Settings.MAXIMUM_RETENTION) initial = Settings.DEFAULT_RETENTION;
        settings = new Settings(initial);
    }

    @Override
    public Settings load() {
        return settings;
    }

    @Override
    public void save(Settings settings) {
        this.settings = settings;
    }
}
