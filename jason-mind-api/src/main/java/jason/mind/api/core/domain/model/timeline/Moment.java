package jason.mind.api.core.domain.model.timeline;

import java.util.Objects;

public record Moment(AgentCycle agentCycle, int iteration, ReasoningStep step, int ordinal) implements Comparable<Moment> {

    public Moment {
        Objects.requireNonNull(agentCycle, "agentCycle");
        Objects.requireNonNull(step, "step");
        if (iteration < 0) throw new IllegalArgumentException("iteration < 0");
        if (ordinal < 1) throw new IllegalArgumentException("ordinal < 1");
    }

    public static Moment of(String agent, int cycle, int iteration, ReasoningStep step, int ordinal) {
        return new Moment(new AgentCycle(agent, cycle), iteration, step, ordinal);
    }

    public String agent() {
        return agentCycle.agent();
    }

    public int cycle() {
        return agentCycle.cycle();
    }

    public ReasoningPhase getPhase() {
        return step.getPhase();
    }

    @Override
    public int compareTo(Moment o) {
        if (!agent().equals(o.agent()))
            throw new IllegalArgumentException("Moments of different agents have no order: " + agent() + ", " + o.agent());
        int c = Integer.compare(cycle(), o.cycle());
        if (c != 0) return c;
        c = Integer.compare(iteration, o.iteration);
        if (c != 0) return c;
        c = step.compareTo(o.step);
        if (c != 0) return c;
        return Integer.compare(ordinal, o.ordinal);
    }

    public boolean isBefore(Moment o) {
        return compareTo(o) < 0;
    }
}
