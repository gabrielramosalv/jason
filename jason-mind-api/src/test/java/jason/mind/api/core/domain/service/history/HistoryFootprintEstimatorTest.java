package jason.mind.api.core.domain.service.history;

import jason.mind.api.core.ports.out.history.Footprint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoryFootprintEstimatorTest {

    @Test
    void theBytesCountEntitiesAndEntriesAndIgnoreEffects() {
        assertEquals(10 * 650 + 100 * 100, HistoryFootprintEstimator.bytes(new Footprint(10, 100, 9_999)));
    }

    @Test
    void aHistoryThatFitsTheBudgetKeepsEverything() {
        assertEquals(0, HistoryFootprintEstimator.horizonFor(500, 100, 1, 100, 1_000));
        assertEquals(0, HistoryFootprintEstimator.horizonFor(5_000_000, 100, 1, 100, Long.MAX_VALUE));
    }

    @Test
    void anOverBudgetHistoryDropsTheOldestCyclesDownBelowTheBudget() {
        // 1000 bytes over 100 cycles, budget 500: drop enough cycles to reach 90% of the budget (450 bytes)
        int horizon = HistoryFootprintEstimator.horizonFor(1_000, 100, 1, 100, 500);
        int kept = 100 - (horizon - 1);
        assertTrue(kept * 10 <= 450, "kept " + kept + " cycles of 10 bytes");
        assertTrue(horizon > 1 && horizon <= 100);
    }

    @Test
    void theNewestCycleIsNeverDropped() {
        assertEquals(100, HistoryFootprintEstimator.horizonFor(1_000_000, 100, 1, 100, 1));
    }
}
