package jason.mind.api.core.application.prune;

import jason.mind.api.core.application.capture.FinishCycleUseCase;
import jason.mind.api.core.application.history.AgentNotFoundException;
import jason.mind.api.core.application.history.InvalidQueryException;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.entities.Message;
import jason.mind.api.core.domain.model.entities.Performative;
import jason.mind.api.core.domain.model.entities.SendMode;
import jason.mind.api.core.domain.model.history.CycleRecord;
import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.ports.in.capture.FinishCycleIn;
import jason.mind.api.core.ports.in.prune.PruneOut;
import jason.mind.api.core.ports.in.prune.RequestPruneIn;
import jason.mind.api.infrastructure.adapters.out.provider.system.clock.ClockProviderAdapter;
import jason.mind.api.infrastructure.adapters.out.metrics.CaptureTimeRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.console.ConsoleRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryMemory;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryQueryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.settings.SettingsRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.provider.memory.sequence.SequenceProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestPruneUseCaseTest {

    private HistoryRepositoryAdapter repo;
    private HistoryQueryRepositoryAdapter query;
    private RequestPruneUseCase prune;
    private FinishCycleUseCase finish;
    private CaptureTimeRepositoryAdapter captureTime;

    @BeforeEach
    void setUp() {
        HistoryMemory memory = new HistoryMemory();
        this.repo = new HistoryRepositoryAdapter(memory);
        this.query = new HistoryQueryRepositoryAdapter(memory);
        this.captureTime = new CaptureTimeRepositoryAdapter();
        this.repo.saveAgent(new Agent("bob", new Occurrence(Moment.of("bob", 1, 0, ReasoningStep.PERCEIVE, 1), 1, null),
                AgentConfig.defaults()));
        this.finish = new FinishCycleUseCase(this.repo, new ClockProviderAdapter(), new SequenceProviderAdapter(),
                new ConsoleRepositoryAdapter(), new SettingsRepositoryAdapter(), this.captureTime);
        this.prune = new RequestPruneUseCase(this.repo, this.query);
        for (int c = 1; c <= 30; c++) {
            this.finish.execute(new FinishCycleIn("bob", c, true, false, false, 0));
        }
    }

    @Test
    void theRequestIsOnlyAppliedAtTheEndOfTheNextCycle() {
        PruneOut out = this.prune.execute(new RequestPruneIn("bob", 20));
        assertEquals(20, out.requests().get(0).until());
        assertEquals(1, this.query.oldestCycle("bob").orElseThrow().cycle());
        this.finish.execute(new FinishCycleIn("bob", 31, true, false, false, 0));
        assertEquals(20, this.query.oldestCycle("bob").orElseThrow().cycle());
    }

    @Test
    void aRequestBeyondTheNewestCycleKeepsTheNewestCycle() {
        PruneOut out = this.prune.execute(new RequestPruneIn("bob", 500));
        assertEquals(30, out.requests().get(0).until());
        this.finish.execute(new FinishCycleIn("bob", 31, true, false, false, 0));
        assertEquals(30, this.query.oldestCycle("bob").orElseThrow().cycle());
    }

    @Test
    void theHorizonOnlyMovesForward() {
        this.prune.execute(new RequestPruneIn("bob", 20));
        this.finish.execute(new FinishCycleIn("bob", 31, true, false, false, 0));
        this.prune.execute(new RequestPruneIn("bob", 5));
        this.finish.execute(new FinishCycleIn("bob", 32, true, false, false, 0));
        assertEquals(20, this.query.oldestCycle("bob").orElseThrow().cycle());
    }

    @Test
    void withoutAnAgentTheRequestReachesEveryAgent() {
        PruneOut out = this.prune.execute(new RequestPruneIn(null, 10));
        assertEquals(1, out.requests().size());
        assertEquals("bob", out.requests().get(0).agent());
    }

    @Test
    void aMissingOrNegativeUntilIsRejected() {
        assertThrows(InvalidQueryException.class, () -> this.prune.execute(new RequestPruneIn("bob", null)));
        assertThrows(InvalidQueryException.class, () -> this.prune.execute(new RequestPruneIn("bob", -1)));
    }

    @Test
    void anUnknownAgentIsNotFound() {
        assertThrows(AgentNotFoundException.class, () -> this.prune.execute(new RequestPruneIn("ghost", 5)));
    }

    @Test
    void theCaptureTimeOfTheCycleIsRecorded() {
        this.finish.execute(new FinishCycleIn("bob", 31, true, false, false, 2_000));
        assertEquals(2_000.0, this.captureTime.meanNanos("bob").getAsDouble());
    }

    @Test
    void anOverBudgetHistoryIsPrunedAtTheEndOfTheCycleWithoutAnyRequest() {
        for (long id = 1; id <= 3_000; id++) {
            this.repo.appendEntity("bob", new Message(id, new Occurrence(Moment.of("bob", 1, 0, ReasoningStep.PERCEIVE, 1), id + 1, null),
                    1, SendMode.ASYNCHRONOUS, null, null, "alice", Performative.TELL, "hi"));
        }
        SettingsRepositoryAdapter small = new SettingsRepositoryAdapter();
        small.save(new Settings(10_000, Settings.MINIMUM_BUDGET));
        FinishCycleUseCase tight = new FinishCycleUseCase(this.repo, new ClockProviderAdapter(), new SequenceProviderAdapter(),
                new ConsoleRepositoryAdapter(), small, this.captureTime);
        assertEquals(1, this.query.oldestCycle("bob").orElseThrow().cycle());
        for (int c = 31; c <= 100; c++) {
            tight.execute(new FinishCycleIn("bob", c, true, false, false, 0));
        }
        assertTrue(this.query.oldestCycle("bob").orElseThrow().cycle() > 1);
    }

    @Test
    void aHistoryThatFitsTheBudgetIsNotPruned() {
        FinishCycleUseCase roomy = new FinishCycleUseCase(this.repo, new ClockProviderAdapter(), new SequenceProviderAdapter(),
                new ConsoleRepositoryAdapter(), new SettingsRepositoryAdapter(), this.captureTime);
        roomy.execute(new FinishCycleIn("bob", 31, true, false, false, 0));
        assertEquals(1, this.query.oldestCycle("bob").orElseThrow().cycle());
    }
}
