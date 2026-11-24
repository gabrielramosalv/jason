package jason.mind.api.core.domain.service.history;

import jason.mind.api.core.ports.out.history.Footprint;

/**
 * Domain service that estimates the memory a retained history uses from the count of what it holds, and the cycle
 * before which to prune so that it fits a budget.
 *
 * @author Gabriel Lima
 */
public final class HistoryFootprintEstimator {

    /**
     * Estimated bytes of one entity held. Fitted by least squares to the heap after GC at 1 000 to 30 000 retained
     * cycles of two examples (a small mind and a mind with 1 000 beliefs), keeping non-negative coefficients; the
     * fit leaves a mean error of 3.6% and a maximum of 14.7% on the history, and it is an estimate, not a measure.
     */
    public static final long BYTES_PER_ENTITY = 650;

    /** Estimated bytes of one timeline entry held, from the same fit as {@link #BYTES_PER_ENTITY}. */
    public static final long BYTES_PER_ENTRY = 100;

    /**
     * Estimated bytes of one caused occurrence held. Zero: the count grows in proportion to the entries, so the fit
     * cannot tell its cost apart and the entities and entries already carry it.
     */
    public static final long BYTES_PER_EFFECT = 0;

    /** Share of the budget a prune brings the history down to, so that it does not prune at every cycle. */
    public static final double PRUNE_TARGET = 0.9;

    private HistoryFootprintEstimator() {
    }

    /**
     * Estimates the bytes of a count.
     *
     * @param f count of what an agent holds
     * @return estimated bytes
     */
    public static long bytes(Footprint f) {
        return f.entities() * BYTES_PER_ENTITY + f.timelineEntries() * BYTES_PER_ENTRY
                + f.effects() * BYTES_PER_EFFECT;
    }

    /**
     * Finds the oldest cycle to keep so that the history fits a budget.
     *
     * @param bytes          estimated bytes of the history now
     * @param retainedCycles number of cycles that estimate covers
     * @param oldestCycle    oldest cycle held now
     * @param newestCycle    newest closed cycle
     * @param budgetBytes    bytes the history may use
     * @return the oldest cycle to keep, or {@code 0} when the history already fits; never after the newest cycle
     */
    public static int horizonFor(long bytes, int retainedCycles, int oldestCycle, int newestCycle, long budgetBytes) {
        if (bytes <= budgetBytes || retainedCycles <= 0 || budgetBytes == Long.MAX_VALUE) {
            return 0;
        }
        double perCycle = (double) bytes / retainedCycles;
        double excess = bytes - budgetBytes * PRUNE_TARGET;
        long drop = (long) Math.ceil(excess / perCycle);
        long horizon = Math.min((long) oldestCycle + drop, newestCycle);
        return (int) Math.max(horizon, 1);
    }
}
