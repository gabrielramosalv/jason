package jason.mind.api.core.domain.model.entities;

import java.util.Objects;

import jason.mind.api.core.domain.model.timeline.Moment;

public record Binding(String name, String value, Moment moment, long sequence) {

    public Binding {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(moment, "moment");
    }
}
