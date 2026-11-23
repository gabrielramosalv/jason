package jason.mind.api.core.ports.in.lifecycle;

import java.util.List;

public record LifecycleOut(String id, String type, String label, List<EntryOut> entries, Long nextCursor) {

    public record EntryOut(long sequence, String kind, String code, String timeline, String value, MomentOut moment,
                           CauseOut cause, EntityOut entity, EntityOut into) {
    }

    public record MomentOut(int cycle, int iteration, String phase, String step, int ordinal) {
    }

    public record CauseOut(EntityOut by, LocationOut location) {
    }

    public record EntityOut(String type, String id, String label, String sender) {
    }

    public record LocationOut(String planId, Integer deedIndex, Integer line) {
    }
}
