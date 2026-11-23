package jason.mind.api.core.domain.model.history;

import jason.mind.api.core.domain.model.timeline.Entity;
import jason.mind.api.core.domain.model.timeline.Moment;

public record Effect(Kind kind, Entity<?> entity, Moment moment, long sequence, String timeline, Object value) {

    public enum Kind { START, CHANGE, END }
}
