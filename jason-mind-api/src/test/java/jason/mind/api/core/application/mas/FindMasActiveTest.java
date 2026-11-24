package jason.mind.api.core.application.mas;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.ports.out.mas.MasRepositoryPort;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryMemory;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryQueryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryRepositoryAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindMasActiveTest {

    private static boolean activeWith(boolean[] busyByCycle) {
        HistoryMemory memory = new HistoryMemory();
        HistoryRepositoryAdapter repo = new HistoryRepositoryAdapter(memory);
        repo.saveAgent(new Agent("bob", new Occurrence(Moment.of("bob", 1, 0, ReasoningStep.PERCEIVE, 1), 1, null),
                AgentConfig.defaults()));
        for (int c = 1; c <= busyByCycle.length; c++) {
            repo.commitCycle("bob", new CycleRecord(c, 0, c * 10L, busyByCycle[c - 1], false, false));
        }
        MasRepositoryPort mas = new MasRepositoryPort() {
            @Override
            public void save(String name) {
            }

            @Override
            public String name() {
                return "mas";
            }
        };
        return new FindMasUseCase(new HistoryQueryRepositoryAdapter(memory), mas, "1", "1").execute().agents().get(0)
                .active();
    }

    @Test
    void anIdleLastCycleDoesNotMakeTheAgentInactiveWhenAnEarlierOneInTheWindowWorked() {
        boolean[] cycles = new boolean[30];
        cycles[24] = true;
        assertTrue(activeWith(cycles));
    }

    @Test
    void anAgentIdleForTheWholeWindowIsInactive() {
        boolean[] cycles = new boolean[30];
        cycles[5] = true;
        assertFalse(activeWith(cycles));
    }
}
