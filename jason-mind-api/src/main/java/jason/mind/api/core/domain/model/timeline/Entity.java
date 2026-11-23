package jason.mind.api.core.domain.model.timeline;

import java.util.Map;
import java.util.Objects;

public abstract class Entity<E extends Entity<E>> {

    private final long id;
    private final Occurrence start;
    private volatile Occurrence end;

    protected Entity(long id, Occurrence start) {
        this.id = id;
        this.start = start;
    }

    public long getId() {
        return id;
    }

    public Occurrence getStart() {
        return start;
    }

    public Occurrence getEnd() {
        return end;
    }

    public String getAgent() {
        return Objects.requireNonNull(start, "start").agent();
    }

    public void finish(Occurrence end) {
        Objects.requireNonNull(end, "end");
        if (this.end != null)
            throw new IllegalStateException(getClass().getSimpleName() + " " + id + " already ended");
        Occurrence s = lifeStart();

        if (s != null && s.agent().equals(end.agent()) && end.sequence() < s.sequence())
            throw new IllegalStateException("end before start [INV-029]");
        this.end = end;
    }

    protected Occurrence lifeStart() {
        return start;
    }

    public boolean isAlive() {
        return end == null;
    }

    public boolean existsAt(AgentCycle agentCycle) {
        if (start == null || !start.agent().equals(agentCycle.agent())) return false;
        if (start.cycle() > agentCycle.cycle()) return false;
        Occurrence e = end;
        return e == null || e.cycle() >= agentCycle.cycle();
    }

    public abstract Map<String, Timeline<?>> timelines();

    public Map<String, CollectionTimeline<?>> collections() {
        return Map.of();
    }

    public abstract String typeName();

    public abstract String label();

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public final int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return typeName() + "#" + id + "(" + label() + ")";
    }
}
