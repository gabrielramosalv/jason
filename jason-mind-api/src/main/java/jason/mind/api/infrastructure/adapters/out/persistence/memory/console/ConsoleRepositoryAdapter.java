package jason.mind.api.infrastructure.adapters.out.persistence.memory.console;

import jason.mind.api.core.domain.model.console.ConsoleLine;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Adapter that keeps the console lines of the agents in memory.
 * Keeps the lines of each agent apart, ordered by sequence, so that pruning an agent costs only the lines it removes
 * and never a scan of the lines of the other agents, and so they can be read by sequence range.
 *
 * @author Gabriel Lima
 */
public class ConsoleRepositoryAdapter implements ConsoleRepositoryPort {

    /** Console lines of each agent, by agent name and then by sequence. */
    private final Map<String, ConcurrentSkipListMap<Long, ConsoleLine>> lines = new ConcurrentHashMap<>();

    @Override
    public void append(ConsoleLine line) {
        this.lines.computeIfAbsent(line.agent(), a -> new ConcurrentSkipListMap<>()).put(line.sequence(), line);
    }

    @Override
    public void pruneBefore(String agent, int cycle) {
        ConcurrentSkipListMap<Long, ConsoleLine> own = this.lines.get(agent);
        if (own == null) {
            return;
        }
        // The lines of an agent are appended in order, so the cycle never decreases along the sequence: the lines to
        // prune are a prefix, and the loop stops at the first one that stays.
        for (Map.Entry<Long, ConsoleLine> e = own.firstEntry(); e != null && e.getValue().cycle() < cycle;
             e = own.firstEntry()) {
            own.remove(e.getKey(), e.getValue());
        }
    }

    @Override
    public List<ConsoleLine> linesOf(Set<String> agents, long afterSequence, long untilSequence) {
        long from = afterSequence == Long.MIN_VALUE ? Long.MIN_VALUE : afterSequence + 1;
        List<ConsoleLine> r = new ArrayList<>();
        for (Map.Entry<String, ConcurrentSkipListMap<Long, ConsoleLine>> own : this.lines.entrySet()) {
            if (agents.isEmpty() || agents.contains(own.getKey())) {
                r.addAll(own.getValue().subMap(from, true, untilSequence, true).values());
            }
        }
        r.sort(Comparator.comparingLong(ConsoleLine::sequence));
        return r;
    }
}
