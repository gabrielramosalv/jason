package jason.mind.api.core.domain.model.timeline;

import jason.mind.api.core.domain.model.entities.Intention;

import java.util.Objects;

/**
 * Cause of an occurrence.
 * Points to the entity that caused it and, for intentions, to the code location being executed.
 *
 * @param by       entity that caused the occurrence
 * @param location code location of the cause, only allowed when the cause is an intention
 * @author Gabriel Lima
 */
public record Cause(Entity by, CodeLocation location) {

    /**
     * Validates the cause.
     *
     * @throws NullPointerException     if the causing entity is null
     * @throws IllegalArgumentException if a location is given for a cause that is not an intention
     */
    public Cause {
        Objects.requireNonNull(by, "by");
        if (location != null && !(by instanceof Intention)) {
            throw new IllegalArgumentException("location is only valid when by is an Intention");
        }
    }

    /**
     * Creates a cause without code location.
     *
     * @param by entity that caused the occurrence
     * @return cause pointing to the entity
     */
    public static Cause by(Entity by) {
        return new Cause(by, null);
    }

    /**
     * Creates a cause from an intention executing at a code location.
     *
     * @param intention intention that caused the occurrence
     * @param location  code location being executed
     * @return cause pointing to the intention and location
     */
    public static Cause byIntention(Intention intention, CodeLocation location) {
        return new Cause(intention, location);
    }
}
