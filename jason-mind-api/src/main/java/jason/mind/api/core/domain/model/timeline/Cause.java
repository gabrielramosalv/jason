package jason.mind.api.core.domain.model.timeline;

import java.util.Objects;

import jason.mind.api.core.domain.model.entities.Intention;

public record Cause(Entity<?> by, CodeLocation location) {

    public Cause {
        Objects.requireNonNull(by, "by");
        if (location != null && !(by instanceof Intention))
            throw new IllegalArgumentException("location is only valid when by is an Intention [INV-014]");
    }

    public static Cause by(Entity<?> by) {
        return new Cause(by, null);
    }

    public static Cause byIntention(Intention intention, CodeLocation location) {
        return new Cause(intention, location);
    }
}
