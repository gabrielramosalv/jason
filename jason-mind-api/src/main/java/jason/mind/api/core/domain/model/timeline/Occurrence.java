package jason.mind.api.core.domain.model.timeline;

import java.util.Objects;

public record Occurrence(Moment moment, long sequence, Cause cause) {
    public Occurrence {
        Objects.requireNonNull(moment, "moment");
    }

    public String agent() {
        return moment.agent();
    }

    public int cycle() {
        return moment.cycle();
    }

    @FunctionalInterface
    public interface CauseSupplier {
        Cause get();
    }
}
