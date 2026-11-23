package jason.mind.api.core.domain.model.program;

import java.util.Objects;

public record Deed(Type type, String content, Integer line, int parent) {

    public enum Type {
        ACTION, INTERNAL_ACTION, ACHIEVE, ACHIEVE_NEW_FOCUS, TEST, ADD_BELIEF, ADD_BELIEF_NEW_FOCUS,
        ADD_BELIEF_BEGIN, ADD_BELIEF_END, DEL_BELIEF, DEL_BELIEF_NEW_FOCUS, DEL_ADD_BELIEF, CONSTRAINT
    }

    public Deed {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(content, "content");
    }
}
