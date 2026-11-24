package jason.mind.api.infrastructure.adapters.out.persistence.memory.settings;

import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.ports.out.settings.SettingsRepositoryPort;

/**
 * Adapter that keeps the settings in memory.
 * Starts with the retention read from the {@value #RETENTION_PROPERTY} system property when it is a valid value.
 *
 * @author Gabriel Lima
 */
public class SettingsRepositoryAdapter implements SettingsRepositoryPort {

    /** System property that holds the initial retention of cycles. */
    public static final String RETENTION_PROPERTY = "mind.retentionCycles";

    /** Name of the system property that sets the memory budget in bytes, or {@code unlimited}. */
    public static final String BUDGET_PROPERTY = "mind.memoryBudgetBytes";

    /** Current settings. */
    private volatile Settings settings;

    /**
     * Creates the adapter with the retention from the {@value #RETENTION_PROPERTY} system property, falling back to
     * the default retention when the property is absent, not a number or outside the allowed range.
     */
    public SettingsRepositoryAdapter() {
        int initial = Settings.DEFAULT_RETENTION;
        String p = System.getProperty(RETENTION_PROPERTY);
        if (p != null) {
            try {
                initial = Integer.parseInt(p.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (initial < Settings.MINIMUM_RETENTION || initial > Settings.MAXIMUM_RETENTION) {
            initial = Settings.DEFAULT_RETENTION;
        }
        long budget = Settings.defaultBudget(Runtime.getRuntime().maxMemory());
        String b = System.getProperty(BUDGET_PROPERTY);
        if (b != null) {
            if (b.trim().equalsIgnoreCase("unlimited")) {
                budget = Settings.UNLIMITED;
            } else {
                try {
                    long v = Long.parseLong(b.trim());
                    if (v >= Settings.MINIMUM_BUDGET) {
                        budget = v;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        this.settings = new Settings(initial, budget);
    }

    @Override
    public Settings load() {
        return this.settings;
    }

    @Override
    public void save(Settings settings) {
        this.settings = settings;
    }
}
