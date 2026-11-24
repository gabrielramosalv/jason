package jason.mind.api.core.domain.model.settings;

import java.util.Map;

/**
 * Settings of the history recording.
 * Holds how many cycles are retained and how many bytes the history of all agents may use, each within fixed
 * bounds. The history is pruned by whichever limit is reached first.
 *
 * @param retentionCycles   number of cycles retained in the history by default
 * @param memoryBudgetBytes bytes the history of all agents together may use, or {@link #UNLIMITED}
 * @param agentRetention    cycles retained by the agents that override the default, by agent name
 * @author Gabriel Lima
 */
public record Settings(int retentionCycles, long memoryBudgetBytes, Map<String, Integer> agentRetention) {

    /** Smallest allowed number of retained cycles. */
    public static final int MINIMUM_RETENTION = 1;

    /** Largest allowed number of retained cycles. */
    public static final int MAXIMUM_RETENTION = 10_000_000;

    /** Default number of retained cycles. */
    public static final int DEFAULT_RETENTION = 10_000;

    /** Smallest allowed memory budget: one mebibyte. */
    public static final long MINIMUM_BUDGET = 1_048_576L;

    /** Value of the budget that turns the limit by bytes off. */
    public static final long UNLIMITED = Long.MAX_VALUE;

    /** Fraction of the largest heap of the JVM that the history may use by default. */
    public static final double DEFAULT_BUDGET_FRACTION = 0.10;

    /**
     * Validates the settings.
     *
     * @throws IllegalArgumentException if the retention or the budget is outside the allowed bounds
     */
    public Settings {
        if (retentionCycles < MINIMUM_RETENTION || retentionCycles > MAXIMUM_RETENTION) {
            throw new IllegalArgumentException("retentionCycles outside 1..10000000: " + retentionCycles);
        }
        if (memoryBudgetBytes < MINIMUM_BUDGET) {
            throw new IllegalArgumentException("memoryBudgetBytes below 1048576: " + memoryBudgetBytes);
        }
        agentRetention = Map.copyOf(agentRetention);
        for (Map.Entry<String, Integer> e : agentRetention.entrySet()) {
            if (e.getValue() < MINIMUM_RETENTION || e.getValue() > MAXIMUM_RETENTION) {
                throw new IllegalArgumentException("retentionCycles of " + e.getKey() + " outside 1..10000000: " + e.getValue());
            }
        }
    }

    /**
     * Creates settings without per-agent retention.
     *
     * @param retentionCycles   number of cycles retained in the history by default
     * @param memoryBudgetBytes bytes the history of all agents together may use, or {@link #UNLIMITED}
     */
    public Settings(int retentionCycles, long memoryBudgetBytes) {
        this(retentionCycles, memoryBudgetBytes, Map.of());
    }

    /**
     * Returns the number of cycles an agent retains.
     *
     * @param agent name of the agent
     * @return the retention the agent overrides, or the default of the system
     */
    public int retentionFor(String agent) {
        return this.agentRetention.getOrDefault(agent, this.retentionCycles);
    }

    /**
     * Returns these settings with the retention of one agent set or removed.
     *
     * @param agent           name of the agent
     * @param retentionCycles new retention of the agent, or null to go back to the default
     * @return the new settings
     */
    public Settings withAgentRetention(String agent, Integer retentionCycles) {
        java.util.HashMap<String, Integer> m = new java.util.HashMap<>(this.agentRetention);
        if (retentionCycles == null) {
            m.remove(agent);
        } else {
            m.put(agent, retentionCycles);
        }
        return new Settings(this.retentionCycles, this.memoryBudgetBytes, m);
    }

    /**
     * Creates settings without a limit by bytes.
     *
     * @param retentionCycles number of cycles retained in the history
     */
    public Settings(int retentionCycles) {
        this(retentionCycles, UNLIMITED, Map.of());
    }

    /**
     * Returns the default budget for a JVM.
     *
     * @param maxHeapBytes largest heap the JVM may use
     * @return {@link #DEFAULT_BUDGET_FRACTION} of the heap, never below {@link #MINIMUM_BUDGET}
     */
    public static long defaultBudget(long maxHeapBytes) {
        return Math.max(MINIMUM_BUDGET, (long) (maxHeapBytes * DEFAULT_BUDGET_FRACTION));
    }
}
