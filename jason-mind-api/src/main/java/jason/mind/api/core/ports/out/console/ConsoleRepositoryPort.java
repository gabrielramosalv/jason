package jason.mind.api.core.ports.out.console;

import java.util.List;
import java.util.Set;

import jason.mind.api.core.domain.model.console.ConsoleLine;

public interface ConsoleRepositoryPort {

    void append(ConsoleLine line);

    void pruneBefore(String agent, int cycle);

    List<ConsoleLine> linesOf(Set<String> agents, long afterSequence, long untilSequence);
}
