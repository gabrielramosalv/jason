package jason.mind.api.core.domain.exception.history;

import lombok.Getter;

/**
 * Exception thrown when an occurrence violates an invariant of the recorded history.
 * Carries the identifier of the violated invariant and prefixes it to the message.
 *
 * @author Gabriel Lima
 */
@Getter
public class InvariantViolationException extends RuntimeException {

    /** Identifier of the violated invariant. */
    private final String invariant;

    /**
     * Creates the exception with the violated invariant and a description of the violation.
     *
     * @param invariant {@link #invariant}
     * @param message   description of the violation, prefixed with the invariant identifier
     */
    public InvariantViolationException(String invariant, String message) {
        super("[" + invariant + "] " + message);
        this.invariant = invariant;
    }
}
