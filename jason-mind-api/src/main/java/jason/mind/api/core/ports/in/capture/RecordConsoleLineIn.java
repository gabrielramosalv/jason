package jason.mind.api.core.ports.in.capture;

/**
 * Input of a line printed to the console by an agent.
 *
 * @param agent name of the agent
 * @param cycle cycle in which the line was printed
 * @param text  printed text
 * @author Gabriel Lima
 */
public record RecordConsoleLineIn(String agent, int cycle, String text) {

}
