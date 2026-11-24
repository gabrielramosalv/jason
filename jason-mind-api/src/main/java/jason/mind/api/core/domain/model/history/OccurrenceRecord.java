package jason.mind.api.core.domain.model.history;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Occurrence;

import java.util.Objects;

/**
 * Record of something that happened to an entity.
 * Holds the entity, the kind of the occurrence, the changed timeline and value, and the occurrence itself.
 *
 * @param entity     entity the occurrence happened to
 * @param kind       kind of the occurrence
 * @param timeline   name of the changed timeline, or null
 * @param value      new value of the timeline, or null
 * @param occurrence occurrence that was recorded
 * @author Gabriel Lima
 */
public record OccurrenceRecord(Entity entity, Kind kind, String timeline, Object value, Occurrence occurrence) {

    /**
     * Validates the record.
     *
     * @throws NullPointerException if the entity, the kind or the occurrence is null
     */
    public OccurrenceRecord {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(occurrence, "occurrence");
    }

    /**
     * Returns the global sequence number of the occurrence.
     *
     * @return sequence of the recorded occurrence
     */
    public long sequence() {
        return this.occurrence.sequence();
    }

    /**
     * Kind of a recorded occurrence.
     */
    public enum Kind {
        /** The entity was started. */
        START,
        /** A timeline of the entity changed. */
        CHANGE,
        /** The entity arrived at its receiver. */
        ARRIVAL,
        /** The entity was ended. */
        END
    }
}
