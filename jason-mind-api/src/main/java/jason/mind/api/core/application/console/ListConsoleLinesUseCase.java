package jason.mind.api.core.application.console;

import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.console.ConsoleLine;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.ports.in.console.ConsoleOut;
import jason.mind.api.core.ports.in.console.ListConsoleLinesIn;
import jason.mind.api.core.ports.in.console.ListConsoleLinesPort;
import jason.mind.api.core.ports.out.console.ConsoleRepositoryPort;
import jason.mind.api.core.ports.out.history.HistoryQueryRepositoryPort;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Use case that lists console lines page by page.
 * Lines are grouped by the closing sequence of their cycle, only lines of closed cycles are listed,
 * and the cursors returned are the closing sequences of the first and last cycles in the page.
 *
 * @author Gabriel Lima
 */
@RequiredArgsConstructor
public class ListConsoleLinesUseCase implements ListConsoleLinesPort {

    /** Repository that holds the console lines. */
    private final ConsoleRepositoryPort consoleRepository;

    /** Read-only access to the cycle records. */
    private final HistoryQueryRepositoryPort historyQuery;

    @Override
    public ConsoleOut execute(ListConsoleLinesIn in) {
        if (in.limit() == null || in.limit() < 1) {
            throw new InvalidQueryException("limit is required and must be positive");
        }
        if (in.after() != null && in.before() != null) {
            throw new InvalidQueryException("after and before are exclusive");
        }
        long until = in.until() == null ? Long.MAX_VALUE : in.until();
        Set<String> agents = in.agents() == null ? Set.of() : in.agents();

        TreeMap<Long, List<ConsoleLine>> byCycle = new TreeMap<>();
        for (ConsoleLine l : this.consoleRepository.linesOf(agents, Long.MIN_VALUE, until)) {
            CycleRecord c = this.historyQuery.cycle(l.agent(), l.cycle()).orElse(null);
            if (c == null || l.sequence() > c.sequenceEnd() || c.sequenceEnd() > until) {
                continue;
            }
            byCycle.computeIfAbsent(c.sequenceEnd(), k -> new ArrayList<>()).add(l);
        }
        Map<Long, List<ConsoleLine>> source;
        if (in.after() != null) {
            source = byCycle.tailMap(in.after(), false);
        } else if (in.before() != null) {
            source = byCycle.headMap(in.before(), false).descendingMap();
        } else {
            source = byCycle.descendingMap();
        }

        List<Long> taken = new ArrayList<>();
        List<ConsoleLine> lines = new ArrayList<>();
        for (Map.Entry<Long, List<ConsoleLine>> e : source.entrySet()) {
            if (lines.size() >= in.limit()) {
                break;
            }
            taken.add(e.getKey());
            lines.addAll(e.getValue());
        }
        lines.sort(Comparator.comparingLong(ConsoleLine::sequence));
        taken.sort(Long::compare);
        List<ConsoleOut.LineOut> out = new ArrayList<>();
        for (ConsoleLine l : lines) {
            out.add(new ConsoleOut.LineOut(l.agent(), l.cycle(), l.sequence(), l.timeMilliseconds(), l.text()));
        }
        Long previous = taken.isEmpty() ? null : taken.get(0);
        Long next = taken.isEmpty() ? in.after() : taken.get(taken.size() - 1);
        return new ConsoleOut(out, previous, next);
    }
}
