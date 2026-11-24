package jason.mind.api.core.ports.in.capture;

/**
 * Output of a recorded occurrence.
 *
 * @param entityId identifier of the affected entity
 * @param sequence global sequence number assigned to the occurrence
 * @author Gabriel Lima
 */
public record RecordOccurrenceOut(long entityId, long sequence) {

}
