package jason.mind.api.core.domain.model.entities;

import java.util.Objects;

public record ActionResult(Action.Result result, String failureReason, String failureMessage) {

    public ActionResult {
        Objects.requireNonNull(result, "result");
        if (result == Action.Result.SUCCESS && (failureReason != null || failureMessage != null))
            throw new IllegalArgumentException("failure reason only with FAILURE [INV-024]");
    }

    @Override
    public String toString() {
        return result.name();
    }
}
