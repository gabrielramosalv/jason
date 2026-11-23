package jason.mind.api.core.domain.model.settings;

public record Settings(int retentionCycles) {

    public static final int MINIMUM_RETENTION = 1;
    public static final int MAXIMUM_RETENTION = 10_000_000;
    public static final int DEFAULT_RETENTION = 10_000;

    public Settings {
        if (retentionCycles < MINIMUM_RETENTION || retentionCycles > MAXIMUM_RETENTION)
            throw new IllegalArgumentException("retentionCycles outside 1..10000000: " + retentionCycles);
    }
}
