package jason.mind.api.core.domain.model.settings;

/**
 * Settings of the history recording.
 * Holds how many cycles are retained, within fixed bounds.
 *
 * @param retentionCycles number of cycles retained in the history
 * @author Gabriel Lima
 */
public record Settings(int retentionCycles) {

    /** Smallest allowed number of retained cycles. */
    public static final int MINIMUM_RETENTION = 1;

    /** Largest allowed number of retained cycles. */
    public static final int MAXIMUM_RETENTION = 10_000_000;

    /** Default number of retained cycles. */
    public static final int DEFAULT_RETENTION = 10_000;

    /**
     * Validates the settings.
     *
     * @throws IllegalArgumentException if the retention is outside the allowed bounds
     */
    public Settings {
        if (retentionCycles < MINIMUM_RETENTION || retentionCycles > MAXIMUM_RETENTION) {
            throw new IllegalArgumentException("retentionCycles outside 1..10000000: " + retentionCycles);
        }
    }
}
