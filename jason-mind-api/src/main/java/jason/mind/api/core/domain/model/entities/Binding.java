package jason.mind.api.core.domain.model.entities;

import jason.mind.api.core.domain.model.timeline.Moment;

import java.util.Objects;

/**
 * Variable binding produced during reasoning.
 * Associates a variable name with its value at a given moment and sequence.
 *
 * @param name     name of the variable
 * @param value    value bound to the variable
 * @param moment   moment in which the binding was made
 * @param sequence global sequence number of the binding
 * @author Gabriel Lima
 */
public record Binding(String name, String value, Moment moment, long sequence) {

    /**
     * Validates the binding.
     *
     * @throws NullPointerException if the name or the moment is null
     */
    public Binding {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(moment, "moment");
    }
}
