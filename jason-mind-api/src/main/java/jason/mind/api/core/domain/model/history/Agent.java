package jason.mind.api.core.domain.model.history;

import java.util.Objects;

import jason.mind.api.core.domain.model.timeline.Moment;
import jason.mind.api.core.domain.model.timeline.Occurrence;

public final class Agent {

    private final String name;
    private final Occurrence start;
    private final AgentConfig config;
    private volatile Occurrence end;
    private volatile Moment incompleteSince;

    public Agent(String name, Occurrence start, AgentConfig config) {
        this.name = Objects.requireNonNull(name, "name");
        this.start = start;
        this.config = Objects.requireNonNull(config, "config");
    }

    public String getName() { return name; }

    public Occurrence getStart() { return start; }

    public AgentConfig getConfig() { return config; }

    public Occurrence getEnd() { return end; }

    public Moment getIncompleteSince() { return incompleteSince; }

    public boolean hasEnded() { return end != null; }

    public void finish(Occurrence end) {
        Objects.requireNonNull(end, "end");
        if (!end.agent().equals(name)) throw new IllegalArgumentException("end outside the agent history");
        if (this.end != null) throw new IllegalStateException("agent " + name + " already ended");
        this.end = end;
    }

    public void markIncompleteSince(Moment moment) {
        if (incompleteSince == null) incompleteSince = moment;
    }
}
