package jason.mind.api.core.ports.out.console;

import jason.mind.api.core.domain.model.console.ConsoleLine;

import java.util.List;
import java.util.Set;

/**
 * Port for storing and reading console lines.
 *
 * @author Gabriel Lima
 */
public interface ConsoleRepositoryPort {

    /**
     * Stores a console line.
     *
     * @param line line to store
     */
    void append(ConsoleLine line);

    /**
     * Removes the lines of an agent printed before a cycle.
     *
     * @param agent name of the agent
     * @param cycle first cycle whose lines are kept
     */
    void pruneBefore(String agent, int cycle);

    /**
     * Reads the lines of some agents in a sequence range.
     *
     * @param agents        agents whose lines are read
     * @param afterSequence lower bound of the sequence range
     * @param untilSequence upper bound of the sequence range
     * @return lines in the range
     */
    List<ConsoleLine> linesOf(Set<String> agents, long afterSequence, long untilSequence);
}
