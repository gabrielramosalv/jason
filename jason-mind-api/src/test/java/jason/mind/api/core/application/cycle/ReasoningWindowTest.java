package jason.mind.api.core.application.cycle;

import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.domain.service.reasoning.CandidatesDomainService;
import jason.mind.api.core.domain.service.reasoning.QueueDomainService;
import jason.mind.api.core.ports.in.cycle.CyclesOut;
import jason.mind.api.core.ports.in.cycle.FindCycleReasoningIn;
import jason.mind.api.core.ports.in.cycle.ListCyclesIn;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryMemory;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryQueryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryRepositoryAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReasoningWindowTest {

    @Test
    void everyListedCycleHasItsReasoning() {
        HistoryMemory memory = new HistoryMemory();
        HistoryRepositoryAdapter repo = new HistoryRepositoryAdapter(memory);
        HistoryQueryRepositoryAdapter query = new HistoryQueryRepositoryAdapter(memory);
        Agent a = new Agent("bob", new Occurrence(Moment.of("bob", 1, 0, ReasoningStep.PERCEIVE, 1), 1, null),
                AgentConfig.defaults());
        repo.saveAgent(a);
        for (int c = 1; c <= 30; c++) {
            repo.commitCycle("bob", new CycleRecord(c, 0, c * 10L, true, true, true));
        }
        repo.prune("bob", 11);
        CyclesOut out = new ListCyclesUseCase(query).execute(new ListCyclesIn("bob", null, null));
        FindCycleReasoningUseCase uc = new FindCycleReasoningUseCase(query, new CandidatesDomainService(), new QueueDomainService());
        for (CyclesOut.CycleItem i : out.cycles()) {
            assertNotNull(uc.execute(new FindCycleReasoningIn("bob", i.cycle())));
        }
    }
}
