package jason.mind.api.core.ports.in.capture;

/**
 * Input describing the cause of an occurrence.
 *
 * @param byId      identifier of the causing entity
 * @param planId    identifier of the plan being executed, or null
 * @param deedIndex index of the deed being executed, or null
 * @author Gabriel Lima
 */
public record CauseIn(long byId, Long planId, Integer deedIndex) {

    /**
     * Creates a cause without code location.
     *
     * @param id identifier of the causing entity
     * @return cause pointing only to the entity
     */
    public static CauseIn by(long id) {
        return new CauseIn(id, null, null);
    }
}
