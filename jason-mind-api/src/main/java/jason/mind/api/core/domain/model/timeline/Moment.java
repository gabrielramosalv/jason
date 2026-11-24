package jason.mind.api.core.domain.model.timeline;

import java.util.Objects;

/**
 * Point in the reasoning of an agent.
 * Ordered by cycle, iteration, step and ordinal; moments of different agents are not comparable.
 *
 * @param agentCycle agent and cycle of the moment
 * @param iteration  iteration inside the cycle, starting at zero
 * @param step       reasoning step
 * @param ordinal    position inside the step, starting at one
 * @author Gabriel Lima
 */
public record Moment(AgentCycle agentCycle, int iteration, ReasoningStep step, int ordinal)
        implements Comparable<Moment> {

    /**
     * Validates the moment.
     *
     * @throws NullPointerException     if the agent cycle or the step is null
     * @throws IllegalArgumentException if the iteration is negative or the ordinal is below one
     */
    public Moment {
        Objects.requireNonNull(agentCycle, "agentCycle");
        Objects.requireNonNull(step, "step");
        if (iteration < 0) {
            throw new IllegalArgumentException("iteration < 0");
        }
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal < 1");
        }
    }

    /**
     * Creates a moment from its parts.
     *
     * @param agent     name of the agent
     * @param cycle     number of the cycle
     * @param iteration iteration inside the cycle
     * @param step      reasoning step
     * @param ordinal   position inside the step
     * @return moment built from the parts
     */
    public static Moment of(String agent, int cycle, int iteration, ReasoningStep step, int ordinal) {
        return new Moment(new AgentCycle(agent, cycle), iteration, step, ordinal);
    }

    /**
     * Returns the name of the agent.
     *
     * @return agent of the moment
     */
    public String agent() {
        return this.agentCycle.agent();
    }

    /**
     * Returns the cycle number.
     *
     * @return cycle of the moment
     */
    public int cycle() {
        return this.agentCycle.cycle();
    }

    /**
     * Tells whether this moment precedes another one of the same agent.
     *
     * @param o moment to compare with
     * @return true if this moment comes first
     * @throws IllegalArgumentException if the moments belong to different agents
     */
    public boolean isBefore(Moment o) {
        return this.compareTo(o) < 0;
    }

    /**
     * Returns the reasoning phase of the step.
     *
     * @return phase of the moment
     */
    public ReasoningPhase getPhase() {
        return this.step.getPhase();
    }

    /**
     * Orders moments by cycle, iteration, step and ordinal. This is a grouping order, not the order of the facts: an
     * {@link ReasoningStep#OUT_OF_CYCLE} effect that another thread queued in the middle of a cycle sorts after steps
     * it really came before. The total order inside an agent is the {@code sequence} of the occurrence.
     *
     * @param o moment to compare with
     * @return negative, zero or positive as this moment is grouped before, with or after the other
     */
    @Override
    public int compareTo(Moment o) {
        if (!this.agent().equals(o.agent())) {
            throw new IllegalArgumentException(
                    "Moments of different agents have no order: " + this.agent() + ", " + o.agent());
        }
        int c = Integer.compare(this.cycle(), o.cycle());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(this.iteration, o.iteration);
        if (c != 0) {
            return c;
        }
        c = this.step.compareTo(o.step);
        if (c != 0) {
            return c;
        }
        return Integer.compare(this.ordinal, o.ordinal);
    }
}
