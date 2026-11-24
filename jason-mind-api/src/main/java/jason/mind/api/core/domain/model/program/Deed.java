package jason.mind.api.core.domain.model.program;

import java.util.Objects;

/**
 * Deed of a plan body.
 * Holds its type, text, source line and the index of its parent deed.
 *
 * @param type    type of the deed
 * @param content text of the deed
 * @param line    line of the deed in the source code, or null
 * @param parent  index of the parent deed
 * @author Gabriel Lima
 */
public record Deed(Type type, String content, Integer line, int parent) {

    /**
     * Validates the deed.
     *
     * @throws NullPointerException if the type or the content is null
     */
    public Deed {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(content, "content");
    }

    /**
     * Type of a deed.
     */
    public enum Type {
        /** Environment action. */
        ACTION,
        /** Internal action. */
        INTERNAL_ACTION,
        /** Achievement goal. */
        ACHIEVE,
        /** Achievement goal with a new focus. */
        ACHIEVE_NEW_FOCUS,
        /** Test goal. */
        TEST,
        /** Belief addition. */
        ADD_BELIEF,
        /** Belief addition with a new focus. */
        ADD_BELIEF_NEW_FOCUS,
        /** Belief addition at the beginning of the base. */
        ADD_BELIEF_BEGIN,
        /** Belief addition at the end of the base. */
        ADD_BELIEF_END,
        /** Belief deletion. */
        DEL_BELIEF,
        /** Belief deletion with a new focus. */
        DEL_BELIEF_NEW_FOCUS,
        /** Belief deletion followed by addition. */
        DEL_ADD_BELIEF,
        /** Constraint expression. */
        CONSTRAINT
    }
}
