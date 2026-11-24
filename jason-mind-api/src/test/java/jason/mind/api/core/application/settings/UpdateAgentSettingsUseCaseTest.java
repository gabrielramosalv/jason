package jason.mind.api.core.application.settings;

import jason.mind.api.core.application.capture.FinishCycleUseCase;
import jason.mind.api.core.application.history.AgentNotFoundException;
import jason.mind.api.core.domain.model.history.Agent;
import jason.mind.api.core.domain.model.history.AgentConfig;
import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;
import jason.mind.api.core.domain.model.timeline.ReasoningStep;
import jason.mind.api.core.ports.in.capture.FinishCycleIn;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.in.settings.UpdateAgentSettingsIn;
import jason.mind.api.core.ports.in.settings.UpdateSettingsIn;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.console.ConsoleRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryMemory;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryQueryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.settings.SettingsRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.provider.memory.sequence.SequenceProviderAdapter;
import jason.mind.api.infrastructure.adapters.out.provider.system.clock.ClockProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateAgentSettingsUseCaseTest {

    private HistoryRepositoryAdapter repo;
    private HistoryQueryRepositoryAdapter query;
    private SettingsRepositoryAdapter settings;
    private UpdateAgentSettingsUseCase update;
    private FinishCycleUseCase finish;

    private void agent(String name) {
        this.repo.saveAgent(new Agent(name, new Occurrence(Moment.of(name, 1, 0, ReasoningStep.PERCEIVE, 1), 1, null),
                AgentConfig.defaults()));
    }

    @BeforeEach
    void setUp() {
        HistoryMemory memory = new HistoryMemory();
        this.repo = new HistoryRepositoryAdapter(memory);
        this.query = new HistoryQueryRepositoryAdapter(memory);
        this.settings = new SettingsRepositoryAdapter();
        this.update = new UpdateAgentSettingsUseCase(this.settings, this.query);
        this.finish = new FinishCycleUseCase(this.repo, new ClockProviderAdapter(), new SequenceProviderAdapter(),
                new ConsoleRepositoryAdapter(), this.settings);
        agent("bob");
        agent("alice");
    }

    private SettingsOut agentView(SettingsOut out, String name) {
        assertTrue(out.agents().stream().anyMatch(a -> a.agent().equals(name)));
        return out;
    }

    @Test
    void anAgentWithoutOverrideShowsTheDefaultAndIsNotMarkedOverridden() {
        SettingsOut out = this.update.execute(new UpdateAgentSettingsIn("bob", 50L));
        SettingsOut.AgentRetention alice = agentView(out, "alice").agents().stream()
                .filter(a -> a.agent().equals("alice")).findFirst().orElseThrow();
        assertEquals(Settings.DEFAULT_RETENTION, alice.retentionCycles());
        assertFalse(alice.overridden());
    }

    @Test
    void theOverrideAppliesToThatAgentOnly() {
        this.update.execute(new UpdateAgentSettingsIn("bob", 50L));
        for (int c = 1; c <= 200; c++) {
            this.finish.execute(new FinishCycleIn("bob", c, true, false, false, 0));
            this.finish.execute(new FinishCycleIn("alice", c, true, false, false, 0));
        }
        assertEquals(151, this.query.oldestCycle("bob").orElseThrow().cycle());
        assertEquals(1, this.query.oldestCycle("alice").orElseThrow().cycle());
    }

    @Test
    void removingTheOverrideReturnsTheAgentToTheDefault() {
        this.update.execute(new UpdateAgentSettingsIn("bob", 50L));
        SettingsOut out = this.update.execute(new UpdateAgentSettingsIn("bob", null));
        SettingsOut.AgentRetention bob = out.agents().stream().filter(a -> a.agent().equals("bob")).findFirst().orElseThrow();
        assertFalse(bob.overridden());
        assertEquals(Settings.DEFAULT_RETENTION, bob.retentionCycles());
    }

    @Test
    void changingTheDefaultKeepsTheOverrides() {
        this.update.execute(new UpdateAgentSettingsIn("bob", 50L));
        new UpdateSettingsUseCase(this.settings, this.query).execute(new UpdateSettingsIn(777L, null));
        assertEquals(50, this.settings.load().retentionFor("bob"));
        assertEquals(777, this.settings.load().retentionFor("alice"));
    }

    @Test
    void anUnknownAgentIsNotFoundAndAnInvalidValueIsRejected() {
        assertThrows(AgentNotFoundException.class, () -> this.update.execute(new UpdateAgentSettingsIn("ghost", 5L)));
        assertThrows(InvalidSettingsException.class, () -> this.update.execute(new UpdateAgentSettingsIn("bob", 0L)));
        assertThrows(InvalidSettingsException.class,
                () -> this.update.execute(new UpdateAgentSettingsIn("bob", 10_000_001L)));
    }
}
