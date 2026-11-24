package jason.mind.api.core.ports.in.capture;

/**
 * Port for recording an occurrence in the history of an agent.
 *
 * @author Gabriel Lima
 */
public interface RecordOccurrencePort {

    /**
     * Records an occurrence.
     *
     * @param in occurrence to record
     * @return affected entity and assigned sequence
     */
    RecordOccurrenceOut execute(RecordOccurrenceIn in);
}
