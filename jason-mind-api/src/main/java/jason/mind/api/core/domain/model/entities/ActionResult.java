package jason.mind.api.core.domain.model.entities;

import java.util.Objects;

/**
 * Result of an executed action.
 * Holds the outcome and, only for failures, the failure reason and message.
 *
 * @param result         outcome of the action
 * @param failureReason  reason of the failure, only allowed when the outcome is a failure
 * @param failureMessage message of the failure, only allowed when the outcome is a failure
 * @author Gabriel Lima
 */
public record ActionResult(Action.Result result, String failureReason, String failureMessage) {

    /**
     * Validates the result.
     *
     * @throws NullPointerException     if the outcome is null
     * @throws IllegalArgumentException if a successful result carries failure details
     */
    public ActionResult {
        Objects.requireNonNull(result, "result");
        if (result == Action.Result.SUCCESS && (failureReason != null || failureMessage != null)) {
            throw new IllegalArgumentException("failure reason only with FAILURE");
        }
    }

    @Override
    public String toString() {
        return this.result.name();
    }
}
