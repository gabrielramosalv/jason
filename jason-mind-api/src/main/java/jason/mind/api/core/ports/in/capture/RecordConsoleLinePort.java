package jason.mind.api.core.ports.in.capture;

/**
 * Port for recording a console line printed by an agent.
 *
 * @author Gabriel Lima
 */
public interface RecordConsoleLinePort {

    /**
     * Records a console line.
     *
     * @param in agent, cycle and text of the line
     */
    void execute(RecordConsoleLineIn in);
}
