package jason.mind.api.core.domain.model.history;

import java.util.Objects;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;

public record OccurrenceRecord(Entity<?> entity, Kind kind, String timeline, Object value, Occurrence occurrence) {

    public enum Kind { START, CHANGE, ARRIVAL, END }

    public OccurrenceRecord {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(occurrence, "occurrence");
    }

    public long sequence() {
        return occurrence.sequence();
    }
}
