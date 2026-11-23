package jason.mind.api.core.ports.in.settings;

import java.util.List;

public record SettingsOut(int retentionCycles, String pruneAppliesAt, List<LiveBeyondHorizon> liveBeyondHorizon) {

    public record LiveBeyondHorizon(String agent, int count, Oldest oldest) {
    }

    public record Oldest(String type, String entityId, String label, int sinceCycle) {
    }
}
