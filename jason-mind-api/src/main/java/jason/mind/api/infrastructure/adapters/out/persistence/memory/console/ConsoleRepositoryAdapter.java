package jason.mind.api.infrastructure.adapters.out.persistence.memory.console;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import jason.mind.api.core.domain.model.console.ConsoleLine;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;

public class ConsoleRepositoryAdapter implements ConsoleRepositoryPort {

    private final ConcurrentSkipListMap<Long, ConsoleLine> lines = new ConcurrentSkipListMap<>();

    @Override
    public void append(ConsoleLine line) {
        lines.put(line.sequence(), line);
    }

    @Override
    public void pruneBefore(String agent, int cycle) {
        Iterator<ConsoleLine> it = lines.values().iterator();
        while (it.hasNext()) {
            ConsoleLine l = it.next();
            if (l.agent().equals(agent) && l.cycle() < cycle) it.remove();
        }
    }

    @Override
    public List<ConsoleLine> linesOf(Set<String> agents, long afterSequence, long untilSequence) {
        List<ConsoleLine> r = new ArrayList<>();
        long from = afterSequence == Long.MIN_VALUE ? Long.MIN_VALUE : afterSequence + 1;
        for (ConsoleLine l : lines.subMap(from, true, untilSequence, true).values())
            if (agents.isEmpty() || agents.contains(l.agent())) r.add(l);
        return r;
    }
}
