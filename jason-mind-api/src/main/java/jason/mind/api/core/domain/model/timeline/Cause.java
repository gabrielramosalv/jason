package jason.mind.api.core.domain.model.timeline;

import jason.mind.api.core.domain.model.entities.IntendedMeans;
import jason.mind.api.core.domain.model.entities.Intention;

import java.util.Objects;

/**
 * Cause of an occurrence.
 * Points to the entity that caused it and, for the goal that executes a deed, to the code location being executed.
 * The goal (the intended means on the top of the stack) is the one that acts: it triggers events and creates, changes
 * and removes entities; the intention only holds the stack.
 *
 * @param by       entity that caused the occurrence
 * @param location code location of the cause, only allowed when the cause is an intended means (or an intention)
 * @author Gabriel Lima
 */
public record Cause(Entity by, CodeLocation location) {

    /**
     * Validates the cause.
     *
     * @throws NullPointerException     if the causing entity is null
     * @throws IllegalArgumentException if a location is given for a cause that is not an intended means or an intention
     */
    public Cause {
        Objects.requireNonNull(by, "by");
        if (location != null && !(by instanceof IntendedMeans) && !(by instanceof Intention)) {
            throw new IllegalArgumentException("location is only valid when by is an IntendedMeans or an Intention");
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
     * Creates a cause from a goal (the intended means on the top of the stack) executing a deed.
     *
     * @param goal     intended means that caused the occurrence
     * @param location code location being executed
     * @return cause pointing to the goal and location
     */
    public static Cause byGoal(IntendedMeans goal, CodeLocation location) {
        return new Cause(goal, location);
    }

    /**
     * Creates a cause from an intention executing at a code location.
     * Kept for an intention whose executing intended means was not tracked.
     *
     * @param intention intention that caused the occurrence
     * @param location  code location being executed
     * @return cause pointing to the intention and location
     */
    public static Cause byIntention(Intention intention, CodeLocation location) {
        return new Cause(intention, location);
    }
}
