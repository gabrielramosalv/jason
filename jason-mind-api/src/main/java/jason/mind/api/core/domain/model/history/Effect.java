package jason.mind.api.core.domain.model.history;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;

/**
 * Effect that an entity caused on another entity.
 * Tells what happened to the affected entity, when, and which timeline and value changed.
 *
 * @param kind     kind of the effect
 * @param entity   affected entity
 * @param moment   moment of the effect
 * @param sequence global sequence number of the effect
 * @param timeline name of the changed timeline, or null
 * @param value    new value of the timeline, or null
 * @author Gabriel Lima
 */
public record Effect(Kind kind, Entity entity, Moment moment, long sequence, String timeline, Object value) {

    /**
     * Kind of an effect.
     */
    public enum Kind {
        /** The entity was started. */
        START,
        /** The entity was changed. */
        CHANGE,
        /** The entity was ended. */
        END
    }
}
