package jason.mind.api.infrastructure.adapters.out.persistence.memory.console;

import jason.mind.api.core.domain.model.console.ConsoleLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleRepositoryAdapterTest {

    private static ConsoleLine line(String agent, int cycle, long sequence) {
        return new ConsoleLine(agent, cycle, sequence, 0, agent + sequence);
    }

    private static List<Long> sequences(List<ConsoleLine> lines) {
        return lines.stream().map(ConsoleLine::sequence).toList();
    }

    @Test
    void linesOfAllAgentsComeInSequenceOrder() {
        ConsoleRepositoryAdapter r = new ConsoleRepositoryAdapter();
        r.append(line("a", 1, 1));
        r.append(line("b", 1, 2));
        r.append(line("a", 2, 3));
        assertEquals(List.of(1L, 2L, 3L), sequences(r.linesOf(Set.of(), Long.MIN_VALUE, Long.MAX_VALUE)));
        assertEquals(List.of(1L, 3L), sequences(r.linesOf(Set.of("a"), Long.MIN_VALUE, Long.MAX_VALUE)));
    }

    @Test
    void sequenceRangeExcludesTheLowerBoundAndIncludesTheUpperOne() {
        ConsoleRepositoryAdapter r = new ConsoleRepositoryAdapter();
        for (long s = 1; s <= 5; s++) {
            r.append(line(s % 2 == 0 ? "a" : "b", 1, s));
        }
        assertEquals(List.of(3L, 4L), sequences(r.linesOf(Set.of(), 2, 4)));
    }

    @Test
    void pruningAnAgentKeepsTheLinesOfTheCyclesFromTheHorizonAndTheOtherAgents() {
        ConsoleRepositoryAdapter r = new ConsoleRepositoryAdapter();
        r.append(line("a", 1, 1));
        r.append(line("b", 1, 2));
        r.append(line("a", 2, 3));
        r.append(line("a", 3, 4));
        r.pruneBefore("a", 3);
        assertEquals(List.of(2L, 4L), sequences(r.linesOf(Set.of(), Long.MIN_VALUE, Long.MAX_VALUE)));
    }

    @Test
    void pruningAnUnknownAgentDoesNothing() {
        ConsoleRepositoryAdapter r = new ConsoleRepositoryAdapter();
        r.append(line("a", 1, 1));
        r.pruneBefore("ghost", 10);
        assertEquals(List.of(1L), sequences(r.linesOf(Set.of(), Long.MIN_VALUE, Long.MAX_VALUE)));
    }
}
