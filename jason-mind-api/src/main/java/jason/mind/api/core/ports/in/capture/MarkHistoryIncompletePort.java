package jason.mind.api.core.ports.in.capture;

/**
 * Port for marking the history of an agent as incomplete.
 *
 * @author Gabriel Lima
 */
public interface MarkHistoryIncompletePort {

    /**
     * Marks the history of an agent as incomplete.
     *
     * @param in agent and moment from which the history is incomplete
     */
    void execute(MarkHistoryIncompleteIn in);
}
