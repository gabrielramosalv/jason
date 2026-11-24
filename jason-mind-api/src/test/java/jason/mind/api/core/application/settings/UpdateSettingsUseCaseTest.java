package jason.mind.api.core.application.settings;

import jason.mind.api.core.domain.model.settings.Settings;
import jason.mind.api.core.ports.in.settings.SettingsOut;
import jason.mind.api.core.ports.in.settings.UpdateSettingsIn;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryMemory;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.history.HistoryQueryRepositoryAdapter;
import jason.mind.api.infrastructure.adapters.out.persistence.memory.settings.SettingsRepositoryAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateSettingsUseCaseTest {

    private final SettingsRepositoryAdapter settings = new SettingsRepositoryAdapter();
    private final UpdateSettingsUseCase update =
            new UpdateSettingsUseCase(this.settings, new HistoryQueryRepositoryAdapter(new HistoryMemory()));

    @Test
    void theDefaultBudgetIsATenthOfTheLargestHeap() {
        assertEquals(Settings.defaultBudget(Runtime.getRuntime().maxMemory()), this.settings.load().memoryBudgetBytes());
    }

    @Test
    void theBudgetCanBeChangedAloneAndTheRetentionIsKept() {
        int retention = this.settings.load().retentionCycles();
        SettingsOut out = this.update.execute(new UpdateSettingsIn(null, 50_000_000L));
        assertEquals(50_000_000L, out.memoryBudgetBytes());
        assertEquals(retention, out.retentionCycles());
    }

    @Test
    void theRetentionCanBeChangedAloneAndTheBudgetIsKept() {
        long budget = this.settings.load().memoryBudgetBytes();
        SettingsOut out = this.update.execute(new UpdateSettingsIn(500L, null));
        assertEquals(500, out.retentionCycles());
        assertEquals(budget, out.memoryBudgetBytes());
    }

    @Test
    void aRequestWithNeitherFieldIsRejected() {
        assertThrows(InvalidSettingsException.class, () -> this.update.execute(new UpdateSettingsIn(null, null)));
    }

    @Test
    void aBudgetBelowOneMebibyteIsRejected() {
        assertThrows(InvalidSettingsException.class, () -> this.update.execute(new UpdateSettingsIn(null, 1_000L)));
    }
}
