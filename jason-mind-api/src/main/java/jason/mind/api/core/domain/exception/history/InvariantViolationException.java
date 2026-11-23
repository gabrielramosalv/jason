package jason.mind.api.core.domain.exception.history;

public class InvariantViolationException extends RuntimeException {

    private final String invariant;

    public InvariantViolationException(String invariant, String message) {
        super("[" + invariant + "] " + message);
        this.invariant = invariant;
    }

    public String getInvariant() {
        return invariant;
    }
}
