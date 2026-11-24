package jason.mind.api.core.application.cycle;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.ports.in.cycle.CyclesOut;
import jason.mind.api.core.ports.in.cycle.ListCyclesIn;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryMemory;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryQueryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryRepositoryAdapter;
import org.junit.jupiter.api.Test;

import jason.mind.api.core.application.history.InvalidQueryException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListCyclesCapTest {

    private static HistoryQueryRepositoryAdapter agentWithCycles(int last) {
        HistoryMemory memory = new HistoryMemory();
        HistoryRepositoryAdapter repo = new HistoryRepositoryAdapter(memory);
        repo.saveAgent(new Agent("bob", new Occurrence(Moment.of("bob", 1, 0, ReasoningStep.PERCEIVE, 1), 1, null),
                AgentConfig.defaults()));
        for (int c = 1; c <= last; c++) {
            repo.commitCycle("bob", new CycleRecord(c, 0, c * 10L, true, false, false));
        }
        return new HistoryQueryRepositoryAdapter(memory);
    }

    @Test
    void aWiderRangeKeepsTheNewestCyclesUpToTheCap() {
        CyclesOut out = new ListCyclesUseCase(agentWithCycles(12000)).execute(new ListCyclesIn("bob", null, null));
        assertEquals(ListCyclesUseCase.MAX_CYCLES, out.cycles().size());
        assertEquals(12000, out.to());
        assertEquals(12000 - ListCyclesUseCase.MAX_CYCLES + 1, out.from());
        assertEquals(1, out.oldestCycle());
    }

    @Test
    void theOlderPartIsReachedByLoweringTo() {
        CyclesOut out = new ListCyclesUseCase(agentWithCycles(12000)).execute(new ListCyclesIn("bob", null, 7000));
        assertEquals(2001, out.from());
        assertEquals(7000, out.to());
    }

    @Test
    void aNarrowRangeIsNotCut() {
        CyclesOut out = new ListCyclesUseCase(agentWithCycles(100)).execute(new ListCyclesIn("bob", null, null));
        assertEquals(100, out.cycles().size());
    }

    @Test
    void aRangeWithFromAfterToIsRejected() {
        assertThrows(InvalidQueryException.class,
                () -> new ListCyclesUseCase(agentWithCycles(100)).execute(new ListCyclesIn("bob", 10, 5)));
    }
}
