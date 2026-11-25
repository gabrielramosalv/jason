package jason.mind.api.core.ports.in.lifecycle;

import java.util.List;

/**
 * Output with a page of the lifecycle of an entity.
 *
 * @param id         identifier of the entity
 * @param type       type of the entity
 * @param label      label of the entity
 * @param entries    lifecycle entries of the page
 * @param nextCursor cursor of the next page, or null
 * @author Gabriel Lima
 */
public record LifecycleOut(String id, String type, String label, List<EntryOut> entries, Long nextCursor) {

    /**
     * Lifecycle entry.
     *
     * @param sequence global sequence number of the entry
     * @param kind     kind of the entry
     * @param code     code of the entry
     * @param timeline name of the changed timeline, or null
     * @param value    new value, or null
     * @param moment   moment of the entry
     * @param cause    cause of the entry, or null
     * @param entity   entity related to the entry, or null
     * @param into     entity the entry leads into, or null
     * @param valueEntity entity the new value points to (the event an intention waits for, the goal pushed on the stack), or null
     */
    public record EntryOut(long sequence, String kind, String code, String timeline, String value, MomentOut moment,
                           CauseOut cause, EntityOut entity, EntityOut into, EntityOut valueEntity) {

    }

    /**
     * Moment of an entry.
     *
     * @param cycle     number of the cycle
     * @param iteration iteration inside the cycle
     * @param phase     name of the phase
     * @param step      name of the step
     * @param ordinal   position inside the step
     */
    public record MomentOut(int cycle, int iteration, String phase, String step, int ordinal) {

    }

    /**
     * Cause of an entry.
     *
     * @param by       causing entity
     * @param location code location of the cause, or null
     */
    public record CauseOut(EntityOut by, LocationOut location) {

    }

    /**
     * Entity referenced by an entry.
     *
     * @param type   type of the entity
     * @param id     identifier of the entity
     * @param label  label of the entity
     * @param sender sender of a message, or null
     */
    public record EntityOut(String type, String id, String label, String sender) {

    }

    /**
     * Code location of a cause.
     *
     * @param planId    identifier of the plan
     * @param deedIndex index of the deed, or null
     * @param line      source line, or null
     */
    public record LocationOut(String planId, Integer deedIndex, Integer line) {

    }
}
