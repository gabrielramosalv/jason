package jason.mind.api.infrastructure.adapters.out.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureTimeRepositoryAdapterTest {

    @Test
    void anAgentWithoutCyclesHasNoMean() {
        assertTrue(new CaptureTimeRepositoryAdapter().meanNanos("bob").isEmpty());
    }

    @Test
    void theMeanCoversOnlyTheLatestWindow() {
        CaptureTimeRepositoryAdapter repo = new CaptureTimeRepositoryAdapter();
        for (int i = 0; i < CaptureTimeRepositoryAdapter.WINDOW; i++) {
            repo.record("bob", 1_000);
        }
        assertEquals(1_000.0, repo.meanNanos("bob").getAsDouble());
        for (int i = 0; i < CaptureTimeRepositoryAdapter.WINDOW; i++) {
            repo.record("bob", 3_000);
        }
        assertEquals(3_000.0, repo.meanNanos("bob").getAsDouble());
    }

    @Test
    void agentsDoNotShareTheirTimes() {
        CaptureTimeRepositoryAdapter repo = new CaptureTimeRepositoryAdapter();
        repo.record("bob", 100);
        repo.record("alice", 300);
        assertEquals(100.0, repo.meanNanos("bob").getAsDouble());
        assertEquals(300.0, repo.meanNanos("alice").getAsDouble());
    }
}
