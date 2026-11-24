package jason.mind.api.core.domain.model.console;

/**
 * Line printed to the console by an agent.
 * Records the agent, the reasoning cycle, the global sequence and the time of the print.
 *
 * @param agent            name of the agent that printed the line
 * @param cycle            reasoning cycle in which the line was printed
 * @param sequence         global sequence number of the line
 * @param timeMilliseconds wall-clock time of the print, in milliseconds
 * @param text             printed text
 * @author Gabriel Lima
 */
public record ConsoleLine(String agent, int cycle, long sequence, long timeMilliseconds, String text) {

}
