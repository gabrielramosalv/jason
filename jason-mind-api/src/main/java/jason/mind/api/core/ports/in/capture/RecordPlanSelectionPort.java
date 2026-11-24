package jason.mind.api.core.ports.in.capture;

/**
 * Port for recording the trace of a plan selection.
 *
 * @author Gabriel Lima
 */
public interface RecordPlanSelectionPort {

    /**
     * Records the trace of a plan selection.
     *
     * @param in event, search method and plan evaluations
     */
    void execute(RecordPlanSelectionIn in);
}
